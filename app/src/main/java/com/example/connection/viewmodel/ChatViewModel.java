package com.example.connection.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.repository.ChatRepository;
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
        chatRepository = new ChatRepository();
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
        boolean check = false;
        for(String id : userIds){
            if(id.contains("delete")){
                check = true;
                userIds.remove(id);
                id = id.split("_")[0];
                userIds.add(id);
                map.put(id, Timestamp.now().toDate().getTime());
            }
        }
        if(check){
            chatRoom.setUserId(userIds);
            chatRoom.setDeleteChat(map);
        }
        send.setValue(true);
        chatRoom.setLastMessageSenderId(currentUserId);
        chatRoom.setLastMessageTimestamp(Timestamp.now());
        if(userIds.size() >= 3){
            String alias = chatRoom.getAlias().get(chatRepository.getCurrentUser().getId());
            if(alias != null){
                if(alias.contains("_")){
                    alias = alias.split("_")[1];
                }
            }
            chatRoom.setLastMessage(alias + ": " + message);
        }else {
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
                Arrays.asList(currentUserId, otherUser.getId())
                ,
                null,
                null,
                null,
                mapDelete,
                avtMap,
                aliasMap,
                searchChat);
        createNewChat.setValue(chatRoom);
    }

    public LiveData<User> getOtherUser(String otherUserId) {
        MutableLiveData<User> otherUserExistLiveData = new MutableLiveData<>();
        chatRepository.getOtherUser(otherUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User other = task.getResult().toObject(User.class);
                if(other != null) otherUser = other;
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

    public void listeningChatRoom(){
        chatRepository.listeningChatRoom(chatRoom, value -> {
            if(value != null) chatRoom = value;
            chatRoomListeningLiveData.setValue(value);
        });
    }

    public void unregisterListeningChatRoom(){
        chatRepository.unregisterListeningChatRoom();
    }

    public ChatRoom getChatRoom() {
        return chatRoom;
    }

    public void getDelete(){
        List<String> userIds = chatRoom.getUserId();
        if(userIds == null) return;
        int count = 0;
        for(String id : userIds){
            if(id.contains("_delete")){
                count++;
            }
        }
        if ((count == (userIds.size() - 1)) || userIds.get(0).equals(userIds.get(1))) {
            chatRepository.deleteTwoSides(chatRoom);
        }
        else {
            chatRepository.deleteOneSide(chatRoom, userIds);
        }

    }

    public void updateChatNameTwoPeople(String alias, String otherId){
        Map<String, String> aliasMap = chatRoom.getAlias();
        List<String> searchChat = chatRoom.getSearchChat();
        String oldAlias = aliasMap.get(otherId);
        String newAlias = null;
        if(oldAlias != null){
            if(oldAlias.contains("_")){
                if(alias != null){
                    newAlias = oldAlias.split("_")[0] + "_" + alias;
                    searchChat.add(alias.toLowerCase());
                    searchChat.remove(oldAlias.split("_")[1].toLowerCase());
                } else {
                    newAlias = oldAlias.split("_")[0];
                    searchChat.remove(oldAlias.split("_")[1].toLowerCase());
                }
            }
            else {
                if (alias != null) {
                    newAlias = oldAlias + "_" + alias;
                    searchChat.add(alias.toLowerCase());
                }
            }
        }
        if (newAlias != null) aliasMap.put(otherId, newAlias);
        chatRoom.setAlias(aliasMap);
        chatRoom.setSearchChat(searchChat);
        chatRepository.updateChatNameTwoPeople(chatRoom).addOnCompleteListener(task -> {
            if(!task.isSuccessful()){
                Log.e(TAG, "onComplete: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
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

}
