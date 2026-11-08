package com.example.connection.ui.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.repository.base.ChatRepository;
import com.example.connection.repository.impl.ChatRepositoryImpl;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepository;
    private String chatRoomId;
    private User otherUser;
    private ChatRoom chatRoom;
    private final String currentUserId;
    private final String TAG = getClass().getName();
    private final MutableLiveData<User> otherUserLiveData = new MutableLiveData<>();
    private final MutableLiveData<ChatRoom> chatRoomLiveData = new MutableLiveData<>();
    private final MutableLiveData<ChatRoom> createNewChat = new MutableLiveData<>();
    private final MutableLiveData<Boolean> send = new MutableLiveData<>();
    private final MutableLiveData<Query> createOptions = new MutableLiveData<>();
    private final MutableLiveData<ChatRoom> chatRoomListeningLiveData = new MutableLiveData<>();

    public ChatViewModel() {
        chatRepository = new ChatRepositoryImpl();
        currentUserId = chatRepository.getCurrentUserId();
    }

    public void setSeenMessage() {
        chatRepository.setSeenMessage(chatRoomId);
    }

    public void unregisterSeenMessageListening() {
        chatRepository.unregisterSeenMessageListening();
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public void createOtherUser(String id, String avatar, String username, String email, String sdt) {
        if (sdt == null) {
            otherUser = new User(id, avatar, username, username.toLowerCase(), email, null, null, null, true);
        } else if (email == null) {
            otherUser = new User(id, avatar, username, username.toLowerCase(), null, sdt, null, null, true);
        }
        if (currentUserId != null) {
            chatRoomId = getChatroomId(currentUserId, otherUser.getId());
        }
        otherUserLiveData.setValue(otherUser);
    }

    public void getOrCreateChatRoom() {
        chatRepository.getOrCreateChatRoom(chatRoomId, chatroom -> {
            chatRoom = chatroom;
            chatRoomLiveData.setValue(chatRoom);
        });
    }

    public void sendMessage(String message) {
        if (currentUserId == null || chatRoomId == null || chatRoom == null) return;
        List<String> userIds = chatRoom.getUserId();
        Map<String, Long> map = chatRoom.getDeleteChat();
        List<String> updatedUserIds = new ArrayList<>();
        boolean check = false;
        if (userIds != null) {
            for (String id : userIds) {
                if (id.contains("delete")) {
                    check = true;
                    String cleanedId = id.split("_")[0];
                    updatedUserIds.add(cleanedId);
                    if (map != null) {
                        map.put(cleanedId, Timestamp.now().toDate().getTime());
                    }
                } else {
                    updatedUserIds.add(id);
                }
            }
        }
        if (check) {
            chatRoom.setUserId(updatedUserIds);
            if (map != null) {
                chatRoom.setDeleteChat(map);
            }
        }
        userIds = updatedUserIds;
        send.setValue(true);
        chatRoom.setLastMessageSenderId(currentUserId);
        chatRoom.setLastMessageTimestamp(Timestamp.now());
        if (userIds.size() >= 3) {
            String alias = chatRoom.getAlias().get(chatRepository.getCurrentUser().getId());
            if (alias != null) {
                if (alias.contains("_")) {
                    alias = alias.split("_")[1];
                }
            }
            chatRoom.setLastMessage(alias + ": " + message);
        } else {
            chatRoom.setLastMessage(message);
        }
        chatRepository.sendMessage(message, chatRoomId, chatRoom);
    }

    public void createNewChatRoom() {
        Map<String, Long> mapDelete = new HashMap<>();
        mapDelete.put(currentUserId, Timestamp.now().toDate().getTime());
        mapDelete.put(otherUser.getId(), Timestamp.now().toDate().getTime());
        Map<String, String> avtMap = new HashMap<>();
        avtMap.put(otherUser.getId(), otherUser.getAvatarUser());
        avtMap.put(chatRepository.getCurrentUser().getId(), chatRepository.getCurrentUser().getAvatarUser());
        Map<String, String> aliasMap = new HashMap<>();
        aliasMap.put(otherUser.getId(), otherUser.getUserName());
        aliasMap.put(chatRepository.getCurrentUser().getId(), chatRepository.getCurrentUser().getUserName());
        List<String> searchChat = new ArrayList<>();
        searchChat.add(otherUser.getUserName().toLowerCase());
        searchChat.add(chatRepository.getCurrentUser().getUserName().toLowerCase());
        chatRoom = new ChatRoom(chatRoomId,
                null,
                null,
                Arrays.asList(currentUserId, otherUser.getId()),
                null,
                null,
                null,
                mapDelete,
                avtMap,
                aliasMap,
                searchChat,
                "",
                0);
        createNewChat.setValue(chatRoom);
    }

    public LiveData<User> getOtherUser(String otherUserId) {
        MutableLiveData<User> otherUserExistLiveData = new MutableLiveData<>();
        chatRepository.getOtherUser(otherUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User other = task.getResult().toObject(User.class);
                if (other != null) otherUser = other;
                otherUserExistLiveData.setValue(other);
            }
        });
        return otherUserExistLiveData;
    }

    public void createOptions(ChatRoom chatRoom1) {
        Query query = chatRepository.getOptions(chatRoom1, chatRoomId);
        createOptions.setValue(query);
    }

    private String getChatroomId(String userId1, String userId2) {
        if (userId1.hashCode() < userId2.hashCode()) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    public void listeningChatRoom() {
        chatRepository.listeningChatRoom(chatRoom, value -> {
            if (value != null) chatRoom = value;
            chatRoomListeningLiveData.setValue(value);
        });
    }

    public void unregisterListeningChatRoom() {
        chatRepository.unregisterListeningChatRoom();
    }

    public void expressEmote(String idMessage, String idUser, String emote) {
        chatRepository.expressEmote(chatRoom, idMessage, idUser, emote).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "onComplete: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }

    public void removeEmote(String idMessage, String idUser) {
        if (idUser != null && idUser.equals(currentUserId)) {
            chatRepository.deleteEmote(chatRoom, idMessage, idUser).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "onComplete: " + Objects.requireNonNull(task.getException()).getMessage());
                }
            });
        }
    }

    public LiveData<Map<String, String>> registerListeningReaction(String idMess) {
        return chatRepository.listeningReaction(chatRoom, idMess);
    }

    public void unregisterListeningReaction() {
        chatRepository.unregisterListeningReaction();
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public void getDelete() {
        List<String> userIds = chatRoom.getUserId();
        if (userIds == null) return;
        int count = 0;
        for (String id : userIds) {
            if (id.contains("_delete")) {
                count++;
            }
        }
        if ((count == (userIds.size() - 1)) || userIds.get(0).equals(userIds.get(1))) {
            chatRepository.deleteTwoSides(chatRoom);
        } else {
            chatRepository.deleteOneSide(chatRoom, userIds);
        }
    }

    public LiveData<Boolean> getSend() {
        return send;
    }

    public LiveData<User> getOtherUserLiveData() {
        return otherUserLiveData;
    }

    public LiveData<ChatRoom> getChatRoomLiveData() {
        return chatRoomLiveData;
    }

    public LiveData<ChatRoom> getCreateNewChat() {
        return createNewChat;
    }

    public LiveData<Query> getCreateOptions() {
        return createOptions;
    }

    public LiveData<ChatRoom> getChatRoomListeningLiveData() {
        return chatRoomListeningLiveData;
    }

    public void listenRecentMessages(ChatRoom room, ChatRepository.OnRecentMessagesListener listener) {
        if (room == null || room.getChatRoomId() == null) return;
        Long minTimestamp = 0L;
        if (room.getDeleteChat() != null && currentUserId != null) {
            Long deletedTime = room.getDeleteChat().get(currentUserId);
            if (deletedTime != null) {
                minTimestamp = deletedTime;
            }
        }
        chatRepository.listenRecentMessages(room.getChatRoomId(), minTimestamp, 30, listener);
    }

    public void unregisterRecentMessages() {
        chatRepository.unregisterRecentMessages();
    }

    public void loadOlderMessages(Long oldestTimestamp, int limit, ChatRepository.OnOlderMessagesLoadedListener callback) {
        if (chatRoom == null || chatRoom.getChatRoomId() == null) {
            if (callback != null) callback.onLoaded(new ArrayList<>());
            return;
        }
        Long minTimestamp = 0L;
        if (chatRoom.getDeleteChat() != null && currentUserId != null) {
            Long deletedTime = chatRoom.getDeleteChat().get(currentUserId);
            if (deletedTime != null) {
                minTimestamp = deletedTime;
            }
        }
        chatRepository.loadOlderMessages(chatRoom.getChatRoomId(), minTimestamp, oldestTimestamp, limit, callback);
    }

}
