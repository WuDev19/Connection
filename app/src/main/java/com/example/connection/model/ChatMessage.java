package com.example.connection.model;

import java.util.Map;

public class ChatMessage {

    private String idMessage;
    private String message;
    private String senderId;
    private Long timestamp;
    private boolean seen;
    private Map<String, String> emote;
    private String messageType; // "TEXT", "IMAGE", "VIDEO"
    private String mediaUrl;

    public ChatMessage() {
    }

    public ChatMessage(String idMessage, String message, String senderId, Long timestamp, boolean seen, Map<String, String> emote) {
        this.idMessage = idMessage;
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.seen = seen;
        this.emote = emote;
        this.messageType = detectMessageType(message);
        this.mediaUrl = extractMediaUrl(message);
    }

    public ChatMessage(String idMessage, String message, String senderId, Long timestamp, boolean seen, Map<String, String> emote, String messageType, String mediaUrl) {
        this.idMessage = idMessage;
        this.message = message;
        this.senderId = senderId;
        this.timestamp = timestamp;
        this.seen = seen;
        this.emote = emote;
        this.messageType = messageType;
        this.mediaUrl = mediaUrl;
    }

    private static String detectMessageType(String msg) {
        if (msg == null) return "TEXT";
        if (msg.contains(".mp4")) return "VIDEO";
        if (msg.contains(".jpg") || msg.contains(".png") || msg.contains("https://")) return "IMAGE";
        return "TEXT";
    }

    private static String extractMediaUrl(String msg) {
        if (msg == null) return null;
        if (msg.contains("_")) {
            String[] parts = msg.split("_");
            if (parts.length > 1 && parts[1].startsWith("http")) return parts[1];
        }
        if (msg.startsWith("http")) return msg;
        return null;
    }

    public String getMessageType() {
        if (messageType == null || messageType.isEmpty()) {
            messageType = detectMessageType(message);
        }
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMediaUrl() {
        if (mediaUrl == null || mediaUrl.isEmpty()) {
            mediaUrl = extractMediaUrl(message);
        }
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
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
