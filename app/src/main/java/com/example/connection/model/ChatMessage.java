package com.example.connection.model;

public class ChatMessage {

    private String message;
    private String senderId;
    private Long timestamp;
    private boolean seen;

    public ChatMessage() {
    }

    public ChatMessage(String message, String senderId, Long timestamp, boolean seen) {
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.seen = seen;
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
