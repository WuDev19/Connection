package com.example.connection.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import java.io.IOException;

public class MediaPlayerUtil {

    private MediaPlayer mediaPlayer;
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
        mediaPlayer = new MediaPlayer();
    }

    public static synchronized MediaPlayerUtil getInstance() {
        if (instance == null) {
            instance = new MediaPlayerUtil();
        }
        return instance;
    }

    public boolean isInChooseFrag() {
        return isInChooseFrag;
    }

    public void setInChooseFrag(boolean inChooseFrag) {
        isInChooseFrag = inChooseFrag;
    }

    public void playMusic(Context context, String urlMusic) {
        if (urlMusic == null) {
            urlMusicCurrent = null;
            if(mediaPlayer != null){
                mediaPlayer.reset();
            }
            return;
        }
        try {
            if ((mediaPlayer != null && position == 1) || (mediaPlayer != null && isInChooseFrag)) {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(context, Uri.parse(urlMusic));
                mediaPlayer.setOnPreparedListener(mediaPlayer1 -> {
                    mediaPlayer1.start();
                    urlMusicCurrent = urlMusic;
                });
                mediaPlayer.prepareAsync();
            }
        } catch (Exception e) {
            Log.e(TAG, "playMusic: " + e.getMessage());
        }
    }

    public void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying() && urlMusicCurrent != null) {
            mediaPlayer.pause();
        }
    }

    public void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && !isInGeneral && urlMusicCurrent != null) {
            if (position == 1 || isInChooseFrag) {
                mediaPlayer.start();
            }
        }
    }

    public void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
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
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }
                return;
            }
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(context, Uri.parse(urlMusic));
                    mediaPlayer.setOnPreparedListener(mediaPlayer1 -> {
                        mediaPlayer1.start();
                        urlMusicCurrent = urlMusic;
                    });
                    mediaPlayer.prepareAsync();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
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
