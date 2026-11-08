package com.example.connection.model.notification;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class NotificationRequest {

    @SerializedName("tokens")
    private List<String> tokens;

    @SerializedName("title")
    private String title;

    @SerializedName("body")
    private String body;

    @SerializedName("data")
    private Map<String, String> data;

    public NotificationRequest() {
    }

    public NotificationRequest(List<String> tokens, String title, String body, Map<String, String> data) {
        this.tokens = tokens;
        this.title = title;
        this.body = body;
        this.data = data;
    }

    public List<String> getTokens() {
        return tokens;
    }

    public void setTokens(List<String> tokens) {
        this.tokens = tokens;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
