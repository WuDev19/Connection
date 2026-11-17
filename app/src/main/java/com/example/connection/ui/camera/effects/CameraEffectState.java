package com.example.connection.ui.camera.effects;

/** Immutable snapshot: changing one group never resets the other. */
public final class CameraEffectState {
    public enum Color { ORIGINAL, WARM, COOL, VINTAGE, MONO }
    public enum Face { NONE, CAT, BUNNY, CROWN, HORNS, SUNGLASSES, HEART_GLASSES,
        WHISKERS, FRECKLES, BLUSH, CHEEK_HEARTS }
    public final Color color;
    public final Face face;
    public final float intensity;

    public CameraEffectState() { this(Color.ORIGINAL, Face.NONE, 0.7f); }
    public CameraEffectState(Color color, Face face, float intensity) {
        this.color = color;
        this.face = face;
        this.intensity = Float.isNaN(intensity) ? 0.7f : Math.max(0, Math.min(1, intensity));
    }
    public CameraEffectState withColor(Color value) { return new CameraEffectState(value, face, intensity); }
    public CameraEffectState withFace(Face value) { return new CameraEffectState(color, value, intensity); }
    public CameraEffectState withIntensity(float value) { return new CameraEffectState(color, face, value); }

    /** Row-major 4x5 matrix; offsets use normalized 0..1 channels (also used by GLSL). */
    public float[] colorMatrix() {
        float[] identity = {1,0,0,0,0, 0,1,0,0,0, 0,0,1,0,0, 0,0,0,1,0};
        float[] target;
        switch (color) {
            case WARM: target = new float[]{1.08f,0,0,0,.025f, 0,1.02f,0,0,.01f, 0,0,.9f,0,0, 0,0,0,1,0}; break;
            case COOL: target = new float[]{.92f,0,0,0,0, 0,1.01f,0,0,.01f, 0,0,1.1f,0,.025f, 0,0,0,1,0}; break;
            case VINTAGE: target = new float[]{.393f,.769f,.189f,0,.025f, .349f,.686f,.168f,0,.015f, .272f,.534f,.131f,0,.015f, 0,0,0,1,0}; break;
            case MONO: target = new float[]{.213f,.715f,.072f,0,0, .213f,.715f,.072f,0,0, .213f,.715f,.072f,0,0, 0,0,0,1,0}; break;
            default: return identity;
        }
        for (int i=0; i<20; i++) target[i] = identity[i] + intensity * (target[i]-identity[i]);
        return target;
    }
}
