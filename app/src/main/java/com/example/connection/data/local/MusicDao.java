package com.example.connection.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MusicDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(MyMusic myMusic);

    @Query("SELECT rowid, image, url, musicName, singer " +
            "FROM my_music " +
            "WHERE musicName " +
            "MATCH :name OR singer MATCH :name")
    List<MyMusic> getListMusic(String name);

}
