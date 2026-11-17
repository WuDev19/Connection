package com.example.connection.ui.camera.effects;

/** Time-based easing on the render thread, with no allocations after acquiring a face. */
final class FacePoseSmoother {
    private float[] points;
    private long lastFrame;

    float[] points() { return points; }

    boolean update(float[] target, long now) {
        if (target == null) {
            boolean changed = points != null;
            points = null;
            lastFrame = 0;
            return changed;
        }
        if (points == null || points.length != target.length) {
            points = target.clone();
            lastFrame = now;
            return true;
        }
        // Snap on reacquisition / a large jump, rather than dragging artwork across the screen.
        boolean snap = now - lastFrame > 200
                || Math.abs(points[66] - target[66]) > .15f
                || Math.abs(points[67] - target[67]) > .15f;
        float alpha = snap ? 1f : (float) (1 - Math.exp(-Math.max(0, now - lastFrame) / 30.0));
        lastFrame = now;
        boolean changed = false;
        for (int i = 0; i < points.length; i++) {
            float delta = target[i] - points[i];
            if (Math.abs(delta) > .0001f) {
                points[i] += delta * alpha;
                changed = true;
            }
        }
        return changed;
    }
}
