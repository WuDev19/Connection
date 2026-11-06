package com.example.connection.constants;

import android.content.Context;
import android.util.Log;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

public class MediaPlayerUtil {

    private ExoPlayer exoPlayer;
    private static MediaPlayerUtil instance;
    private static final String TAG = "MediaPlayerUtil";
    private boolean isInGeneral = false;
    private int position;
    private boolean isBackPress = false;
    private boolean isClickGeneral = false;
    private boolean isInChooseFrag = false;
    private boolean isFirstTimeScrollToShow = true;
    private String urlMusicCurrent;
    private boolean isCLickToPause;

    private MediaPlayerUtil() {
    }

    public static synchronized MediaPlayerUtil getInstance() {
        if (instance == null) {
            instance = new MediaPlayerUtil();
        }
        return instance;
    }

    private void ensurePlayer(Context context) {
        if (exoPlayer == null && context != null) {
            exoPlayer = new ExoPlayer.Builder(context.getApplicationContext()).build();
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        }
    }

    public boolean isInChooseFrag() {
        return isInChooseFrag;
    }

    public void setInChooseFrag(boolean inChooseFrag) {
        isInChooseFrag = inChooseFrag;
    }

    public void playMusic(Context context, String urlMusic) {
        if (urlMusic == null || urlMusic.isEmpty()) {
            urlMusicCurrent = null;
            if (exoPlayer != null) {
                exoPlayer.stop();
                exoPlayer.clearMediaItems();
            }
            return;
        }
        try {
            ensurePlayer(context);
            if ((position == 1) || isInChooseFrag) {
                urlMusicCurrent = urlMusic;
                MediaItem mediaItem = MediaItem.fromUri(urlMusic);
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.play();
                Log.e(TAG, "playMusic via Media3: " + urlMusic);
            }
        } catch (Exception e) {
            Log.e(TAG, "playMusic error: " + e.getMessage());
        }
    }

    public void pauseMusic() {
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
            Log.e(TAG, "pauseMusic: pause");
        }
    }

    public void resumeMusic() {
        if (exoPlayer != null && !exoPlayer.isPlaying() && !isInGeneral && urlMusicCurrent != null) {
            if (position == 1 || isInChooseFrag) {
                exoPlayer.play();
                Log.e(TAG, "resume music via Media3");
            }
        }
    }

    public void releaseMediaPlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    public void prepareMusicBeforePlaying(Context context, String urlMusic) {
        if (urlMusic == null || urlMusic.isEmpty()) {
            urlMusicCurrent = null;
            if (exoPlayer != null) {
                exoPlayer.stop();
                exoPlayer.clearMediaItems();
            }
            return;
        }
        try {
            ensurePlayer(context);
            urlMusicCurrent = urlMusic;
            MediaItem mediaItem = MediaItem.fromUri(urlMusic);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
        } catch (Exception e) {
            Log.e(TAG, "prepareMusic error: " + e.getMessage());
        }
    }

    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.isPlaying();
    }

    public void reset() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.clearMediaItems();
        }
    }

    public boolean isInGeneral() {
        return isInGeneral;
    }

    public void setIsInGeneral(boolean isInGeneral) {
        this.isInGeneral = isInGeneral;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setWhenBackPressFromGeneral(boolean isBackPress) {
        this.isBackPress = isBackPress;
    }

    public void resumeMusicWhenBackPress(Context context, String urlMusic) {
        if (position == 1 && isBackPress) {
            Log.e("FragmentShowPhoto", "resumeMusicWhenBackPress: " + urlMusic);
            if (urlMusic == null) {
                urlMusicCurrent = null;
                if (exoPlayer != null) {
                    exoPlayer.pause();
                }
                return;
            }
            try {
                ensurePlayer(context);
                urlMusicCurrent = urlMusic;
                MediaItem mediaItem = MediaItem.fromUri(urlMusic);
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.play();
            } catch (Exception e) {
                Log.e(TAG, "resumeMusicWhenBackPress error: " + e.getMessage());
            }
        }
    }

    public boolean isBackPress() {
        return isBackPress;
    }

    public boolean isClickGeneral() {
        return isClickGeneral;
    }

    public void setClickGeneral(boolean clickGeneral) {
        isClickGeneral = clickGeneral;
    }

    public boolean isFirstTimeScrollToShow() {
        return isFirstTimeScrollToShow;
    }

    public void setFirstTimeScrollToShow(boolean firstTimeScrollToShow) {
        isFirstTimeScrollToShow = firstTimeScrollToShow;
    }

    public boolean isCLickToPause() {
        return isCLickToPause;
    }

    public void setCLickToPause(boolean CLickToPause) {
        isCLickToPause = CLickToPause;
    }

    public int getPosition() {
        return position;
    }

}
