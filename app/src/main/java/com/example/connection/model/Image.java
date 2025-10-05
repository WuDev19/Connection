package com.example.connection.model;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.Map;

public class Image {

    private String postId;
    private List<String> friendId;
    private String imageUrl;
    private String videoUrl;
    private String status;
    private String posterId;
    private Timestamp timestamp;
    private List<String> seen;
    private String urlMusic;
    private String nameMusic;
    private String singerName;
    private String userNamePoster;
    private String avatarPoster;
    private int numberEmote, numberComment;
    private Map<String, String> emoteMap;

    public Image() {
    }

    public Image(String postId, List<String> friendId, String imageUrl, String videoUrl, String status, String posterId,
                 Timestamp timestamp, List<String> seen, String urlMusic, String nameMusic,
                 String singerName, String userNamePoster, String avatarPoster, int numberEmote, int numberComment,
                 Map<String, String> emoteMap) {
        this.postId = postId;
        this.friendId = friendId;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.status = status;
        this.posterId = posterId;
        this.timestamp = timestamp;
        this.seen = seen;
        this.urlMusic = urlMusic;
        this.nameMusic = nameMusic;
        this.singerName =singerName;
        this.userNamePoster = userNamePoster;
        this.avatarPoster = avatarPoster;
        this.numberEmote = numberEmote;
        this.numberComment = numberComment;
        this.emoteMap = emoteMap;
    }

    public Map<String, String> getEmoteMap() {
        return emoteMap;
    }

    public void setEmoteMap(Map<String, String> emoteMap) {
        this.emoteMap = emoteMap;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public int getNumberEmote() {
        return numberEmote;
    }

    public void setNumberEmote(int numberEmote) {
        this.numberEmote = numberEmote;
    }

    public int getNumberComment() {
        return numberComment;
    }

    public void setNumberComment(int numberComment) {
        this.numberComment = numberComment;
    }

    public String getNameMusic() {
        return nameMusic;
    }

    public void setNameMusic(String nameMusic) {
        this.nameMusic = nameMusic;
    }

    public String getSingerName() {
        return singerName;
    }

    public void setSingerName(String singerName) {
        this.singerName = singerName;
    }

    public String getUrlMusic() {
        return urlMusic;
    }

    public void setUrlMusic(String urlMusic) {
        this.urlMusic = urlMusic;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getPosterId() {
        return posterId;
    }

    public void setPosterId(String posterId) {
        this.posterId = posterId;
    }

    public List<String> getFriendId() {
        return friendId;
    }

    public void setFriendId(List<String> friendId) {
        this.friendId = friendId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getSeen() {
        return seen;
    }

    public void setSeen(List<String> seen) {
        this.seen = seen;
    }

    public String getUserNamePoster() {
        return userNamePoster;
    }

    public void setUserNamePoster(String userNamePoster) {
        this.userNamePoster = userNamePoster;
    }

    public String getAvatarPoster() {
        return avatarPoster;
    }

    public void setAvatarPoster(String avatarPoster) {
        this.avatarPoster = avatarPoster;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
