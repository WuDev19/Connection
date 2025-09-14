package com.example.connection.repository;

import android.app.Application;
import com.example.connection.listener.UserDao;
import com.example.connection.model.MusicDatabase;
import com.example.connection.model.MyMusic;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChooseMusicRepository {

    private final UserDao userDao;
    private final ExecutorService queryRoomService = Executors.newSingleThreadExecutor();

    public interface OnGetInitListMusic{
        void getInitListMusic(List<MyMusic> listInitListMusic);
    }

    public interface OnSearchMusic{
        void getListMusic(List<MyMusic> myMusicList);
    }

    public ChooseMusicRepository(Application application){
        userDao = MusicDatabase.getInstance(application).userDao();
    }

    public void getInitListMusic(OnGetInitListMusic onGetInitListMusic) {
        FirebaseFirestore.getInstance().collection("music")
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<MyMusic> initMusic = queryDocumentSnapshots.toObjects(MyMusic.class);
                        onGetInitListMusic.getInitListMusic(initMusic);
                    }
                });
    }

    public void searchMusic(String name, OnSearchMusic onSearchMusic){
        queryRoomService.execute(() -> onSearchMusic.getListMusic(userDao.getListMusic(name)));
    }

    public void shutdownExecutor(){
        queryRoomService.shutdown();
    }

}
