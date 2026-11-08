package com.example.connection.ui.viewmodel;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.repository.base.ChatRepository;
import com.example.connection.repository.impl.ChatRepositoryImpl;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecentChatViewModel extends ViewModel {

    private final ChatRepository recentChatRepository;
    private final HashMap<String, MutableLiveData<User>> chatRoomCache = new HashMap<>();
    private final Map<String, User> userCache = new HashMap<>();
    private final MutableLiveData<ChatRoom> success = new MutableLiveData<>();
    private final String TAG = getClass().getName();
    private final String avt = "https://res.cloudinary.com/dsmw3ean4/image/upload/v1759998024/avatar/hhdyowwt0im321bzdyuu.png";

    public interface OnUserInfoLoadedListener {
        void onLoaded(User user);
    }

    public RecentChatViewModel(){
        recentChatRepository = new ChatRepositoryImpl();
    }

    public void getUserInfo(ChatRoom model, OnUserInfoLoadedListener listener) {
        if (model == null || model.getUserId() == null || listener == null) return;
        String otherUserId = getOtherUserId(model.getUserId());
        if (otherUserId == null) return;
        if (otherUserId.contains("delete")) {
            String[] otherId = otherUserId.split("_");
            otherUserId = otherId[0];
        }
        final String finalUserId = otherUserId;
        if (userCache.containsKey(finalUserId)) {
            listener.onLoaded(userCache.get(finalUserId));
            return;
        }
        recentChatRepository.showInfo(finalUserId, user -> {
            if (user != null) {
                userCache.put(finalUserId, user);
            }
            listener.onLoaded(user);
        });
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

    public interface OnRandomChatCallback {
        void onSuccess(ChatRoom chatRoom);
        void onError(String message);
    }

    public void getNumberUser(int number, Context context){
        startRandomMatching(number, context, null);
    }

    public void startRandomMatching(int number, Context context, OnRandomChatCallback callback) {
        String myId = recentChatRepository.getCurrentUserId();
        if (myId == null) {
            if (callback != null) callback.onError("Bạn chưa đăng nhập");
            return;
        }

        // Lấy đúng số lượng người do người dùng nhập (mặc định tối thiểu 1, tối đa 50)
        int poolSize = (number > 0) ? Math.min(number, 50) : 5;

        // Bước 1: Ưu tiên tìm những người đang online (active == true) theo đúng số lượng poolSize đã chọn
        recentChatRepository.getActiveUsers(poolSize).addOnCompleteListener(task -> {
            List<User> candidates = new ArrayList<>();
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult()) {
                    User u = doc.toObject(User.class);
                    if (u != null && u.getId() != null && !u.getId().equals(myId)) {
                        candidates.add(u);
                    }
                }
            }

            if (!candidates.isEmpty()) {
                // Có người đang online -> Xáo trộn ngẫu nhiên và bốc 1 người
                Collections.shuffle(candidates);
                User targetUser = candidates.get(0);
                createAnonymousChat(targetUser, context, callback);
            } else {
                // Bước 2: Không có ai online -> Sử dụng Random ID Cursor trên toàn bộ hệ thống theo poolSize
                String randomKey = recentChatRepository.createChatRoomId();
                recentChatRepository.getRandomUsers(randomKey, poolSize).addOnCompleteListener(fallbackTask -> {
                    List<User> fallbackCandidates = new ArrayList<>();
                    if (fallbackTask.isSuccessful() && fallbackTask.getResult() != null) {
                        for (DocumentSnapshot doc : fallbackTask.getResult()) {
                            User u = doc.toObject(User.class);
                            if (u != null && u.getId() != null && !u.getId().equals(myId)) {
                                fallbackCandidates.add(u);
                            }
                        }
                    }

                    if (fallbackCandidates.isEmpty()) {
                        // Quét lấy danh sách người dùng chung nếu randomKey rơi vào đuôi theo poolSize
                        recentChatRepository.getNumberUser(poolSize).addOnCompleteListener(allTask -> {
                            List<User> allCandidates = new ArrayList<>();
                            if (allTask.isSuccessful() && allTask.getResult() != null) {
                                for (DocumentSnapshot doc : allTask.getResult()) {
                                    User u = doc.toObject(User.class);
                                    if (u != null && u.getId() != null && !u.getId().equals(myId)) {
                                        allCandidates.add(u);
                                    }
                                }
                            }
                            if (!allCandidates.isEmpty()) {
                                Collections.shuffle(allCandidates);
                                createAnonymousChat(allCandidates.get(0), context, callback);
                            } else {
                                if (callback != null) callback.onError("Hiện không tìm thấy người dùng phù hợp để ghép nối. Hãy thử lại sau!");
                            }
                        });
                    } else {
                        Collections.shuffle(fallbackCandidates);
                        createAnonymousChat(fallbackCandidates.get(0), context, callback);
                    }
                });
            }
        });
    }

    public void createAnonymousChat(User otherUser, Context context, OnRandomChatCallback callback){
        if (otherUser == null) {
            if (callback != null) callback.onError("Người dùng không hợp lệ");
            return;
        }
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
        Timestamp now = Timestamp.now();
        ChatRoom chatRoom = new ChatRoom(
                chatroomId,
                "Anonymous Chat",
                avt,
                Arrays.asList(otherUser.getId(), currentId),
                now,
                currentId,
                "Đoạn chat ngẫu nhiên đã được tạo",
                mapDelete,
                avtMap,
                aliasMap,
                new ArrayList<>(),
                "",
                1);
        chatRoom.setUnreadCounts(new HashMap<>());
        recentChatRepository.createChatRoom(chatRoom).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                success.setValue(chatRoom);
                Log.e("deleteRandomChat", "createAnonymousChat: " + chatroomId);
                recentChatRepository.scheduleDeleteRandomChat(context, chatroomId);
                if (callback != null) callback.onSuccess(chatRoom);
            }
            else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Lỗi tạo phòng chat";
                Log.e(TAG, "onComplete: " + errorMsg);
                if (callback != null) callback.onError(errorMsg);
            }
        });
    }

    public void createAnonymousChat(User otherUser, Context context){
        createAnonymousChat(otherUser, context, null);
    }

    public LiveData<ChatRoom> getSuccess() {
        return success;
    }

}
