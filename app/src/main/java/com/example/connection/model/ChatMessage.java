package com.example.connection.model;

import java.util.Map;

public class ChatMessage {

    private String idMessage;
    private String message;
    private String senderId;
    private Long timestamp;
    private boolean seen;
    private Map<String, String> emote;

    public ChatMessage() {
    }

    public ChatMessage(String idMessage, String message, String senderId, Long timestamp, boolean seen, Map<String, String> emote) {
        this.idMessage = idMessage;
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.seen = seen;
        this.emote = emote;
    }

    public Map<String, String> getEmote() {
        return emote;
    }

    public void setEmote(Map<String, String> emote) {
        this.emote = emote;
    }

    public String getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(String idMessage) {
        this.idMessage = idMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}
