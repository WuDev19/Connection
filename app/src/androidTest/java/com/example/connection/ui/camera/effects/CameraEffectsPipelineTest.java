package com.example.connection.ui.camera.effects;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Size;
import android.view.Surface;

import androidx.camera.core.CameraEffect;
import androidx.camera.core.SurfaceOutput;
import androidx.camera.core.SurfaceRequest;
import androidx.core.util.Consumer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Synthetic surfaces only: never opens the camera or records the user's environment. */
@RunWith(AndroidJUnit4.class)
@SuppressLint("RestrictedApi")
public class CameraEffectsPipelineTest {
    private static final int WIDTH=320, HEIGHT=240;
    @Test public void photoAppliesMonochromeAndAllFaceArtworkDraws() {
        AtomicReference<Throwable> error=new AtomicReference<>();
        CameraEffectsProcessor processor=new CameraEffectsProcessor(context(),error::set);
        Bitmap input=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888); input.eraseColor(Color.RED);
        Bitmap output=processor.processPhoto(input,new CameraEffectState().withColor(CameraEffectState.Color.MONO).withIntensity(1));
        int pixel=output.getPixel(128,128);
        assertEquals(Color.red(pixel),Color.green(pixel)); assertEquals(Color.red(pixel),Color.blue(pixel));
        float[] face=new float[478*2];
        face[33*2]=.35f; face[33*2+1]=.5f; face[263*2]=.65f; face[263*2+1]=.5f;
        face[234*2]=.2f; face[234*2+1]=.5f; face[454*2]=.8f; face[454*2+1]=.5f;
        Bitmap overlay=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);
        for(CameraEffectState.Face effect:CameraEffectState.Face.values()) {
            if(effect==CameraEffectState.Face.NONE) continue;
            overlay.eraseColor(Color.TRANSPARENT);
            new FaceEffectRenderer().draw(new Canvas(overlay),face,effect);
            int[] pixels=new int[256*256]; overlay.getPixels(pixels,0,256,0,0,256,256);
            int count=0; for(int p:pixels) if(Color.alpha(p)>0) count++;
            assertTrue(effect.name(),count>20);
        }
        assertNull(error.get()); processor.close(); input.recycle(); output.recycle(); overlay.recycle();
    }

    @Test public void modelLoadsAndMissingFaceProducesNoOverlay() {
        AtomicReference<Throwable> error=new AtomicReference<>();
        FaceEffectTracker tracker=new FaceEffectTracker(context(),error::set);
        Bitmap blank=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888); blank.eraseColor(Color.GRAY);
        assertNull(tracker.detect(blank)); assertNull(String.valueOf(error.get()),error.get());
        tracker.close(); blank.recycle();
    }

    @Test public void modelFindsSampleFaceAndPhotoBakesArtwork() throws Exception {
        AtomicReference<Throwable> error=new AtomicReference<>();
        CameraEffectsProcessor processor=new CameraEffectsProcessor(context(),error::set);
        Bitmap portrait;
        try(java.io.InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("portrait.jpg")) {
            portrait=android.graphics.BitmapFactory.decodeStream(in);
        }
        float[] points=processor.tracker.detect(portrait);
        assertNull(String.valueOf(error.get()),error.get()); assertNotNull(points); assertTrue(points.length>=936);
        CameraEffectState state=new CameraEffectState().withFace(CameraEffectState.Face.CHEEK_HEARTS);
        Bitmap processed=processor.processPhoto(portrait,state);
        int changed=0;
        for(int y=0;y<portrait.getHeight();y+=2) for(int x=0;x<portrait.getWidth();x+=2)
            if(processed.getPixel(x,y)!=portrait.getPixel(x,y)) changed++;
        assertTrue("Face artwork is baked into saved bitmap",changed>20);
        processor.close(); portrait.recycle(); processed.recycle();
    }

    @Test public void cameraUseCasesBindOnBothLensesWithoutStartingCamera() throws Exception {
        CountDownLatch ready=new CountDownLatch(1);
        AtomicReference<Throwable> error=new AtomicReference<>();
        AtomicReference<com.example.connection.ui.camera.CameraManager> manager=new AtomicReference<>();
        AtomicReference<androidx.lifecycle.LifecycleRegistry> lifecycle=new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            androidx.lifecycle.LifecycleOwner owner=new androidx.lifecycle.LifecycleOwner() {
                final androidx.lifecycle.LifecycleRegistry registry=new androidx.lifecycle.LifecycleRegistry(this);
                @Override public androidx.lifecycle.Lifecycle getLifecycle() { return registry; }
            };
            lifecycle.set((androidx.lifecycle.LifecycleRegistry)owner.getLifecycle());
            lifecycle.get().setCurrentState(androidx.lifecycle.Lifecycle.State.CREATED);
            com.example.connection.ui.camera.CameraManager camera=new com.example.connection.ui.camera.CameraManager();
            manager.set(camera);
            androidx.camera.view.PreviewView preview=new androidx.camera.view.PreviewView(context());
            camera.startCamera(context(),owner,preview,Surface.ROTATION_0,() -> {
                try {
                    assertTrue(camera.isReady());
                    camera.flipCamera(context(),owner,preview,Surface.ROTATION_0);
                    assertTrue(camera.isReady()); assertFalse(camera.isBackFacing());
                } catch(Throwable e) { error.set(e); }
                finally { ready.countDown(); }
            });
        });
        try {
            assertTrue("CameraX effect use-case binding completed",ready.await(15,TimeUnit.SECONDS));
            assertNull(String.valueOf(error.get()),error.get());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                manager.get().shutdown(); lifecycle.get().setCurrentState(androidx.lifecycle.Lifecycle.State.DESTROYED);
            });
        }
    }

    @Test public void gpuFiltersPreviewAndActualEncodedVideo() throws Exception {
        AtomicReference<Throwable> error=new AtomicReference<>();
        CameraEffectsProcessor processor=new CameraEffectsProcessor(context(),error::set);
        processor.setState(new CameraEffectState().withColor(CameraEffectState.Color.MONO).withIntensity(1));
        CameraGpuEffect gpu=new CameraGpuEffect(processor,error::set);
        Executor gl=gpu.asCameraEffect().getExecutor();
        ImageReader reader=ImageReader.newInstance(WIDTH,HEIGHT,PixelFormat.RGBA_8888,3);
        MediaCodec encoder=MediaCodec.createEncoderByType("video/avc");
        MediaFormat format=MediaFormat.createVideoFormat("video/avc",WIDTH,HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE,200_000); format.setInteger(MediaFormat.KEY_FRAME_RATE,15);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
        encoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        Surface encoderSurface=encoder.createInputSurface(); encoder.start();
        File file=File.createTempFile("effects-synthetic-",".mp4",context().getCacheDir());
        MediaMuxer muxer=new MediaMuxer(file.getPath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        SurfaceRequest request=new SurfaceRequest(new Size(WIDTH,HEIGHT),null,() -> {});
        boolean muxerStarted=false;
        try {
            CountDownLatch ready=new CountDownLatch(1);
            gl.execute(() -> {
                gpu.onOutputSurface(new TestOutput(reader.getSurface(),CameraEffect.PREVIEW));
                gpu.onOutputSurface(new TestOutput(encoderSurface,CameraEffect.VIDEO_CAPTURE));
                gpu.onInputSurface(request); ready.countDown();
            });
            assertTrue(ready.await(10,TimeUnit.SECONDS)); assertNull(String.valueOf(error.get()),error.get());
            Surface input=request.getDeferrableSurface().getSurface().get(10,TimeUnit.SECONDS);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo(); int track=-1;
            int previewPixel=0;
            for(int frame=0;frame<12;frame++) {
                Canvas canvas=input.lockCanvas(null); canvas.drawColor(Color.RED); input.unlockCanvasAndPost(canvas);
                SystemClock.sleep(80);
                try(Image image=reader.acquireLatestImage()) {
                    if(image!=null) {
                        Image.Plane plane=image.getPlanes()[0]; ByteBuffer b=plane.getBuffer();
                        int i=(HEIGHT/2)*plane.getRowStride()+(WIDTH/2)*plane.getPixelStride();
                        previewPixel=Color.rgb(b.get(i)&255,b.get(i+1)&255,b.get(i+2)&255);
                    }
                }
                int index;
                while((index=encoder.dequeueOutputBuffer(info,0))!=MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if(index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) { track=muxer.addTrack(encoder.getOutputFormat()); muxer.start(); muxerStarted=true; }
                    else if(index>=0) {
                        if(info.size>0 && (info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0)
                            muxer.writeSampleData(track,encoder.getOutputBuffer(index),info);
                        encoder.releaseOutputBuffer(index,false);
                    }
                }
            }
            assertTrue("Preview frame received",Color.red(previewPixel)>30);
            assertTrue(Math.abs(Color.red(previewPixel)-Color.green(previewPixel))<4);
            assertTrue(Math.abs(Color.red(previewPixel)-Color.blue(previewPixel))<4);
            encoder.signalEndOfInputStream();
            long deadline=SystemClock.elapsedRealtime()+5000;
            while(SystemClock.elapsedRealtime()<deadline) {
                int index=encoder.dequeueOutputBuffer(info,100_000);
                if(index>=0) {
                    if(info.size>0 && (info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)==0)
                        muxer.writeSampleData(track,encoder.getOutputBuffer(index),info);
                    encoder.releaseOutputBuffer(index,false);
                    if((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0) break;
                }
            }
            assertTrue(muxerStarted); muxer.stop(); muxerStarted=false;
            try(MediaMetadataRetriever retriever=new MediaMetadataRetriever()) {
                retriever.setDataSource(file.getPath()); Bitmap decoded=retriever.getFrameAtTime();
                assertNotNull(decoded); int p=decoded.getPixel(WIDTH/2,HEIGHT/2); decoded.recycle();
                assertTrue(Color.red(p)>25); assertTrue(Math.abs(Color.red(p)-Color.green(p))<10);
                assertTrue(Math.abs(Color.red(p)-Color.blue(p))<10);
            }
            assertNull(String.valueOf(error.get()),error.get());
        } finally {
            request.getDeferrableSurface().close(); gpu.close(); processor.close();
            CountDownLatch drained=new CountDownLatch(1); gl.execute(drained::countDown); drained.await(5,TimeUnit.SECONDS);
            encoder.stop(); encoder.release(); encoderSurface.release(); reader.close();
            if(muxerStarted) muxer.stop(); muxer.release(); file.delete();
        }
    }

    private android.content.Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static final class TestOutput implements SurfaceOutput {
        private final Surface surface; private final int targets;
        TestOutput(Surface surface,int targets) { this.surface=surface; this.targets=targets; }
        @Override public Surface getSurface(Executor executor,Consumer<Event> listener) { return surface; }
        @Override public int getTargets() { return targets; }
        @Override public Size getSize() { return new Size(WIDTH,HEIGHT); }
        @Override public void updateTransformMatrix(float[] output,float[] input) { Matrix.setIdentityM(output,0); }
        @Override public void close() {}
    }
}
