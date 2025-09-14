package com.example.connection.listener;

import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.connection.model.MyMusic;
import java.util.List;

@androidx.room.Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(MyMusic myMusic);

    @Query("SELECT rowid, image, url, musicName, singer " +
            "FROM my_music " +
            "WHERE musicName " +
            "MATCH :name OR singer MATCH :name")
    List<MyMusic> getListMusic(String name);
}
