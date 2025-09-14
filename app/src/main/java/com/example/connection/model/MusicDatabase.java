package com.example.connection.model;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.connection.listener.UserDao;

@Database(entities = {MyMusic.class}, version = 1)
public abstract class MusicDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "music.db";
    private static MusicDatabase musicDatabase;

    public static synchronized MusicDatabase getInstance(Context context){
        if(musicDatabase == null){
            musicDatabase = Room.databaseBuilder(context.getApplicationContext(), MusicDatabase.class, DATABASE_NAME)
                    .build();
        }
        return musicDatabase;
    }

    public abstract UserDao userDao();

}
