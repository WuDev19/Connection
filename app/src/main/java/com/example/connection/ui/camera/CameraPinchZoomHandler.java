package com.example.connection.ui.camera;

import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.function.Consumer;

/** Camera zoom gestures never resize PreviewView and never synthesize a shutter press. */
public final class CameraPinchZoomHandler implements View.OnTouchListener {
    private final View preview;
    private final ScaleGestureDetector scales;
    private final GestureDetector taps;
    private final Consumer<Boolean> intercept;
    private boolean multiTouch;

    public CameraPinchZoomHandler(View preview, Consumer<Float> onScale, Consumer<Boolean> intercept) {
        this.preview = preview;
        this.intercept = intercept;
        scales = new ScaleGestureDetector(preview.getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector detector) {
                onScale.accept(detector.getScaleFactor());
                return true;
            }
        });
        scales.setQuickScaleEnabled(false);
        scales.setStylusScaleEnabled(false);
        taps = new GestureDetector(preview.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onSingleTapUp(MotionEvent event) {
                if (!multiTouch) preview.performClick();
                return true;
            }
        });
        taps.setIsLongpressEnabled(false);
    }

    @Override public boolean onTouch(View view, MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) multiTouch = false;
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            multiTouch = true;
            cancelTaps();
            intercept.accept(true);
        }
        scales.onTouchEvent(event);
        if (!multiTouch) taps.onTouchEvent(event);
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (action == MotionEvent.ACTION_CANCEL) cancelTaps();
            intercept.accept(false);
        }
        return true;
    }

    private void cancelTaps() {
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        taps.onTouchEvent(cancel);
        cancel.recycle();
    }

    public void cancel() {
        cancelTaps();
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        scales.onTouchEvent(cancel);
        cancel.recycle();
        multiTouch = false;
        intercept.accept(false);
    }

    public void detach() {
        cancel();
        preview.setOnTouchListener(null);
    }
}
