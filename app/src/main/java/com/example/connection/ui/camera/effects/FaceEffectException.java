package com.example.connection.ui.camera.effects;

/** Preserves the original cause without exposing native diagnostic details in the camera UI. */
public final class FaceEffectException extends RuntimeException {
    public enum Stage { MODEL_LOAD, DETECTOR_INIT, FRAME_PREPARATION, INFERENCE }

    public final Stage stage;

    FaceEffectException(Stage stage, String message, Throwable cause) {
        super(message, cause);
        this.stage = stage;
    }
}
