package com.example.connection.model;

import com.google.firebase.Timestamp;

public class Comment {
    private String idComment;
    private String idUser;
    private String avtComment;
    private String userNameComment;
    private String comment;
    private Timestamp timestamp;

    public Comment() {
    }

    public Comment(String idComment, String idUser, String avtComment, String userNameComment, String comment, Timestamp timestamp) {
        this.idComment = idComment;
        this.idUser = idUser;
        this.avtComment = avtComment;
        this.userNameComment = userNameComment;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getIdUser() {
        return idUser;
    }

    public void setIdUser(String idUser) {
        this.idUser = idUser;
    }

    public String getIdComment() {
        return idComment;
    }

    public void setIdComment(String idComment) {
        this.idComment = idComment;
    }

    public String getAvtComment() {
        return avtComment;
    }

    public void setAvtComment(String avtComment) {
        this.avtComment = avtComment;
    }

    public String getUserNameComment() {
        return userNameComment;
    }

    public void setUserNameComment(String userNameComment) {
        this.userNameComment = userNameComment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
