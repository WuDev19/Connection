package com.example.connection.ui.state;

import android.content.Context;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import com.example.connection.constants.MediaPlayerUtil;
import com.example.connection.constants.VideoCache;
import com.example.connection.model.Image;
import java.util.Objects;

/** Playback is selected by the snapped post, never by RecyclerView binding order. */
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class PhotoPlaybackController {
    private final Context context;
    private final ExoPlayer video;
    private final VideoCache.Preloader preloader;
    private final MediaPlayerUtil music = MediaPlayerUtil.getInstance();
    private PlayerView playerView;
    private final PhotoPlaybackState state = new PhotoPlaybackState();
    private String videoUrl;
    private String musicUrl;
    private String nextVideoUrl;

    public PhotoPlaybackController(Context context) {
        this.context = context.getApplicationContext();
        video = new ExoPlayer.Builder(this.context)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(VideoCache.dataSourceFactory(this.context))).build();
        video.setRepeatMode(Player.REPEAT_MODE_ONE);
        preloader = new VideoCache.Preloader(this.context);
        video.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) preloader.cancel();
                if (state.shouldPlay() && playbackState == Player.STATE_READY) preloader.preload(nextVideoUrl);
            }
        });
    }

    public void select(Image image, PlayerView view, String nextUrl) {
        boolean changed = state.select(image.getPostId());
        if (changed) {
            pause();
        }
        String newVideo = clean(image.getVideoUrl());
        String newMusic = clean(image.getUrlMusic());
        if (!Objects.equals(videoUrl, newVideo)) {
            preloader.cancel();
            video.stop();
            video.clearMediaItems();
            videoUrl = newVideo;
            if (newVideo != null) {
                video.setMediaItem(MediaItem.fromUri(newVideo));
                video.prepare();
            }
        }
        musicUrl = newMusic;
        video.setVolume(newMusic == null ? 1f : 0f);
        if (playerView != view) {
            if (playerView != null) playerView.setPlayer(null);
            playerView = view;
        }
        if (playerView != null && newVideo != null) playerView.setPlayer(video);
        nextVideoUrl = nextUrl;
        resume();
    }

    public void setActive(boolean active) {
        state.setActive(active);
        if (active) resume();
        else pause();
    }

    public void toggle() {
        state.toggle();
        music.setCLickToPause(state.getPausedPostId() != null);
        if (state.shouldPlay()) resume();
        else pause();
    }

    public void pause() {
        video.pause();
        music.pauseMusicForOwner(this);
        preloader.cancel();
    }

    private void resume() {
        if (!state.shouldPlay()) return;
        music.setIsInGeneral(false);
        music.setPosition(1);
        music.setCLickToPause(false);
        music.playMusicForOwner(this, context, musicUrl);
        if (videoUrl != null) video.play();
        if (videoUrl == null || video.getPlaybackState() == Player.STATE_READY) preloader.preload(nextVideoUrl);
    }

    public String getPausedPostId() { return state.getPausedPostId(); }

    public void restorePausedPost(String id) { state.restorePausedPost(id); }

    public void release() {
        setActive(false);
        if (playerView != null) playerView.setPlayer(null);
        playerView = null;
        preloader.release();
        music.releaseOwner(this);
        video.release();
    }

    private static String clean(String value) { return value == null || value.isEmpty() ? null : value; }
}
