package com.example.connection.ui.view.widget;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import com.example.connection.ui.state.MediaZoomState;

/** Owns media gestures so TextureView video and images share identical zoom behavior. */
public final class ZoomableMediaLayout extends FrameLayout {
    private final MediaZoomState zoom = new MediaZoomState();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector taps;
    private final int touchSlop;
    private int activePointer = MotionEvent.INVALID_POINTER_ID;
    private float downX, downY, lastX, lastY, lastFocusX, lastFocusY;
    private boolean multiTouch, dragged, longPressed, doubleTapped;

    public ZoomableMediaLayout(Context context) { this(context, null); }

    public ZoomableMediaLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        setLongClickable(true);
        setFocusable(true);
        setClipChildren(true);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                lastFocusX = detector.getFocusX();
                lastFocusY = detector.getFocusY();
                disallowParent(true);
                return true;
            }

            @Override public boolean onScale(ScaleGestureDetector detector) {
                zoom.panBy(detector.getFocusX() - lastFocusX, detector.getFocusY() - lastFocusY);
                zoom.scaleBy(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                lastFocusX = detector.getFocusX();
                lastFocusY = detector.getFocusY();
                applyTransform();
                return true;
            }
        });
        scaleDetector.setQuickScaleEnabled(false);
        scaleDetector.setStylusScaleEnabled(false);
        taps = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onSingleTapConfirmed(MotionEvent event) {
                if (!multiTouch && !dragged && !longPressed && !doubleTapped) performClick();
                return true;
            }
            @Override public void onLongPress(MotionEvent event) {
                if (!multiTouch && !dragged && !doubleTapped) {
                    longPressed = true;
                    performLongClick();
                }
            }
            @Override public boolean onDoubleTap(MotionEvent event) {
                doubleTapped = true;
                zoom.reset();
                applyTransform();
                // Keep the rest of this gesture from becoming a list/page swipe.
                disallowParent(true);
                return true;
            }
        });
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        // Read raw viewport coordinates, rather than the scaled child's local coordinates.
        return true;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            multiTouch = dragged = longPressed = doubleTapped = false;
            activePointer = event.getPointerId(0);
            downX = lastX = event.getX();
            downY = lastY = event.getY();
            disallowParent(zoom.isZoomed());
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            multiTouch = true;
            cancelTaps();
            disallowParent(true);
        }

        scaleDetector.onTouchEvent(event);
        if (!multiTouch && !dragged) taps.onTouchEvent(event);

        if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1 && zoom.isZoomed()
                && !scaleDetector.isInProgress() && !longPressed && !doubleTapped) {
            int index = event.findPointerIndex(activePointer);
            if (index >= 0) {
                float x = event.getX(index), y = event.getY(index);
                if (!dragged && Math.hypot(x - downX, y - downY) > touchSlop) {
                    dragged = true;
                    cancelTaps();
                }
                if (dragged) {
                    zoom.panBy(x - lastX, y - lastY);
                    applyTransform();
                }
                lastX = x;
                lastY = y;
            }
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            if (event.getPointerId(event.getActionIndex()) == activePointer) {
                int remaining = event.getActionIndex() == 0 ? 1 : 0;
                activePointer = event.getPointerId(remaining);
                downX = lastX = event.getX(remaining);
                downY = lastY = event.getY(remaining);
                dragged = false;
            } else {
                int index = event.findPointerIndex(activePointer);
                if (index >= 0) {
                    downX = lastX = event.getX(index);
                    downY = lastY = event.getY(index);
                }
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (action == MotionEvent.ACTION_CANCEL) cancelTaps();
            activePointer = MotionEvent.INVALID_POINTER_ID;
            disallowParent(false);
        }
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }

    private void cancelTaps() {
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        taps.onTouchEvent(cancel);
        cancel.recycle();
    }

    private void disallowParent(boolean disallow) {
        if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(disallow);
    }

    private void applyTransform() {
        if (getChildCount() == 0) return;
        View content = getChildAt(0);
        content.setPivotX(0);
        content.setPivotY(0);
        content.setScaleX(zoom.getScale());
        content.setScaleY(zoom.getScale());
        content.setTranslationX(zoom.getTranslationX());
        content.setTranslationY(zoom.getTranslationY());
    }

    public void resetZoom() {
        cancelTaps();
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        scaleDetector.onTouchEvent(cancel);
        cancel.recycle();
        activePointer = MotionEvent.INVALID_POINTER_ID;
        multiTouch = dragged = longPressed = doubleTapped = false;
        zoom.reset();
        applyTransform();
        disallowParent(false);
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        zoom.setViewport(width, height);
        applyTransform();
    }

    @Override protected void onDetachedFromWindow() {
        resetZoom();
        super.onDetachedFromWindow();
    }
}
