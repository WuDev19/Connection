package com.example.connection.ui.camera;

import com.example.connection.ui.camera.effects.CameraEffectState;
import com.example.connection.ui.camera.effects.CameraEffectsProcessor;
import com.example.connection.ui.camera.effects.CameraGpuEffect;
import com.example.connection.ui.camera.effects.FaceEffectException;
import com.example.connection.R;
import androidx.camera.core.UseCaseGroup;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MirrorMode;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraManager {

    private CameraEffectState effectState = new CameraEffectState();
    private CameraEffectsProcessor effects;
    private CameraGpuEffect gpuEffect;
    private final MutableLiveData<String> effectError = new MutableLiveData<>();

    public CameraEffectState getEffectState() { return effectState; }
    public void setEffectState(CameraEffectState value) {
        effectState = value;
        if (effects != null) effects.setState(value);
    }
    public LiveData<String> getEffectError() { return effectError; }

    private static final String TAG = "CameraManager";

    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private ProcessCameraProvider cameraProvider;
    private CameraControl cameraControl;
    private LiveData<ZoomState> zoomSource;
    private Observer<ZoomState> zoomObserver;
    private final MutableLiveData<ZoomState> zoomState = new MutableLiveData<>();
    private int cameraGeneration;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private boolean isBackFacing = true;

    public interface OnPhotoCapturedListener {
        void onSuccess(Bitmap photo);
        void onError(Exception exception);
    }

    public interface OnVideoFinalizedListener {
        void onFinalized(File videoFile);
        void onError(Exception exception);
    }

    public void startCamera(@NonNull Context context,
                            @NonNull LifecycleOwner lifecycleOwner,
                            @NonNull PreviewView previewView,
                            int displayRotation,
                            Runnable onReady) {
        final int generation = ++cameraGeneration;
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);
        future.addListener(() -> {
            if (generation != cameraGeneration || lifecycleOwner.getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) return;
            try {
                cameraProvider = future.get();
                bindCameraUseCases(lifecycleOwner, previewView, displayRotation);
                if (onReady != null) {
                    onReady.run();
                }
            } catch (Exception e) {
                Log.e(TAG, "startCamera failed: " + e.getMessage(), e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void flipCamera(@NonNull Context context,
                           @NonNull LifecycleOwner lifecycleOwner,
                           @NonNull PreviewView previewView,
                           int displayRotation) {
        if (recording != null) return;
        isBackFacing = !isBackFacing;
        if (cameraProvider != null) {
            bindCameraUseCases(lifecycleOwner, previewView, displayRotation);
        } else {
            startCamera(context, lifecycleOwner, previewView, displayRotation, null);
        }
    }

    private void bindCameraUseCases(@NonNull LifecycleOwner lifecycleOwner,
                                   @NonNull PreviewView previewView,
                                   int displayRotation) {
        if (cameraProvider == null) return;
        clearZoomObserver();
        if (effects == null) {
            Context appContext = previewView.getContext().getApplicationContext();
            effects = new CameraEffectsProcessor(appContext,
                    error -> reportFaceEffectError(appContext, error));
            effects.setState(effectState);
        }
        effects.resetTracking();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(displayRotation)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build();

        QualitySelector qualitySelector = QualitySelector.fromOrderedList(
                Arrays.asList(Quality.HD, Quality.SD),
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        );
        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build();
        videoCapture = new VideoCapture.Builder<>(recorder)
                .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(isBackFacing ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                .build();

        cameraProvider.unbindAll();
        if (gpuEffect != null) gpuEffect.close();
        gpuEffect = new CameraGpuEffect(effects,
                error -> {
                    Log.e(TAG, "Camera effect failed", error);
                    effectError.postValue("Không xử lý được filter camera. Hãy mở lại camera để thử lại.");
                });
        UseCaseGroup group = new UseCaseGroup.Builder()
                .addUseCase(preview).addUseCase(imageCapture).addUseCase(videoCapture)
                .addEffect(gpuEffect.asCameraEffect()).build();
        Camera camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                group
        );
        cameraControl = camera.getCameraControl();
        final LiveData<ZoomState> source = camera.getCameraInfo().getZoomState();
        zoomSource = source;
        zoomObserver = state -> {
            if (source == zoomSource) zoomState.setValue(state);
        };
        source.observe(lifecycleOwner, zoomObserver);
        ZoomState initial = source.getValue();
        if (initial != null) {
            setZoomRatio(CameraZoomMath.clampRatio(1f, initial.getMinZoomRatio(), initial.getMaxZoomRatio()));
        }
    }

    private void reportFaceEffectError(Context context, Throwable error) {
        Log.e(TAG, "Face effect failed", error);
        int message = R.string.camera_face_error_inference;
        if (error instanceof FaceEffectException) {
            switch (((FaceEffectException) error).stage) {
                case MODEL_LOAD: message = R.string.camera_face_error_model; break;
                case DETECTOR_INIT: message = R.string.camera_face_error_init; break;
                case FRAME_PREPARATION: message = R.string.camera_face_error_frame; break;
                case INFERENCE: break;
            }
        }
        effectError.postValue(context.getString(message));
    }

    public void takePhoto(@NonNull OnPhotoCapturedListener listener) {
        if (imageCapture == null) {
            listener.onError(new IllegalStateException("ImageCapture not initialized"));
            return;
        }

        final CameraEffectState snapshot = effectState;
        final CameraEffectsProcessor processor = effects;
        final boolean mirror = !isBackFacing;
        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);
                Bitmap source = null;
                Bitmap upright = null;
                Bitmap processed = null;
                try {
                    source = image.toBitmap();
                    int rotation = image.getImageInfo().getRotationDegrees();
                    Matrix matrix = new Matrix(); matrix.postRotate(rotation);
                    upright = rotation == 0 ? source : Bitmap.createBitmap(source, 0, 0,
                            source.getWidth(), source.getHeight(), matrix, true);
                    if (upright != source) { source.recycle(); source = null; }
                    processed = processor == null ? upright.copy(Bitmap.Config.ARGB_8888, true)
                            : processor.processPhoto(upright, snapshot);
                    if (upright == source) source = null;
                    upright.recycle(); upright = null;
                    Bitmap result = mirror ? flipBitmapHorizontally(processed) : processed;
                    if (result != processed) processed.recycle();
                    listener.onSuccess(result);
                } catch (Exception e) {
                    if (processed != null && !processed.isRecycled()) processed.recycle();
                    listener.onError(e);
                } finally {
                    if (upright != null && upright != source) upright.recycle();
                    if (source != null) source.recycle();
                    image.close();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                super.onError(exception);
                listener.onError(exception);
            }
        });
    }

    public void startVideoRecording(@NonNull Context context,
                                    @NonNull File outputFile,
                                    @NonNull OnVideoFinalizedListener listener) {
        if (videoCapture == null) {
            listener.onError(new IllegalStateException("VideoCapture not initialized"));
            return;
        }

        FileOutputOptions options = new FileOutputOptions.Builder(outputFile).build();
        recording = videoCapture.getOutput()
                .prepareRecording(context, options)
                .start(cameraExecutor, event -> {
                    if (event instanceof VideoRecordEvent.Finalize) {
                        VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) event;
                        if (!finalizeEvent.hasError()) {
                            listener.onFinalized(outputFile);
                        } else {
                            listener.onError(new RuntimeException("Video recording failed with error code: " + finalizeEvent.getError()));
                        }
                    }
                });
    }

    public void stopVideoRecording() {
        if (recording != null) {
            recording.stop();
            recording = null;
        }
    }

    public void setLinearZoom(float zoom) {
        if (cameraControl != null) {
            cameraControl.setLinearZoom(CameraZoomMath.clampLinear(zoom));
        }
    }

    public LiveData<ZoomState> getZoomState() { return zoomState; }

    public boolean isReady() { return cameraControl != null && videoCapture != null && imageCapture != null; }

    public void scaleZoom(float factor) {
        ZoomState state = zoomSource == null ? null : zoomSource.getValue();
        if (state == null) return;
        setZoomRatio(CameraZoomMath.scaleRatio(state.getZoomRatio(), factor,
                state.getMinZoomRatio(), state.getMaxZoomRatio()));
    }

    public void setZoomRatio(float ratio) {
        ZoomState state = zoomSource == null ? null : zoomSource.getValue();
        if (cameraControl == null || state == null) return;
        cameraControl.setZoomRatio(CameraZoomMath.clampRatio(ratio, state.getMinZoomRatio(), state.getMaxZoomRatio()));
    }

    private void clearZoomObserver() {
        if (zoomSource != null && zoomObserver != null) zoomSource.removeObserver(zoomObserver);
        zoomSource = null;
        zoomObserver = null;
        zoomState.setValue(null);
    }

    public void releaseCamera() {
        cameraGeneration++;
        stopVideoRecording();
        clearZoomObserver();
        cameraControl = null;
        imageCapture = null;
        videoCapture = null;
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (gpuEffect != null) { gpuEffect.close(); gpuEffect = null; }
        if (effects != null) { effects.close(); effects = null; }
    }

    public boolean isBackFacing() {
        return isBackFacing;
    }

    public void shutdown() {
        releaseCamera();
        cameraExecutor.shutdown();
    }

    public static Bitmap flipBitmapHorizontally(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
