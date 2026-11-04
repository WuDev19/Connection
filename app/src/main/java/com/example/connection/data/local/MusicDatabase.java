package com.example.connection.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {MyMusic.class}, version = 1, exportSchema = false)
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
