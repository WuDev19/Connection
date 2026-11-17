package com.example.connection.ui.camera.effects;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraEffect;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceProcessor;
import androidx.camera.core.SurfaceRequest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** One GPU pipeline for both camera preview and encoder. All EGL objects live on one thread. */
public final class CameraGpuEffect implements SurfaceProcessor, AutoCloseable {
    private static final String VERTEX="attribute vec2 aPosition; attribute vec2 aTex; uniform mat4 uTransform; varying vec2 vTex; void main(){ gl_Position=vec4(aPosition,0.,1.); vTex=(uTransform*vec4(aTex,0.,1.)).xy; }";
    private static final String EXTERNAL="#extension GL_OES_EGL_image_external : require\nprecision mediump float; varying vec2 vTex; uniform samplerExternalOES uTexture; void main(){ gl_FragColor=texture2D(uTexture,vTex); }";
    private static final String COMPOSITE="precision mediump float; varying vec2 vTex; uniform sampler2D uTexture; uniform sampler2D uOverlay; uniform mat4 uColor; uniform vec4 uOffset; void main(){ vec4 c=clamp(uColor*texture2D(uTexture,vTex)+uOffset,0.,1.); vec4 a=texture2D(uOverlay,vec2(vTex.x,1.-vTex.y)); gl_FragColor=vec4(c.rgb*(1.-a.a)+a.rgb,1.); }";
    private static final String COPY="precision mediump float; varying vec2 vTex; uniform sampler2D uTexture; void main(){ gl_FragColor=texture2D(uTexture,vTex); }";
    private final HandlerThread thread=new HandlerThread("Camera-effects-GL");
    private final Handler handler;
    private final Executor executor;
    private final CameraEffectsProcessor effects;
    private final Consumer<Throwable> onError;
    private final List<Output> outputs=new ArrayList<>();
    private final float[] identity=new float[16];
    private final float[] analysisTransform=new float[16];
    private final float[] transform=new float[16];
    private final float[] colorUniform=new float[16];
    private CameraEffectState colorState;
    private EGLDisplay display;
    private EGLContext context;
    private EGLConfig config;
    private EGLSurface pbuffer;
    private int externalProgram, copyProgram, compositeProgram, overlayTexture, vertexBuffer;
    private final java.util.Map<Integer, Locations> locations=new java.util.HashMap<>();
    private boolean asyncReadback;
    private int pendingInputs;
    private volatile boolean closing;
    private boolean failed, disposed;
    private Bitmap overlay;
    private CameraEffectState.Face drawnFace;
    private int drawnGeneration=-1;
    private final FacePoseSmoother smoother=new FacePoseSmoother();
    private final FaceEffectRenderer renderer=new FaceEffectRenderer();
    private long lastAnalysis;

    public CameraGpuEffect(CameraEffectsProcessor effects, Consumer<Throwable> onError) {
        this.effects=effects; this.onError=onError;
        Matrix.setIdentityM(identity,0);
        // Readback rows can be copied directly into an Android bitmap; flip on the GPU.
        Matrix.setIdentityM(analysisTransform,0);
        analysisTransform[5]=-1; analysisTransform[13]=1;
        thread.start(); handler=new Handler(thread.getLooper()); executor=command -> handler.post(command);
    }

    public CameraEffect asCameraEffect() {
        return new CameraEffect(CameraEffect.PREVIEW|CameraEffect.VIDEO_CAPTURE, executor,this,
                error -> report(error)) {};
    }

    @Override public void onInputSurface(@NonNull SurfaceRequest request) {
        if(closing || failed) { request.willNotProvideSurface(); return; }
        Input input=null;
        try {
            init(); current(pbuffer);
            input=new Input(request.getResolution().getWidth(),request.getResolution().getHeight());
            final Input owned=input;
            request.setTransformationInfoListener(executor,info -> {
                if(owned.rotation!=info.getRotationDegrees()) effects.resetTracking();
                owned.rotation=info.getRotationDegrees();
            });
            owned.stream.setOnFrameAvailableListener(st -> {
                if(!owned.renderScheduled && !owned.released) {
                    owned.renderScheduled=true;
                    handler.post(owned.renderTask);
                }
            },handler);
            pendingInputs++;
            request.provideSurface(owned.surface,executor,result -> {
                request.clearTransformationInfoListener();
                current(pbuffer); owned.release(); pendingInputs--;
                if(closing && pendingInputs==0) dispose();
            });
        } catch(Exception e) {
            if(input!=null) input.release();
            request.willNotProvideSurface(); report(e);
        }
    }

    @Override public void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) {
        if(closing || failed) { surfaceOutput.close(); return; }
        try {
            init();
            Surface surface=surfaceOutput.getSurface(executor,event -> removeOutput(surfaceOutput));
            EGLSurface window=EGL14.eglCreateWindowSurface(display,config,surface,new int[]{EGL14.EGL_NONE},0);
            if(window==EGL14.EGL_NO_SURFACE) throw new IllegalStateException("Cannot create effect output");
            outputs.add(new Output(surfaceOutput,window));
        } catch(Exception e) { surfaceOutput.close(); report(e); }
    }

    private void render(Input input) {
        if(closing || failed || input.released) return;
        try {
            current(pbuffer);
            input.stream.updateTexImage();
            input.stream.getTransformMatrix(transform);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,input.framebuffer);
            GLES20.glViewport(0,0,input.width,input.height);
            draw(externalProgram,input.external,GLES11Ext.GL_TEXTURE_EXTERNAL_OES,transform);

            CameraEffectState state=effects.getState();
            updateOverlay(input,state);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
            GLES20.glUseProgram(compositeProgram);
            Locations composite=locations.get(compositeProgram);
            if(colorState!=state) {
                float[] m=state.colorMatrix();
                for(int row=0;row<4;row++) for(int col=0;col<4;col++) colorUniform[col*4+row]=m[row*5+col];
                GLES20.glUniformMatrix4fv(composite.color,1,false,colorUniform,0);
                GLES20.glUniform4f(composite.offset,m[4],m[9],m[14],m[19]);
                colorState=state;
            }
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,overlayTexture);
            GLES20.glUniform1i(composite.overlay,1);
            for(int i=0;i<outputs.size();i++) {
                Output output=outputs.get(i);
                current(output.window);
                GLES20.glViewport(0,0,output.source.getSize().getWidth(),output.source.getSize().getHeight());
                output.source.updateTransformMatrix(transform,identity);
                draw(compositeProgram,input.texture,GLES20.GL_TEXTURE_2D,transform);
                EGLExt.eglPresentationTimeANDROID(display,output.window,input.stream.getTimestamp());
                if(!EGL14.eglSwapBuffers(display,output.window)) { removeOutput(output.source); i--; }
            }
            // Present the camera frame before doing optional analysis work.
            current(pbuffer);
            analyze(input,state);
        } catch(Exception e) { report(e); }
    }

    private void updateOverlay(Input input,CameraEffectState state) {
        float[] points=state.face==CameraEffectState.Face.NONE?null:effects.tracker.current();
        int generation=effects.tracker.generation();
        if(generation!=drawnGeneration || state.face!=drawnFace) smoother.update(null,0);
        boolean changed=smoother.update(points,SystemClock.elapsedRealtime());
        int w=state.face==CameraEffectState.Face.NONE?1:512*input.width/Math.max(input.width,input.height);
        int h=state.face==CameraEffectState.Face.NONE?1:512*input.height/Math.max(input.width,input.height);
        if(overlay!=null && overlay.getWidth()==w && overlay.getHeight()==h
                && !changed && state.face==drawnFace && generation==drawnGeneration) return;
        boolean resized=overlay==null || overlay.getWidth()!=w || overlay.getHeight()!=h;
        if(resized) {
            if(overlay!=null) overlay.recycle();
            overlay=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        }
        Canvas canvas=new Canvas(overlay); canvas.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR);
        renderer.draw(canvas,smoother.points(),state.face);
        drawnFace=state.face; drawnGeneration=generation;
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,overlayTexture);
        if(resized) GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,overlay,0);
        else GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D,0,0,0,overlay);
    }

    private void analyze(Input input,CameraEffectState state) {
        long now=SystemClock.elapsedRealtime();
        if(state.face==CameraEffectState.Face.NONE) { input.clearFence(); return; }
        // GLES 3 transfers into a PBO. A later frame only maps it after a zero-timeout fence poll.
        if(input.fence!=0) {
            if(input.analysisGeneration!=effects.tracker.generation() || now-input.capturedAt>250) {
                input.clearFence();
            } else {
                int status=GLES30.glClientWaitSync(input.fence,0,0);
                if(status==GLES30.GL_TIMEOUT_EXPIRED) return;
                if(status==GLES30.GL_WAIT_FAILED) input.disableAsync();
                else if(effects.tracker.canSubmit() && input.cpuBufferInUse.compareAndSet(false,true)) {
                    GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER,input.pbo);
                    java.nio.Buffer mapped=GLES30.glMapBufferRange(GLES30.GL_PIXEL_PACK_BUFFER,0,input.byteCount,GLES30.GL_MAP_READ_BIT);
                    boolean copied=false;
                    if(mapped instanceof ByteBuffer) {
                        input.readback.clear();
                        ByteBuffer bytes=(ByteBuffer)mapped; bytes.position(0); bytes.limit(input.byteCount);
                        input.readback.put(bytes);
                        copied=GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER);
                    }
                    GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER,0);
                    if(copied) input.submitReadback();
                    else { input.cpuBufferInUse.set(false); input.disableAsync(); }
                    input.clearFence();
                } else { input.clearFence(); }
            }
        }
        if(!effects.tracker.canSubmit() || input.cpuBufferInUse.get()
                || now-lastAnalysis<(asyncReadback?80:125)) return;
        lastAnalysis=now;
        input.capturedAt=now; input.analysisGeneration=effects.tracker.generation(); input.analysisRotation=input.rotation;
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,input.analysisFramebuffer);
        GLES20.glViewport(0,0,input.analysisWidth,input.analysisHeight);
        draw(copyProgram,input.texture,GLES20.GL_TEXTURE_2D,analysisTransform);
        if(asyncReadback) {
            if(input.pbo==0) {
                int[] id=new int[1]; GLES30.glGenBuffers(1,id,0); input.pbo=id[0];
                GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER,input.pbo);
                GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER,input.byteCount,null,GLES30.GL_STREAM_READ);
            } else GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER,input.pbo);
            GLES30.glReadPixels(0,0,input.analysisWidth,input.analysisHeight,GLES30.GL_RGBA,GLES30.GL_UNSIGNED_BYTE,0);
            int error=GLES30.glGetError();
            if(error==GLES30.GL_NO_ERROR) input.fence=GLES30.glFenceSync(GLES30.GL_SYNC_GPU_COMMANDS_COMPLETE,0);
            GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER,0);
            if(error!=GLES30.GL_NO_ERROR || input.fence==0) input.disableAsync();
            else GLES30.glFlush();
        } else if(input.cpuBufferInUse.compareAndSet(false,true)) {
            // Compatibility path: smaller, rate-limited reads after presentation, without Java pixel loops.
            input.readback.clear();
            GLES20.glReadPixels(0,0,input.analysisWidth,input.analysisHeight,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,input.readback);
            input.submitReadback();
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
    }

    private void init() {
        if(display!=null) return;
        display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if(!EGL14.eglInitialize(display,new int[2],0,new int[2],0)) throw new IllegalStateException("EGL initialize failed");
        if(!createContext(3)) {
            if(!createContext(2)) throw new IllegalStateException("No recordable EGL context");
        }
        pbuffer=EGL14.eglCreatePbufferSurface(display,config,new int[]{EGL14.EGL_WIDTH,1,EGL14.EGL_HEIGHT,1,EGL14.EGL_NONE},0);
        current(pbuffer);
        int[] id=new int[1]; GLES20.glGenBuffers(1,id,0); vertexBuffer=id[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,vertexBuffer);
        FloatBuffer mesh=buffer(new float[]{-1,-1,0,0, 1,-1,1,0, -1,1,0,1, 1,1,1,1});
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,16*4,mesh,GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);
        externalProgram=program(EXTERNAL); copyProgram=program(COPY); compositeProgram=program(COMPOSITE);
        overlayTexture=texture(GLES20.GL_TEXTURE_2D);
    }

    private boolean createContext(int version) {
        EGLConfig[] configs=new EGLConfig[1]; int[] count=new int[1];
        int[] attributes={EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,
                EGL14.EGL_RENDERABLE_TYPE,version==3?0x40:EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT|EGL14.EGL_PBUFFER_BIT,
                0x3142,1,EGL14.EGL_NONE}; // EGL_RECORDABLE_ANDROID
        if(!EGL14.eglChooseConfig(display,attributes,0,configs,0,1,count,0) || count[0]==0)
            return false;
        config=configs[0];
        context=EGL14.eglCreateContext(display,config,EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,version,EGL14.EGL_NONE},0);
        if(context==EGL14.EGL_NO_CONTEXT) return false;
        asyncReadback=version>=3;
        return true;
    }

    private void current(EGLSurface surface) {
        if(!EGL14.eglMakeCurrent(display,surface,surface,context)) throw new IllegalStateException("EGL context lost");
    }
    private void draw(int program,int texture,int target,float[] transform) {
        GLES20.glUseProgram(program);
        Locations loc=locations.get(program);
        int position=loc.position, tex=loc.tex;
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,vertexBuffer);
        GLES20.glEnableVertexAttribArray(position); GLES20.glEnableVertexAttribArray(tex);
        GLES20.glVertexAttribPointer(position,2,GLES20.GL_FLOAT,false,16,0);
        GLES20.glVertexAttribPointer(tex,2,GLES20.GL_FLOAT,false,16,8);
        GLES20.glUniformMatrix4fv(loc.transform,1,false,transform,0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0); GLES20.glBindTexture(target,texture);
        GLES20.glUniform1i(loc.texture,0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        GLES20.glDisableVertexAttribArray(position); GLES20.glDisableVertexAttribArray(tex);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);
    }
    private int texture(int target) {
        int[] id=new int[1]; GLES20.glGenTextures(1,id,0); GLES20.glBindTexture(target,id[0]);
        GLES20.glTexParameteri(target,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameteri(target,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        GLES20.glTexParameteri(target,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(target,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE); return id[0];
    }
    private int framebuffer(int texture,int w,int h) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,w,h,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);
        int[] id=new int[1]; GLES20.glGenFramebuffers(1,id,0); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,id[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,texture,0);
        if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)!=GLES20.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Incomplete effect framebuffer");
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0); return id[0];
    }
    private int program(String fragment) {
        int vertex=shader(GLES20.GL_VERTEX_SHADER,VERTEX), frag=shader(GLES20.GL_FRAGMENT_SHADER,fragment);
        int program=GLES20.glCreateProgram(); GLES20.glAttachShader(program,vertex); GLES20.glAttachShader(program,frag);
        GLES20.glLinkProgram(program); int[] ok=new int[1]; GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);
        GLES20.glDeleteShader(vertex); GLES20.glDeleteShader(frag);
        if(ok[0]==0) throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
        locations.put(program,new Locations(program));
        return program;
    }
    private int shader(int type,String source) {
        int shader=GLES20.glCreateShader(type); GLES20.glShaderSource(shader,source); GLES20.glCompileShader(shader);
        int[] ok=new int[1]; GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,ok,0);
        if(ok[0]==0) throw new IllegalStateException(GLES20.glGetShaderInfoLog(shader)); return shader;
    }
    private static FloatBuffer buffer(float[] data) {
        FloatBuffer result=ByteBuffer.allocateDirect(data.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        result.put(data).position(0); return result;
    }
    private void removeOutput(SurfaceOutput source) {
        for(int i=outputs.size()-1;i>=0;i--) if(outputs.get(i).source==source) {
            EGL14.eglDestroySurface(display,outputs.remove(i).window); source.close();
        }
    }
    private void report(Throwable error) { if(!failed && !closing) { failed=true; onError.accept(error); } }
    @Override public void close() {
        closing=true;
        executor.execute(() -> {
            for(Output o:new ArrayList<>(outputs)) removeOutput(o.source);
            if(pendingInputs==0) dispose();
        });
    }
    private void dispose() {
        if(disposed) return; disposed=true;
        if(display!=null && display!=EGL14.EGL_NO_DISPLAY) {
            if(context!=null && context!=EGL14.EGL_NO_CONTEXT && pbuffer!=null && pbuffer!=EGL14.EGL_NO_SURFACE
                    && EGL14.eglMakeCurrent(display,pbuffer,pbuffer,context)) {
                GLES20.glDeleteProgram(externalProgram); GLES20.glDeleteProgram(copyProgram); GLES20.glDeleteProgram(compositeProgram);
                GLES20.glDeleteTextures(1,new int[]{overlayTexture},0);
                GLES20.glDeleteBuffers(1,new int[]{vertexBuffer},0);
            }
            EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
            if(pbuffer!=null && pbuffer!=EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display,pbuffer);
            if(context!=null && context!=EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display,context);
            EGL14.eglTerminate(display); EGL14.eglReleaseThread();
        }
        if(overlay!=null) { overlay.recycle(); overlay=null; }
        thread.quitSafely();
    }
    private static final class Output {
        final SurfaceOutput source; final EGLSurface window;
        Output(SurfaceOutput source,EGLSurface window) { this.source=source; this.window=window; }
    }
    private static final class Locations {
        final int position,tex,transform,texture,color,offset,overlay;
        Locations(int program) {
            position=GLES20.glGetAttribLocation(program,"aPosition"); tex=GLES20.glGetAttribLocation(program,"aTex");
            transform=GLES20.glGetUniformLocation(program,"uTransform"); texture=GLES20.glGetUniformLocation(program,"uTexture");
            color=GLES20.glGetUniformLocation(program,"uColor"); offset=GLES20.glGetUniformLocation(program,"uOffset");
            overlay=GLES20.glGetUniformLocation(program,"uOverlay");
        }
    }
    private final class Input {
        final int width,height,external,texture,framebuffer,analysisTexture,analysisFramebuffer,analysisWidth,analysisHeight;
        final SurfaceTexture stream; final Surface surface;
        final ByteBuffer readback;
        final int byteCount;
        final AtomicBoolean cpuBufferInUse=new AtomicBoolean();
        final Runnable releaseReadback=() -> cpuBufferInUse.set(false);
        final Runnable renderTask=() -> { renderScheduled=false; render(this); };
        int rotation,analysisRotation,analysisGeneration,pbo;
        long fence,capturedAt;
        boolean renderScheduled,released;
        Input(int width,int height) {
            this.width=width; this.height=height;
            external=texture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
            stream=new SurfaceTexture(external); stream.setDefaultBufferSize(width,height); surface=new Surface(stream);
            texture=texture(GLES20.GL_TEXTURE_2D); framebuffer=framebuffer(texture,width,height);
            analysisWidth=Math.max(1,256*width/Math.max(width,height));
            analysisHeight=Math.max(1,256*height/Math.max(width,height));
            analysisTexture=texture(GLES20.GL_TEXTURE_2D); analysisFramebuffer=framebuffer(analysisTexture,analysisWidth,analysisHeight);
            byteCount=analysisWidth*analysisHeight*4;
            readback=ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
        }
        void submitReadback() {
            effects.tracker.submitPixels(readback,analysisWidth,analysisHeight,analysisRotation,
                    capturedAt,analysisGeneration,releaseReadback);
        }
        void clearFence() {
            if(fence!=0) { GLES30.glDeleteSync(fence); fence=0; }
        }
        void disableAsync() {
            clearFence();
            if(pbo!=0) { GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER,0); GLES30.glDeleteBuffers(1,new int[]{pbo},0); pbo=0; }
            asyncReadback=false;
        }
        void release() {
            released=true; handler.removeCallbacks(renderTask); clearFence();
            if(pbo!=0) GLES30.glDeleteBuffers(1,new int[]{pbo},0);
            stream.setOnFrameAvailableListener(null); surface.release(); stream.release();
            GLES20.glDeleteTextures(3,new int[]{external,texture,analysisTexture},0);
            GLES20.glDeleteFramebuffers(2,new int[]{framebuffer,analysisFramebuffer},0);
        }
    }
}
