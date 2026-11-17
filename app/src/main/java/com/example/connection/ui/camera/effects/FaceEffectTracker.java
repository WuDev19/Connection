package com.example.connection.ui.camera.effects;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.Log;

import com.google.mediapipe.framework.image.ByteBufferImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** One preview job in flight. Model inference and bitmap conversion never run on the GL thread. */
final class FaceEffectTracker implements AutoCloseable {
    private static final String TAG = "FaceEffectTracker";
    private static final int MAX_FRAME_FAILURES = 3;
    private static final long FALLBACK_INTERVAL_MS = 160;
    private final Context context;
    private final java.util.function.Consumer<Throwable> onError;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean();
    private final Object submissionLock = new Object();
    private FaceLandmarker photoDetector, videoDetector, previewImageDetector;
    private ByteBuffer model;
    private volatile boolean closed, modelUnavailable, previewStopped, photoStopped;
    private volatile long nextPreviewAt;
    private boolean imageFallback;
    private int previewFailures, photoFailures;
    private volatile Result latest;
    private volatile int generation;
    // Only the worker owns these reusable preview bitmaps and VIDEO task.
    private Bitmap previewBitmap, uprightBitmap;
    // Separate ownership: preview runs on worker; photo runs under this tracker's monitor.
    private final RgbaInput previewInput = new RgbaInput();
    private final RgbaInput photoInput = new RgbaInput();
    private final Matrix rotationMatrix = new Matrix();
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private long lastVideoTimestamp;
    private int videoGeneration = -1;

    FaceEffectTracker(Context context, java.util.function.Consumer<Throwable> onError) {
        this.context = context.getApplicationContext();
        this.onError = onError;
    }

    boolean canSubmit() {
        return !closed && !modelUnavailable && !previewStopped && !busy.get()
                && SystemClock.elapsedRealtime() >= nextPreviewAt;
    }
    int generation() { return generation; }
    float[] current() {
        Result result = latest;
        return result != null && result.generation == generation
                && SystemClock.elapsedRealtime() - result.capturedAt < 400 ? result.points : null;
    }
    void reset() {
        synchronized (submissionLock) { generation++; latest = null; }
    }

    /** The caller lends a CPU buffer until release runs; no Bitmap is allocated by the renderer. */
    void submitPixels(ByteBuffer pixels, int width, int height, int rotation,
                      long capturedAt, int token, Runnable release) {
        synchronized (submissionLock) {
            if (!canSubmit() || token != generation || !busy.compareAndSet(false, true)) {
                release.run();
                return;
            }
            worker.execute(() -> {
                try {
                    if (token != generation || closed) return;
                    try {
                        if (pixels == null || width <= 0 || height <= 0
                                || pixels.limit() < (long) width * height * 4)
                            throw new IllegalArgumentException("Incomplete RGBA frame " + width + "x" + height);
                        previewBitmap = sizedBitmap(previewBitmap, width, height);
                        pixels.rewind();
                        previewBitmap.copyPixelsFromBuffer(pixels);
                    } catch (RuntimeException e) {
                        throw failure(FaceEffectException.Stage.FRAME_PREPARATION,
                                "Cannot prepare preview " + width + "x" + height + ", rotation=" + rotation, e);
                    }
                    processPreview(previewBitmap, rotation, capturedAt, token);
                } catch (RuntimeException e) { handlePreviewFailure(e, token); }
                finally { release.run(); busy.set(false); }
            });
        }
    }

    /** Compatibility entry point for an already-owned bitmap; ownership transfers to the worker. */
    void submit(Bitmap bitmap, int rotation) {
        synchronized (submissionLock) {
            if (!canSubmit() || !busy.compareAndSet(false, true)) { bitmap.recycle(); return; }
            int token = generation;
            long capturedAt = SystemClock.elapsedRealtime();
            worker.execute(() -> {
                try { if (!closed && token == generation) processPreview(bitmap, rotation, capturedAt, token); }
                catch (RuntimeException e) { handlePreviewFailure(e, token); }
                finally { bitmap.recycle(); busy.set(false); }
            });
        }
    }

    private void processPreview(Bitmap source, int rotation, long capturedAt, int token) {
        Bitmap upright = source;
        try {
            if (rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270)
                throw new IllegalArgumentException("Unsupported frame rotation: " + rotation);
            if (rotation != 0) {
                boolean quarterTurn = rotation == 90 || rotation == 270;
                uprightBitmap = sizedBitmap(uprightBitmap, quarterTurn ? source.getHeight() : source.getWidth(),
                        quarterTurn ? source.getWidth() : source.getHeight());
                rotationMatrix.setRotate(rotation);
                if (rotation == 90) rotationMatrix.postTranslate(source.getHeight(), 0);
                else if (rotation == 180) rotationMatrix.postTranslate(source.getWidth(), source.getHeight());
                else if (rotation == 270) rotationMatrix.postTranslate(0, source.getWidth());
                new Canvas(uprightBitmap).drawBitmap(source, rotationMatrix, bitmapPaint);
                upright = uprightBitmap;
            }
        } catch (RuntimeException e) {
            throw failure(FaceEffectException.Stage.FRAME_PREPARATION, "Cannot rotate preview frame", e);
        }
        if (closed || token != generation) return;
        lastVideoTimestamp = Math.max(lastVideoTimestamp + 1, capturedAt);
        float[] points = detectPreview(upright, token);
        if (points != null && rotation != 0) for (int i = 0; i < points.length; i += 2) {
            float x = points[i], y = points[i + 1];
            if (rotation == 90) { points[i] = y; points[i + 1] = 1 - x; }
            else if (rotation == 180) { points[i] = 1 - x; points[i + 1] = 1 - y; }
            else if (rotation == 270) { points[i] = 1 - y; points[i + 1] = x; }
        }
        synchronized (submissionLock) {
            if (!closed && token == generation) {
                latest = new Result(points, capturedAt, token);
                previewFailures = 0;
                nextPreviewAt = imageFallback ? SystemClock.elapsedRealtime() + FALLBACK_INTERVAL_MS : 0;
            }
        }
    }

    private float[] detectPreview(Bitmap upright, int token) {
        if (!imageFallback) {
            try {
                if (videoGeneration != token) {
                    closeDetector(videoDetector); videoDetector = null;
                    videoGeneration = token;
                }
                if (videoDetector == null) videoDetector = createDetector(RunningMode.VIDEO);
                return infer(videoDetector, upright, true, previewInput);
            } catch (FaceEffectException videoError) {
                if (closed || token != generation) return null;
                if (videoError.stage == FaceEffectException.Stage.MODEL_LOAD
                        || videoError.stage == FaceEffectException.Stage.FRAME_PREPARATION) throw videoError;
                // One-way fallback for this camera session; do not repeatedly rebuild a broken VIDEO task.
                Log.w(TAG, "VIDEO preview failed; switching to bounded IMAGE preview", videoError);
                imageFallback = true;
                closeDetector(videoDetector); videoDetector = null;
                try {
                    previewImageDetector = createDetector(RunningMode.IMAGE);
                    return infer(previewImageDetector, upright, false, previewInput);
                } catch (FaceEffectException imageError) {
                    imageError.addSuppressed(videoError);
                    throw imageError;
                }
            }
        }
        if (previewImageDetector == null) previewImageDetector = createDetector(RunningMode.IMAGE);
        return infer(previewImageDetector, upright, false, previewInput);
    }

    private float[] infer(FaceLandmarker detector, Bitmap bitmap, boolean video, RgbaInput input) {
        MPImage prepared;
        try {
            prepared = input.copyFrom(bitmap);
        } catch (RuntimeException e) {
            throw failure(FaceEffectException.Stage.FRAME_PREPARATION, "Cannot prepare face detector RGBA input", e);
        }
        // BitmapImageBuilder transfers bitmap ownership: MPImage.close() recycles that bitmap.
        // ByteBufferImageBuilder only borrows our buffer until synchronous inference completes.
        try (MPImage image = prepared) {
            return landmarks(video ? detector.detectForVideo(image, lastVideoTimestamp) : detector.detect(image));
        } catch (RuntimeException e) {
            throw failure(FaceEffectException.Stage.INFERENCE,
                    (video ? "VIDEO" : "IMAGE") + " inference failed for " + bitmap.getWidth() + "x" + bitmap.getHeight(), e);
        }
    }

    /** A separate IMAGE task keeps still-image capture independent of VIDEO timestamps/tracking. */
    synchronized float[] detect(Bitmap bitmap) {
        if (closed || modelUnavailable || photoStopped) return null;
        try {
            if (photoDetector == null) photoDetector = createDetector(RunningMode.IMAGE);
            Bitmap small;
            try {
                int max = Math.max(bitmap.getWidth(), bitmap.getHeight());
                small = max > 512 ? Bitmap.createScaledBitmap(bitmap,
                        Math.max(1, bitmap.getWidth() * 512 / max), Math.max(1, bitmap.getHeight() * 512 / max), true) : bitmap;
            } catch (RuntimeException e) {
                throw failure(FaceEffectException.Stage.FRAME_PREPARATION, "Cannot prepare captured photo", e);
            }
            try {
                float[] points = infer(photoDetector, small, false, photoInput);
                photoFailures = 0;
                return points;
            } finally { if (small != bitmap) small.recycle(); }
        } catch (FaceEffectException e) {
            Log.e(TAG, "Photo face effect failed at " + e.stage, e);
            closeDetector(photoDetector); photoDetector = null;
            photoStopped = e.stage == FaceEffectException.Stage.MODEL_LOAD
                    || e.stage == FaceEffectException.Stage.DETECTOR_INIT || ++photoFailures >= MAX_FRAME_FAILURES;
            if (!closed) onError.accept(e);
            return null;
        }
    }

    private synchronized FaceLandmarker createDetector(RunningMode mode) {
        try {
            if (model == null) {
                try (InputStream in = context.getAssets().open("face_landmarker.task");
                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] block = new byte[8192];
                    int n;
                    while ((n = in.read(block)) != -1) out.write(block, 0, n);
                    byte[] bytes = out.toByteArray();
                    model = ByteBuffer.allocateDirect(bytes.length).order(java.nio.ByteOrder.nativeOrder());
                    model.put(bytes).rewind();
                }
            }
        } catch (java.io.IOException | RuntimeException e) {
            modelUnavailable = true;
            throw failure(FaceEffectException.Stage.MODEL_LOAD, "Cannot read bundled face_landmarker.task", e);
        }
        try {
            // Explicit bounds/order for each task; only the immutable model bytes are shared.
            ByteBuffer taskModel = model.duplicate().order(java.nio.ByteOrder.nativeOrder());
            taskModel.position(0); taskModel.limit(model.capacity());
            return FaceLandmarker.createFromOptions(context, FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetBuffer(taskModel).build())
                    .setRunningMode(mode).setNumFaces(1).setMinTrackingConfidence(.5f)
                    .setMinFaceDetectionConfidence(.6f).setMinFacePresenceConfidence(.6f).build());
        } catch (RuntimeException | LinkageError e) {
            throw failure(FaceEffectException.Stage.DETECTOR_INIT, "Cannot initialize " + mode + " face detector", e);
        }
    }

    private static float[] landmarks(FaceLandmarkerResult result) {
        if (result.faceLandmarks().isEmpty()) return null;
        List<NormalizedLandmark> landmarks = result.faceLandmarks().get(0);
        float[] points = new float[landmarks.size() * 2];
        for (int i = 0; i < landmarks.size(); i++) {
            points[i * 2] = landmarks.get(i).x(); points[i * 2 + 1] = landmarks.get(i).y();
        }
        return points;
    }
    private static Bitmap sizedBitmap(Bitmap existing, int width, int height) {
        if (existing != null && !existing.isRecycled()) {
            if (existing.getWidth() == width && existing.getHeight() == height) return existing;
            existing.recycle();
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }
    private static FaceEffectException failure(FaceEffectException.Stage stage, String message, Throwable cause) {
        return new FaceEffectException(stage, message, cause);
    }

    private void handlePreviewFailure(RuntimeException error, int token) {
        FaceEffectException detail = error instanceof FaceEffectException ? (FaceEffectException) error
                : failure(FaceEffectException.Stage.FRAME_PREPARATION, "Unexpected preview frame error", error);
        Log.e(TAG, "Preview face effect failed at " + detail.stage + ", generation=" + token, detail);
        synchronized (submissionLock) {
            if (closed || token != generation) return;
            latest = null;
            boolean permanent = detail.stage == FaceEffectException.Stage.MODEL_LOAD
                    || detail.stage == FaceEffectException.Stage.DETECTOR_INIT;
            previewFailures++;
            previewStopped = permanent || previewFailures >= MAX_FRAME_FAILURES;
            nextPreviewAt = SystemClock.elapsedRealtime() + 300;
        }
        // Only the worker closes preview tasks. A frame error gets bounded retries after a cooldown.
        closeDetector(videoDetector); videoDetector = null;
        closeDetector(previewImageDetector); previewImageDetector = null;
        if (previewStopped && !closed) onError.accept(detail);
    }

    private void closeDetector(FaceLandmarker detector) {
        if (detector == null) return;
        try { detector.close(); }
        catch (RuntimeException e) { Log.w(TAG, "Face detector cleanup failed", e); }
    }
    @Override public void close() {
        synchronized (submissionLock) {
            if (closed) return;
            closed = true; generation++; latest = null;
            worker.execute(() -> {
                closeDetector(videoDetector); videoDetector = null;
                closeDetector(previewImageDetector); previewImageDetector = null;
                if (previewBitmap != null && !previewBitmap.isRecycled()) previewBitmap.recycle(); previewBitmap = null;
                if (uprightBitmap != null && !uprightBitmap.isRecycled()) uprightBitmap.recycle(); uprightBitmap = null;
                previewInput.clear();
                synchronized (this) {
                    closeDetector(photoDetector); photoDetector = null; model = null;
                    photoInput.clear();
                }
            });
            worker.shutdown();
        }
    }
    /** Reused native RGBA storage; never owns or recycles the caller's bitmap. */
    private static final class RgbaInput {
        private ByteBuffer packed, padded;

        MPImage copyFrom(Bitmap bitmap) {
            if (bitmap == null || bitmap.isRecycled())
                throw new IllegalArgumentException("Face detector bitmap has been released");
            if (bitmap.getConfig() != Bitmap.Config.ARGB_8888)
                throw new IllegalArgumentException("Face detector requires ARGB_8888 input");
            int width = bitmap.getWidth(), height = bitmap.getHeight();
            int rowBytes = width * 4, size = rowBytes * height;
            if (packed == null || packed.capacity() != size)
                packed = ByteBuffer.allocateDirect(size).order(java.nio.ByteOrder.nativeOrder());
            packed.clear();
            if (bitmap.getRowBytes() == rowBytes) {
                bitmap.copyPixelsToBuffer(packed);
            } else {
                // Bitmap stride may include padding; MediaPipe expects tightly packed RGBA rows.
                int bitmapBytes = bitmap.getByteCount();
                if (padded == null || padded.capacity() != bitmapBytes)
                    padded = ByteBuffer.allocateDirect(bitmapBytes).order(java.nio.ByteOrder.nativeOrder());
                padded.clear();
                bitmap.copyPixelsToBuffer(padded);
                for (int y = 0; y < height; y++) {
                    int start = y * bitmap.getRowBytes();
                    padded.limit(start + rowBytes); padded.position(start);
                    packed.put(padded);
                }
            }
            packed.flip();
            return new ByteBufferImageBuilder(packed, width, height, MPImage.IMAGE_FORMAT_RGBA).build();
        }

        void clear() { packed = null; padded = null; }
    }

    private static final class Result {
        final float[] points;
        final long capturedAt;
        final int generation;
        Result(float[] points, long capturedAt, int generation) {
            this.points = points; this.capturedAt = capturedAt; this.generation = generation;
        }
    }
}
