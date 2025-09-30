package com.example.connection.model;

public class EmoteModel {
    private String idUser;
    private String urlAvt;
    private String username;
    private String emote;

    public EmoteModel() {
    }

    public EmoteModel(String idUser, String urlAvt, String username, String emote) {
        this.idUser = idUser;
        this.urlAvt = urlAvt;
        this.username = username;
        this.emote = emote;
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
