package com.example.connection.ui.camera;

/** Input validation shared by pinch and slider zoom; bounds come from the current lens. */
public final class CameraZoomMath {
    public static float clampRatio(float ratio, float min, float max) {
        if (!Float.isFinite(min) || !Float.isFinite(max) || min <= 0 || max < min) return 1f;
        if (!Float.isFinite(ratio)) ratio = 1f;
        return Math.max(min, Math.min(max, ratio));
    }

    public static float scaleRatio(float current, float factor, float min, float max) {
        current = clampRatio(current, min, max);
        if (!Float.isFinite(min) || !Float.isFinite(max) || min <= 0 || max < min) return current;
        if (!Float.isFinite(factor) || factor <= 0) return clampRatio(current, min, max);
        double next = (double) current * factor;
        return (float) Math.max(min, Math.min(max, next));
    }

    public static float clampLinear(float value) {
        return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
    }

    private CameraZoomMath() {}
}
