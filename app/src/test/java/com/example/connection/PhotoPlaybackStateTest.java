package com.example.connection;

import com.example.connection.ui.state.PhotoPlaybackState;
import org.junit.Test;
import static org.junit.Assert.*;

public class PhotoPlaybackStateTest {
    @Test public void openingScreenWithoutSelectedPostDoesNotPlayOldMusic() {
        PhotoPlaybackState state = new PhotoPlaybackState();
        state.setActive(true);
        assertFalse(state.shouldPlay());
    }

    @Test public void navigatingAwayStopsAndReturningResumesSamePost() {
        PhotoPlaybackState state = new PhotoPlaybackState();
        state.select("video-a");
        state.setActive(true);
        assertTrue(state.shouldPlay());
        state.setActive(false);
        assertFalse(state.shouldPlay());
        state.setActive(true);
        assertTrue(state.shouldPlay());
    }

    @Test public void explicitPauseSurvivesNavigationAndRealtimeRebind() {
        PhotoPlaybackState state = new PhotoPlaybackState();
        state.select("photo-a");
        state.setActive(true);
        state.toggle();
        state.setActive(false);
        assertFalse(state.select("photo-a"));
        state.setActive(true);
        assertFalse(state.shouldPlay());
        assertEquals("photo-a", state.getPausedPostId());
        state.toggle();
        assertTrue(state.shouldPlay());
    }

    @Test public void selectingDifferentPostClearsPreviousPostsExplicitPause() {
        PhotoPlaybackState state = new PhotoPlaybackState();
        state.setActive(true);
        state.select("photo-a");
        state.toggle();
        assertTrue(state.select("video-b"));
        assertTrue(state.shouldPlay());
        assertNull(state.getPausedPostId());
    }

    @Test public void lateDataOnHiddenScreenDoesNotStartPlayback() {
        PhotoPlaybackState state = new PhotoPlaybackState();
        state.select("video-a");
        assertFalse(state.shouldPlay());
        state.toggle();
        state.setActive(true);
        assertTrue(state.shouldPlay());
    }

    @Test public void recreationRestoresPauseBeforeDataArrives() {
        PhotoPlaybackState state = new PhotoPlaybackState();
        state.restorePausedPost("photo-a");
        assertEquals("photo-a", state.getPausedPostId());
        state.setActive(true);
        state.select("photo-a");
        assertFalse(state.shouldPlay());
    }

    @Test public void deletedRestoredPostDoesNotPauseDifferentFallbackPost() {
        PhotoPlaybackState state = new PhotoPlaybackState();
        state.restorePausedPost("deleted");
        state.setActive(true);
        state.select("photo-b");
        assertTrue(state.shouldPlay());
        assertNull(state.getPausedPostId());
    }
}
