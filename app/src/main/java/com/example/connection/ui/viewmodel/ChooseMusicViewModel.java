package com.example.connection.ui.viewmodel;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.connection.constants.MediaPlayerUtil;
import com.example.connection.data.local.MyMusic;
import com.example.connection.repository.ChooseMusicRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChooseMusicViewModel extends AndroidViewModel {

    private Object[] list;
    private final Set<Integer> positionCheck = new HashSet<>();
    private final MutableLiveData<Boolean> playComplete = new MutableLiveData<>();
    private final MutableLiveData<Boolean> changeIconPlayMusic = new MutableLiveData<>();
    private final MutableLiveData<Object[]> changeIconOldMusic = new MutableLiveData<>();
    private final MutableLiveData<Boolean> setPlaying = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hideTvCancel = new MutableLiveData<>();
    private final MutableLiveData<List<MyMusic>> listInitListMusic = new MutableLiveData<>();
    private final MutableLiveData<List<MyMusic>> myMusicListLiveData = new MutableLiveData<>();
    private final ChooseMusicRepository chooseMusicRepository;
    private ExoPlayer exoPlayer;

    public ChooseMusicViewModel(@NonNull Application application) {
        super(application);
        chooseMusicRepository = new ChooseMusicRepository(application);
    }

    private void ensurePlayer() {
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(getApplication()).build();
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_ENDED) {
                        playComplete.postValue(true);
                    }
                }
            });
        }
    }

    public void handleMusic(String url, int position, boolean isPlaying, Context context) {
        ensurePlayer();
        if (!isPlaying) {
            if (!positionCheck.contains(position)) {
                playMusic(context, url);
                positionCheck.clear();
                positionCheck.add(position);
            } else {
                resumeMusic();
            }
            setPlaying.postValue(true);
            changeIconPlayMusic.postValue(true);
        } else {
            if (positionCheck.contains(position)) {
                pauseMusic();
                setPlaying.postValue(false);
                changeIconPlayMusic.postValue(false);
            } else {
                playMusic(context, url);
                list = positionCheck.toArray();
                changeIconOldMusic.postValue(list);
                positionCheck.clear();
                positionCheck.add(position);
                setPlaying.postValue(true);
                changeIconPlayMusic.postValue(true);
            }
        }
    }

    public void handleMusicAfterTextChange(String title) {
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
        if (!positionCheck.isEmpty()) {
            list = positionCheck.toArray();
            positionCheck.clear();
            setPlaying.setValue(false);
        }
        if (title.isEmpty()) {
            hideTvCancel.setValue(true);
        }
    }

    public void playMusic(Context context, String url) {
        if (url == null) return;
        ensurePlayer();
        try {
            MediaItem mediaItem = MediaItem.fromUri(url);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();
        } catch (Exception ignored) {
        }
    }

    public void resumeMusic() {
        if (exoPlayer != null && !exoPlayer.isPlaying()) {
            exoPlayer.play();
        }
    }

    public void pauseMusic() {
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
        }
    }

    public void searchMusic(String name) {
        chooseMusicRepository.searchMusic(name, myMusicListLiveData::postValue);
    }

    public void getInitListMusic() {
        chooseMusicRepository.getInitListMusic(listInitListMusic::setValue);
    }

    public void setMediaOnDestroy() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        MediaPlayerUtil.getInstance().setInChooseFrag(false); //đánh dấu đã vào fragment choose music
    }

    public void setMediaPlayerInChooseFrag() {
        MediaPlayerUtil.getInstance().setInChooseFrag(true); //đánh dấu đã vào fragment choose music
    }

    public LiveData<Boolean> getPlayComplete() {
        return playComplete;
    }

    public LiveData<Boolean> getChangeIconPlayMusic() {
        return changeIconPlayMusic;
    }

    public LiveData<Object[]> getChangeIconOldMusic() {
        return changeIconOldMusic;
    }

    public LiveData<Boolean> getSetPlaying() {
        return setPlaying;
    }

    public LiveData<Boolean> getHideTvCancel() {
        return hideTvCancel;
    }

    public LiveData<List<MyMusic>> getListInitListMusic() {
        return listInitListMusic;
    }

    public LiveData<List<MyMusic>> getMyMusicListLiveData() {
        return myMusicListLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        chooseMusicRepository.shutdownExecutor();
    }

}
