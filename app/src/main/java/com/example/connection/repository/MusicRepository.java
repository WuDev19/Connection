package com.example.connection.repository;

import android.app.Application;
import com.example.connection.data.local.MusicDao;
import com.example.connection.data.local.MusicDatabase;
import com.example.connection.data.local.MyMusic;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicRepository {

    private final MusicDao musicDao;
    private final ExecutorService queryRoomService = Executors.newSingleThreadExecutor();

    public interface OnGetInitListMusic {
        void getInitListMusic(List<MyMusic> listInitListMusic);
    }

    public interface OnSearchMusic {
        void getListMusic(List<MyMusic> myMusicList);
    }

    public MusicRepository(Application application) {
        musicDao = MusicDatabase.getInstance(application).musicDao();
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

    public void searchMusic(String name, OnSearchMusic onSearchMusic) {
        queryRoomService.execute(() -> onSearchMusic.getListMusic(musicDao.getListMusic(name)));
    }

    public void shutdownExecutor() {
        queryRoomService.shutdown();
    }

}
