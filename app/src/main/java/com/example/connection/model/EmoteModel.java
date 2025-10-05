package com.example.connection.model;

import com.google.firebase.Timestamp;

public class EmoteModel {

    private String idEmote;
    private String idUser;
    private String urlAvt;
    private String username;
    private String emote;
    private Timestamp timestamp;

    public EmoteModel() {
    }

    public EmoteModel(String idEmote, String idUser, String urlAvt, String username, String emote, Timestamp timestamp) {
        this.idEmote = idEmote;
        this.idUser = idUser;
        this.urlAvt = urlAvt;
        this.username = username;
        this.emote = emote;
        this.timestamp = timestamp;
    }

    public String getIdEmote() {
        return idEmote;
    }

    public void setIdEmote(String idEmote) {
        this.idEmote = idEmote;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getIdUser() {
        return idUser;
    }

    public void setIdUser(String idUser) {
        this.idUser = idUser;
    }

    public String getUrlAvt() {
        return urlAvt;
    }

    public void setUrlAvt(String urlAvt) {
        this.urlAvt = urlAvt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmote() {
        return emote;
    }

    public void setEmote(String emote) {
        this.emote = emote;
    }

}
