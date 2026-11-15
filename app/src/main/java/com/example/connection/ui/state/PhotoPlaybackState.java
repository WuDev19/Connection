package com.example.connection.ui.state;

import java.util.Objects;

/** Keeps user intent separate from temporary pauses caused by navigation or scrolling. */
public final class PhotoPlaybackState {
    private String postId;
    private String restoredPausedPostId;
    private boolean active;
    private boolean userPaused;

    public boolean select(String id) {
        if (Objects.equals(postId, id)) return false;
        postId = id;
        userPaused = id != null && id.equals(restoredPausedPostId);
        restoredPausedPostId = null;
        return true;
    }

    public void setActive(boolean active) { this.active = active; }

    public void toggle() {
        if (active && postId != null) userPaused = !userPaused;
    }

    public boolean shouldPlay() { return active && postId != null && !userPaused; }

    public String getPausedPostId() {
        return userPaused ? postId : restoredPausedPostId;
    }

    public void restorePausedPost(String id) {
        restoredPausedPostId = id;
        if (id != null && id.equals(postId)) userPaused = true;
    }
}
