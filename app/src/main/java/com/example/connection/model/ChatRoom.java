package com.example.connection.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoom implements Parcelable {

    private String chatRoomId;
    private String chatName;
    private String chatRoomAvt;
    private List<String> userId;
    private Timestamp lastMessageTimestamp;
    private String lastMessageSenderId;
    private String lastMessage;
    private Map<String, Long> deleteChat;
    private Map<String, String> avtSender;
    private Map<String, String> alias;
    private List<String> searchChat;
    private String background;
    private int isRandom;

    public ChatRoom() {
    }

    public ChatRoom(String chatRoomId,
                    String chatName,
                    String chatRoomAvt,
                    List<String> userId,
                    Timestamp lastMessageTimestamp,
                    String lastMessageSenderId,
                    String lastMessage,
                    Map<String, Long> deleteChat,
                    Map<String, String> avtSender,
                    Map<String, String> alias,
                    List<String> searchChat,
                    String background,
                    int isRandom) {
        this.chatRoomId = chatRoomId;
        this.chatName = chatName;
        this.chatRoomAvt = chatRoomAvt;
        this.userId = userId;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessageSenderId = lastMessageSenderId;
        this.lastMessage = lastMessage;
        this.deleteChat = deleteChat;
        this.avtSender = avtSender;
        this.alias = alias;
        this.searchChat = searchChat;
        this.background = background;
        this.isRandom = isRandom;
    }

    protected ChatRoom(Parcel in) {
        chatRoomId = in.readString();
        chatName = in.readString();
        chatRoomAvt = in.readString();
        userId = in.createStringArrayList();
        lastMessageTimestamp = in.readParcelable(Timestamp.class.getClassLoader());
        lastMessageSenderId = in.readString();
        lastMessage = in.readString();

        deleteChat = new HashMap<>();
        int deleteChatSize = in.readInt();
        for (int i = 0; i < deleteChatSize; i++) {
            String key = in.readString();
            long value = in.readLong();
            deleteChat.put(key, value);
        }

        avtSender = new HashMap<>();
        int avtSenderSize = in.readInt();
        for (int i = 0; i < avtSenderSize; i++) {
            String key = in.readString();
            String value = in.readString();
            avtSender.put(key, value);
        }

        alias = new HashMap<>();
        int aliasSize = in.readInt();
        for (int i = 0; i < aliasSize; i++) {
            String key = in.readString();
            String value = in.readString();
            alias.put(key, value);
        }

        searchChat = in.createStringArrayList();

        background = in.readString();

        isRandom = in.readInt();
    }

    public static final Creator<ChatRoom> CREATOR = new Creator<ChatRoom>() {
        @Override
        public ChatRoom createFromParcel(Parcel in) {
            return new ChatRoom(in);
        }

        @Override
        public ChatRoom[] newArray(int size) {
            return new ChatRoom[size];
        }
    };

    public int getIsRandom() {
        return isRandom;
    }

    public void setIsRandom(int isRandom) {
        this.isRandom = isRandom;
    }

    public List<String> getSearchChat() {
        return searchChat;
    }

    public void setSearchChat(List<String> searchChat) {
        this.searchChat = searchChat;
    }

    public Map<String, String> getAvtSender() {
        return avtSender;
    }

    public Map<String, String> getAlias() {
        return alias;
    }

    public void setAlias(Map<String, String> alias) {
        this.alias = alias;
    }

    public void setAvtSender(Map<String, String> avtSender) {
        this.avtSender = avtSender;
    }

    public String getChatRoomAvt() {
        return chatRoomAvt;
    }

    public void setChatRoomAvt(String chatRoomAvt) {
        this.chatRoomAvt = chatRoomAvt;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public Map<String, Long> getDeleteChat() {
        return deleteChat;
    }

    public void setDeleteChat(Map<String, Long> deleteChat) {
        this.deleteChat = deleteChat;
    }

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public List<String> getUserId() {
        return userId;
    }

    public void setUserId(List<String> userId) {
        this.userId = userId;
    }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastMessageSenderId() {
        return lastMessageSenderId;
    }

    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeString(chatRoomId);
        parcel.writeString(chatName);
        parcel.writeString(chatRoomAvt);
        parcel.writeStringList(userId);
        parcel.writeParcelable(lastMessageTimestamp, flags);
        parcel.writeString(lastMessageSenderId);
        parcel.writeString(lastMessage);

        if (deleteChat != null) {
            parcel.writeInt(deleteChat.size());
            for (Map.Entry<String, Long> entry : deleteChat.entrySet()) {
                parcel.writeString(entry.getKey());
                parcel.writeLong(entry.getValue());
            }
        } else {
            parcel.writeInt(0);
        }

        if (avtSender != null) {
            parcel.writeInt(avtSender.size());
            for (Map.Entry<String, String> entry : avtSender.entrySet()) {
                parcel.writeString(entry.getKey());
                parcel.writeString(entry.getValue());
            }
        } else {
            parcel.writeInt(0);
        }

        if (alias != null) {
            parcel.writeInt(alias.size());
            for (Map.Entry<String, String> entry : alias.entrySet()) {
                parcel.writeString(entry.getKey());
                parcel.writeString(entry.getValue());
            }
        } else {
            parcel.writeInt(0);
        }

        parcel.writeStringList(searchChat);

        parcel.writeString(background);

        parcel.writeInt(isRandom);
    }

}
