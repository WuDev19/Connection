package com.example.connection.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.repository.RecentChatRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;
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

    public void getDelete(ChatRoom model){
        List<String> userIds = model.getUserId();
        if(userIds == null) return;
        int count = 0;
        for(String id : userIds){
            if(id.contains("_delete")){
                count++;
            }
        }
        if ((count == (userIds.size() - 1)) || userIds.get(0).equals(userIds.get(1))) {
            recentChatRepository.deleteTwoSides(model);
        }
        else {
            recentChatRepository.deleteOneSide(model, userIds);
        }
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
        return recentChatRepository.searchChatRoom(chatName);
    }

}
