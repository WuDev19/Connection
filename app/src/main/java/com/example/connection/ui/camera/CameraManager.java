package com.example.connection.ui.camera;

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
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
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

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraManager {

    private static final String TAG = "CameraManager";

    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private ProcessCameraProvider cameraProvider;
    private CameraControl cameraControl;
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
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(context);
        future.addListener(() -> {
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

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(displayRotation)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build();

        Recorder recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.SD))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(isBackFacing ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                .build();

        cameraProvider.unbindAll();
        Camera camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
        );
        cameraControl = camera.getCameraControl();
    }

    public void takePhoto(@NonNull OnPhotoCapturedListener listener) {
        if (imageCapture == null) {
            listener.onError(new IllegalStateException("ImageCapture not initialized"));
            return;
        }

        imageCapture.takePicture(cameraExecutor, new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                super.onCaptureSuccess(image);
                Bitmap bitmap = image.toBitmap();
                if (!isBackFacing) {
                    bitmap = flipBitmapHorizontally(bitmap);
                }
                image.close();
                listener.onSuccess(bitmap);
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
            cameraControl.setLinearZoom(zoom);
        }
    }

    public boolean isBackFacing() {
        return isBackFacing;
    }

    public void shutdown() {
        if (recording != null) {
            recording.stop();
            recording = null;
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        cameraExecutor.shutdown();
    }

    public static Bitmap flipBitmapHorizontally(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1, 1);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
