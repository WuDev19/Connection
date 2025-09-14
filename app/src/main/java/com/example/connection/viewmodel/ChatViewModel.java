package com.example.connection.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.repository.ChatRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepository;
    private String chatRoomId;
    private User otherUser;
    private ChatRoom chatRoom;
    private final String currentUserId;
    private final MutableLiveData<User> otherUserLiveData = new MutableLiveData<>();
    private final MutableLiveData<ChatRoom> chatRoomLiveData = new MutableLiveData<>();
    private final MutableLiveData<ChatRoom> createNewChat = new MutableLiveData<>();
    private final MutableLiveData<Boolean> send = new MutableLiveData<>();
    private final MutableLiveData<Query> createOptions = new MutableLiveData<>();

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
            otherUser = new User(id, avatar, username, email, null, null, null, true);
        } else if (email == null) {
            otherUser = new User(id, avatar, username, null, sdt, null, null, true);
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
            chatRoom.setLastMessage(chatRepository.getCurrentUser().getUserName() + ": " + message);
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
        chatRoom = new ChatRoom(chatRoomId, otherUser.getUserName(), null, Arrays.asList(FirebaseAuth.getInstance().getUid(), otherUser.getId())
                , null, null, null, mapDelete, avtMap);
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

}
