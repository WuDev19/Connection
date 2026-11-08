package com.example.connection.repository.base;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.LiveData;

import com.example.connection.model.ChatMessage;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.ui.state.OnStateListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Map;

public interface ChatRepository {

    interface OnRecentMessagesListener {
        void onInitialLoaded(List<ChatMessage> initialMessages);
        void onMessageAdded(ChatMessage message);
        void onMessageModified(ChatMessage message);
        void onMessageRemoved(String idMessage);
        void onError(Exception e);
    }

    interface OnOlderMessagesLoadedListener {
        void onLoaded(List<ChatMessage> olderMessages);
        void onError(Exception e);
    }

    interface HandleChat {
        void onCreateChatRoom(ChatRoom chatroom);
    }

    interface OnUpdateChatRoom {
        void onUpdate(ChatRoom chatRoom);
    }

    interface OnShowInfo {
        void onShow(User otherUser);
    }

    interface OnSearchChatRoom {
        void onSearch(List<ChatRoom> roomList);
    }

    interface OnSeenChatRoom {
        void onSeen(boolean seen);
    }

    String getCurrentUserId();

    User getCurrentUser();

    Task<DocumentSnapshot> getUser();

    Task<DocumentSnapshot> getUserFriend(String id);

    Task<DocumentSnapshot> getOtherUser(String otherUserId);

    LiveData<List<User>> searchUser(String username);

    void setSeenMessage(String chatRoomId);

    void unregisterSeenMessageListening();

    void getOrCreateChatRoom(String chatRoomId, HandleChat handleChat);

    void sendMessage(String message, String chatRoomId, ChatRoom chatRoom);

    Task<Void> expressEmote(ChatRoom chatRoom, String idMessage, String idUser, String emote);

    Task<Void> deleteEmote(ChatRoom chatRoom, String idMessage, String idUser);

    Query getOptions(ChatRoom chatRoom1, String chatRoomId);

    void listeningChatRoom(ChatRoom chatRoom, OnUpdateChatRoom updateChatRoom);

    void unregisterListeningChatRoom();

    void deleteOneSide(ChatRoom model, List<String> userIds);

    void deleteTwoSides(ChatRoom model);

    LiveData<Map<String, String>> listeningReaction(ChatRoom chatRoom, String idMess);

    void unregisterListeningReaction();

    ListenerRegistration listenRecentMessages(String chatRoomId, Long minTimestamp, int limit, OnRecentMessagesListener listener);

    void unregisterRecentMessages();

    void loadOlderMessages(String chatRoomId, Long minTimestamp, Long oldestTimestamp, int limit, OnOlderMessagesLoadedListener callback);

    LiveData<Query> getQuery();

    void showInfo(String otherUserId, OnShowInfo onShowInfo);

    void getIsNotSeen(ChatRoom model, OnSeenChatRoom onSeenChatRoom);

    void searchChatRoom(OnSearchChatRoom onSearchChatRoom);

    Task<QuerySnapshot> getActiveUsers(int limit);

    Task<QuerySnapshot> getRandomUsers(String randomKey, int limit);

    Task<QuerySnapshot> getNumberUser(int number);

    String createChatRoomId();

    Task<Void> createChatRoom(ChatRoom chatRoom);

    void scheduleDeleteRandomChat(Context context, String chatRoomId);

    void setChatRoomToFireStore(ChatRoom chatRoom, OnStateListener onStateListener);

    Task<Void> updateAlias(ChatRoom chatRoom, String idUser, String newAlias);

    Task<Void> updateChatGroupName(String chatroomId, String name);

    void upload_avt(Uri uri, ChatRoom chatRoom, OnStateListener onStateListener);

    Task<Void> addMemberToGroup(ChatRoom chatRoom);

    Task<Void> leaveChat(ChatRoom chatRoom);

    Task<QuerySnapshot> getListImageBackGround();

    Task<Void> updateBackGround(String chatId, String background);
}
