package com.example.connection.ui.viewmodel;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.repository.RecentChatRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class RecentChatViewModel extends ViewModel {

    private final RecentChatRepository recentChatRepository;
    private final HashMap<String, MutableLiveData<User>> chatRoomCache = new HashMap<>();
    private final MutableLiveData<ChatRoom> success = new MutableLiveData<>();
    private final String TAG = getClass().getName();
    private final String avt = "https://res.cloudinary.com/dsmw3ean4/image/upload/v1759998024/avatar/hhdyowwt0im321bzdyuu.png";

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

    public void getNumberUser(int number, Context context){
        recentChatRepository.getNumberUser(number).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                QuerySnapshot querySnapshot = task.getResult();
                List<User> list = querySnapshot.toObjects(User.class);
                Random random = new Random();
                int randomInt = random.nextInt(list.size());
                User user = list.get(randomInt);
                if(user != null){
                    createAnonymousChat(user, context);
                }
            }
        });
    }

    public void createAnonymousChat(User otherUser, Context context){
        String chatroomId = recentChatRepository.createChatRoomId();
        String currentId = recentChatRepository.getCurrentUserId();
        Map<String, Long> mapDelete = new HashMap<>();
        mapDelete.put(currentId, Timestamp.now().toDate().getTime());
        mapDelete.put(otherUser.getId(), Timestamp.now().toDate().getTime());
        Map<String, String> avtMap = new HashMap<>();
        avtMap.put(otherUser.getId(), avt);
        avtMap.put(currentId, avt);
        Map<String, String> aliasMap = new HashMap<>();
        aliasMap.put(otherUser.getId(), "Anonymous");
        aliasMap.put(currentId, "Anonymous");
        ChatRoom chatRoom = new ChatRoom(
                chatroomId,
                "Anonymous Chat",
                avt,
                Arrays.asList(otherUser.getId(), currentId),
                null,
                null,
                null,
                mapDelete,
                avtMap,
                aliasMap,
                new ArrayList<>(),
                "",
                1);
        recentChatRepository.createChatRoom(chatRoom).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                success.setValue(chatRoom);
                Log.e("deleteRandomChat", "createAnonymousChat: " + chatroomId);
                recentChatRepository.scheduleDeleteRandomChat(context, chatroomId);
            }
            else {
                Log.e(TAG, "onComplete: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }

    public LiveData<ChatRoom> getSuccess() {
        return success;
    }

}
