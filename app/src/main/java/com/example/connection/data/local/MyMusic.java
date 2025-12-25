package com.example.connection.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.PrimaryKey;

@Fts4
@Entity(tableName = "my_music")
public class MyMusic {
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    private int id;
    private String image;
    private String url;
    private String musicName;
    private String singer;

    public MyMusic() {
    }

    public MyMusic(int id, String image, String url, String musicName, String singer) {
        this.id = id;
        this.image = image;
        this.url = url;
        this.musicName = musicName;
        this.singer = singer;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }
}
