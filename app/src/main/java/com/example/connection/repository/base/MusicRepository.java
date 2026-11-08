package com.example.connection.repository.base;

import com.example.connection.data.local.MyMusic;
import java.util.List;

public interface MusicRepository {

    interface OnGetInitListMusic {
        void getInitListMusic(List<MyMusic> listInitListMusic);
    }

    interface OnSearchMusic {
        void getListMusic(List<MyMusic> myMusicList);
    }

    void getInitListMusic(OnGetInitListMusic onGetInitListMusic);

    void searchMusic(String name, OnSearchMusic onSearchMusic);

    void shutdownExecutor();
}
