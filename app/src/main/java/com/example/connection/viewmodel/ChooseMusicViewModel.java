package com.example.connection.viewmodel;

import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.connection.model.MyMusic;
import com.example.connection.repository.ChooseMusicRepository;
import com.example.connection.utils.MediaPlayerUtil;

import java.io.IOException;
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
    private MediaPlayer mediaPlayer = new MediaPlayer();

    public ChooseMusicViewModel(@NonNull Application application) {
        super(application);
        chooseMusicRepository = new ChooseMusicRepository(application);
    }

    public void handleMusic(String url, int position, boolean isPlaying, Context context) {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mediaPlayer1 -> playComplete.postValue(true));
        }
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
        if(mediaPlayer != null){
            mediaPlayer.stop();
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

    public void playMusic(Context context, String url){
        if(mediaPlayer == null || url == null) return;
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(context, Uri.parse(url));
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void resumeMusic(){
        if(mediaPlayer != null && !mediaPlayer.isPlaying()){
            mediaPlayer.start();
        }
    }

    public void pauseMusic(){
        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }

    public void searchMusic(String name) {
        chooseMusicRepository.searchMusic(name, myMusicListLiveData::postValue);
    }

    public void getInitListMusic() {
        chooseMusicRepository.getInitListMusic(listInitListMusic::setValue);
    }

    public void setMediaOnDestroy() {
        if(mediaPlayer != null){
            mediaPlayer.release();
            mediaPlayer = null;
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
        chooseMusicRepository.shutdownExecutor();
    }

}
