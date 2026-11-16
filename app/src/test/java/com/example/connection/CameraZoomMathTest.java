package com.example.connection;

import com.example.connection.ui.camera.CameraZoomMath;
import org.junit.Test;
import static org.junit.Assert.*;

public class CameraZoomMathTest {
    @Test public void pinchUsesTheCurrentLensLimitsRatherThanAFixedFourTimesLimit() {
        assertEquals(8f, CameraZoomMath.scaleRatio(4, 2, 1, 10), 0.001f);
        assertEquals(10f, CameraZoomMath.scaleRatio(8, 2, 1, 10), 0.001f);
        assertEquals(2f, CameraZoomMath.scaleRatio(1.5f, 2, 1, 2), 0.001f);
    }

    @Test public void supportedWideAngleZoomCanGoBelowOne() {
        assertEquals(0.5f, CameraZoomMath.scaleRatio(1, 0.1f, 0.5f, 10), 0.001f);
        assertEquals(1f, CameraZoomMath.clampRatio(1, 0.5f, 10), 0.001f);
    }

    @Test public void fixedZoomCameraRemainsAtItsOnlySupportedRatio() {
        assertEquals(1f, CameraZoomMath.scaleRatio(1, 4, 1, 1), 0.001f);
        assertEquals(1f, CameraZoomMath.scaleRatio(1, 0.2f, 1, 1), 0.001f);
    }

    @Test public void switchingToAnotherLensResetsToASupportedDefault() {
        assertEquals(1f, CameraZoomMath.clampRatio(1, 1, 4), 0.001f);
        assertEquals(2f, CameraZoomMath.clampRatio(1, 2, 10), 0.001f);
        assertEquals(0.8f, CameraZoomMath.clampRatio(1, 0.5f, 0.8f), 0.001f);
    }

    @Test public void invalidPinchSamplesDoNotCorruptTheZoomRatio() {
        assertEquals(2f, CameraZoomMath.scaleRatio(2, Float.NaN, 1, 10), 0.001f);
        assertEquals(2f, CameraZoomMath.scaleRatio(2, Float.POSITIVE_INFINITY, 1, 10), 0.001f);
        assertEquals(2f, CameraZoomMath.scaleRatio(2, -3, 1, 10), 0.001f);
        assertEquals(1f, CameraZoomMath.clampRatio(Float.NaN, 1, 10), 0.001f);
        assertEquals(1f, CameraZoomMath.clampRatio(2, 0, 10), 0.001f);
        assertEquals(10f, CameraZoomMath.scaleRatio(2, Float.MAX_VALUE, 1, 10), 0.001f);
    }

    @Test public void sliderInputIsClampedToTheCameraXLinearRange() {
        assertEquals(0, CameraZoomMath.clampLinear(-1), 0.001f);
        assertEquals(1, CameraZoomMath.clampLinear(2), 0.001f);
        assertEquals(0.4f, CameraZoomMath.clampLinear(0.4f), 0.001f);
        assertEquals(0, CameraZoomMath.clampLinear(Float.NaN), 0.001f);
    }
}
