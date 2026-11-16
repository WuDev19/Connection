package com.example.connection.ui.state;

/** Geometry in viewport coordinates; the transformed content uses a top-left pivot. */
public final class MediaZoomState {
    public static final float MIN_SCALE = 1f;
    public static final float MAX_SCALE = 4f;
    private float width;
    private float height;
    private float scale = MIN_SCALE;
    private float translationX;
    private float translationY;

    public void setViewport(float width, float height) {
        if (this.width != width || this.height != height) reset();
        this.width = Math.max(0, width);
        this.height = Math.max(0, height);
    }

    public void scaleBy(float factor, float focusX, float focusY) {
        if (width <= 0 || height <= 0 || !Float.isFinite(factor) || factor <= 0
                || !Float.isFinite(focusX) || !Float.isFinite(focusY)) return;
        float nextScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale * factor));
        if (nextScale <= 1.001f) { reset(); return; }
        float ratio = nextScale / scale;
        translationX = focusX - (focusX - translationX) * ratio;
        translationY = focusY - (focusY - translationY) * ratio;
        scale = nextScale;
        constrain();
    }

    public void panBy(float dx, float dy) {
        if (!Float.isFinite(dx) || !Float.isFinite(dy)) return;
        translationX += dx;
        translationY += dy;
        constrain();
    }

    private void constrain() {
        translationX = Math.max(width * (1 - scale), Math.min(0, translationX));
        translationY = Math.max(height * (1 - scale), Math.min(0, translationY));
    }

    public void reset() { scale = MIN_SCALE; translationX = translationY = 0; }
    public float getScale() { return scale; }
    public float getTranslationX() { return translationX; }
    public float getTranslationY() { return translationY; }
    public boolean isZoomed() { return scale > MIN_SCALE; }
}
