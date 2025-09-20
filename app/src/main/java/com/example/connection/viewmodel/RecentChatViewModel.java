package com.example.connection.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.repository.RecentChatRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RecentChatViewModel extends ViewModel {

    private final RecentChatRepository recentChatRepository;
    private final HashMap<String, MutableLiveData<User>> chatRoomCache = new HashMap<>();

    public RecentChatViewModel(){
        recentChatRepository = new RecentChatRepository();
    }

    public LiveData<User> getInfo(ChatRoom model){
        if(chatRoomCache.containsKey(model.getChatRoomId())){
            return chatRoomCache.get(model.getChatRoomId());
        }
        String currentUserId = recentChatRepository.getCurrentUserId();
        MutableLiveData<User> getInfo = new MutableLiveData<>();
        chatRoomCache.put(model.getChatRoomId(), getInfo);
        if (currentUserId == null) return null;
        String otherUserId = getOtherUserId(model.getUserId());
        if (otherUserId == null) return null;
        if (otherUserId.contains("delete")) {
            String[] otherId = otherUserId.split("_");
            otherUserId = otherId[0];
        }
        recentChatRepository.showInfo(otherUserId, getInfo::setValue);
        return getInfo;
    }

    public LiveData<Query> getQuery(){
        return recentChatRepository.getQuery();
    }

    public LiveData<Boolean> getIsNotSeen(ChatRoom model){
        MutableLiveData<Boolean> isNotSeen = new MutableLiveData<>();
        recentChatRepository.getIsNotSeen(model, isNotSeen::setValue);
        return isNotSeen;
    }

    private String getOtherUserId(List<String> list) {
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return null;
        if (myId.equals(list.get(0))) {
            return list.get(1);
        } else {
            return list.get(0);
        }
    }

    public LiveData<List<ChatRoom>> searchChatRoom(String chatName){
        MutableLiveData<List<ChatRoom>> listMutableLiveData = new MutableLiveData<>();
        List<ChatRoom> listChat = new ArrayList<>();
        recentChatRepository.searchChatRoom(roomList -> {
            if(roomList != null){
                for(ChatRoom chatRoom : roomList){
                    List<String> searchChat = chatRoom.getSearchChat();
                    StringBuilder search = new StringBuilder();
                    for(String s : searchChat){
                        search.append(s);
                    }
                    if(search.toString().contains(chatName.toLowerCase())){
                        listChat.add(chatRoom);
                    }
                }
                listMutableLiveData.setValue(listChat);
            }
        });
        return listMutableLiveData;
    }

}
