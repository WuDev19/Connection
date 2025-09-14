package com.example.connection.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import com.google.firebase.Timestamp;
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
                    Map<String, String> avtSender) {
        this.chatRoomId = chatRoomId;
        this.chatName = chatName;
        this.chatRoomAvt = chatRoomAvt;
        this.userId = userId;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.lastMessageSenderId = lastMessageSenderId;
        this.lastMessage = lastMessage;
        this.deleteChat = deleteChat;
        this.avtSender = avtSender;
    }

    protected ChatRoom(Parcel in) {
        chatRoomId = in.readString();
        chatName = in.readString();
        chatRoomAvt = in.readString();
        userId = in.createStringArrayList();
        lastMessageTimestamp = in.readParcelable(Timestamp.class.getClassLoader());
        lastMessageSenderId = in.readString();
        lastMessage = in.readString();
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

    public Map<String, String> getAvtSender() {
        return avtSender;
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

    @NonNull
    @Override
    public String toString() {
        return "chatRoomId = " + chatRoomId + "\n";
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(chatRoomId);
        parcel.writeString(chatName);
        parcel.writeString(chatRoomAvt);
        parcel.writeStringList(userId);
        parcel.writeParcelable(lastMessageTimestamp, i);
        parcel.writeString(lastMessageSenderId);
        parcel.writeString(lastMessage);
    }
}
