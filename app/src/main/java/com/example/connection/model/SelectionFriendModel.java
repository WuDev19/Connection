package com.example.connection.model;

public class SelectionFriendModel {
    private String id;
    private String avt;
    private String username;
    private boolean isSelected;

    public SelectionFriendModel() {
    }

    public SelectionFriendModel(String id, String avt, String username, boolean isSelected) {
        this.id = id;
        this.avt = avt;
        this.username = username;
        this.isSelected = isSelected;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAvt() {
        return avt;
    }

    public void setAvt(String avt) {
        this.avt = avt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

}
