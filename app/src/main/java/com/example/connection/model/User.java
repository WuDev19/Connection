package com.example.connection.model;

import androidx.annotation.NonNull;

import java.util.List;

public class User {

    private String id;
    private String avatarUser;
    private String userName;
    private String userNameLowerCase;
    private String email;
    private String sdt;
    private List<String> friend;
    private List<String> friendConfirm;
    private boolean active;
    private String fcmToken;

    public User() {
    }

    public User(String id, String avatarUser, String userName, String userNameLowerCase, String email, String sdt, List<String> friend, List<String> friendConfirm, boolean active) {
        this.id = id;
        this.avatarUser = avatarUser;
        this.userName = userName;
        this.userNameLowerCase = userNameLowerCase;
        this.email = email;
        this.sdt = sdt;
        this.friend = friend;
        this.friendConfirm = friendConfirm;
        this.active = active;
    }

    public String getUserNameLowerCase() {
        return userNameLowerCase;
    }

    public void setUserNameLowerCase(String userNameLowerCase) {
        this.userNameLowerCase = userNameLowerCase;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAvatarUser() {
        return avatarUser;
    }

    public void setAvatarUser(String avatarUser) {
        this.avatarUser = avatarUser;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSdt() {
        return sdt;
    }

    public void setSdt(String sdt) {
        this.sdt = sdt;
    }

    public List<String> getFriend() {
        return friend;
    }

    public void setFriend(List<String> friend) {
        this.friend = friend;
    }

    public List<String> getFriendConfirm() {
        return friendConfirm;
    }

    public void setFriendConfirm(List<String> friendConfirm) {
        this.friendConfirm = friendConfirm;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", avatarUser='" + avatarUser + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", sdt='" + sdt + '\'' +
                ", friend=" + friend +
                ", friendConfirm=" + friendConfirm +
                ", active=" + active +
                '}';
    }
}
