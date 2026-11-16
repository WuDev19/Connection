package com.example.connection;

import com.example.connection.ui.state.MediaZoomState;
import org.junit.Test;
import static org.junit.Assert.*;

public class MediaZoomStateTest {
    private MediaZoomState viewport() {
        MediaZoomState state = new MediaZoomState();
        state.setViewport(200, 100);
        return state;
    }

    @Test public void pinchKeepsThePointBetweenFingersStationary() {
        MediaZoomState state = viewport();
        state.scaleBy(2, 50, 25);
        assertEquals(2, state.getScale(), 0.001f);
        assertEquals(50, 50 * state.getScale() + state.getTranslationX(), 0.001f);
        assertEquals(25, 25 * state.getScale() + state.getTranslationY(), 0.001f);
    }

    @Test public void scaleIsBoundedAndReturningToOneResetsPan() {
        MediaZoomState state = viewport();
        state.scaleBy(100, 100, 50);
        assertEquals(4, state.getScale(), 0.001f);
        state.panBy(-10, -10);
        state.scaleBy(0.01f, 100, 50);
        assertFalse(state.isZoomed());
        assertEquals(0, state.getTranslationX(), 0.001f);
        assertEquals(0, state.getTranslationY(), 0.001f);
    }

    @Test public void draggingCannotExposeEmptyEdges() {
        MediaZoomState state = viewport();
        state.scaleBy(2, 100, 50);
        state.panBy(10000, 10000);
        assertEquals(0, state.getTranslationX(), 0.001f);
        assertEquals(0, state.getTranslationY(), 0.001f);
        state.panBy(-10000, -10000);
        assertEquals(-200, state.getTranslationX(), 0.001f);
        assertEquals(-100, state.getTranslationY(), 0.001f);
    }

    @Test public void panAtOriginalSizeDoesNotMoveTheMedia() {
        MediaZoomState state = viewport();
        state.panBy(100, -100);
        assertEquals(0, state.getTranslationX(), 0.001f);
        assertEquals(0, state.getTranslationY(), 0.001f);
    }

    @Test public void invalidDetectorValuesDoNotCorruptTransforms() {
        MediaZoomState state = viewport();
        state.scaleBy(Float.NaN, 0, 0);
        state.scaleBy(Float.POSITIVE_INFINITY, 0, 0);
        state.scaleBy(-1, 0, 0);
        state.scaleBy(2, Float.NaN, 0);
        state.panBy(Float.NaN, Float.POSITIVE_INFINITY);
        assertEquals(1, state.getScale(), 0.001f);
        assertEquals(0, state.getTranslationX(), 0.001f);
    }

    @Test public void changingViewportResetsZoomButSameSizeDoesNot() {
        MediaZoomState state = viewport();
        state.scaleBy(2, 100, 50);
        state.setViewport(200, 100);
        assertEquals(2, state.getScale(), 0.001f);
        state.setViewport(100, 200);
        assertEquals(1, state.getScale(), 0.001f);
    }

    @Test public void switchingPostsResetsZoomAndPan() {
        MediaZoomState state = viewport();
        state.scaleBy(3, 100, 50);
        state.panBy(-50, -20);
        state.reset();
        assertEquals(1, state.getScale(), 0.001f);
        assertEquals(0, state.getTranslationX(), 0.001f);
        assertEquals(0, state.getTranslationY(), 0.001f);
    }

    @Test public void scalingBeforeLayoutIsIgnored() {
        MediaZoomState state = new MediaZoomState();
        state.scaleBy(3, 100, 50);
        assertEquals(1, state.getScale(), 0.001f);
    }
}
