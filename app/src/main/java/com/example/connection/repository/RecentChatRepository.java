package com.example.connection.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.work.DeleteWork;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RecentChatRepository {

    private final String currentUserId;
    private final FirebaseFirestore db;
    private final String TAG = getClass().getName();

    public RecentChatRepository() {
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public interface OnShowInfo{
        void onShow(User otherUser);
    }

    public interface OnSearchChatRoom{
        void onSearch(List<ChatRoom> roomList);
    }

    public interface OnSeenChatRoom{
        void onSeen(boolean seen);
    }

    public String getCurrentUserId(){
        return currentUserId;
    }

    public LiveData<Query> getQuery() {
        MutableLiveData<Query> queryMutableLiveData = new MutableLiveData<>();
        if (currentUserId == null) queryMutableLiveData.setValue(null);
        else {
            Query query = db.collection("chatroom")
                    .whereArrayContains("userId", currentUserId)
                    .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);
            queryMutableLiveData.setValue(query);
        }
        return queryMutableLiveData;
    }

    public void showInfo(String otherUserId, OnShowInfo onShowInfo) {
        db.collection("users").document(otherUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) return;
            User otherUser = documentSnapshot.toObject(User.class);
            if (otherUser == null) return;
            Log.e(TAG, otherUser.toString());
            onShowInfo.onShow(otherUser);
        });
    }

    public void getIsNotSeen(ChatRoom model, OnSeenChatRoom onSeenChatRoom){
        db.collection("chatroom").document(model.getChatRoomId())
                .collection("chats")
                .whereNotEqualTo("senderId", FirebaseAuth.getInstance().getUid())
                .whereEqualTo("seen", false)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.e(TAG, queryDocumentSnapshots.size() + "");
                    if (!queryDocumentSnapshots.isEmpty()) {
                        onSeenChatRoom.onSeen(true);
                        Log.e(TAG, queryDocumentSnapshots.size() + "");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, Objects.requireNonNull(e.getMessage())));
    }

    public void searchChatRoom(OnSearchChatRoom onSearchChatRoom) {
        db.collection("chatroom")
                .whereArrayContains("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<ChatRoom> listChat = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : task.getResult()) {
                            listChat.add(documentSnapshot.toObject(ChatRoom.class));
                            Log.e(TAG, "searchChatRoom: " + documentSnapshot);
                        }
                        onSearchChatRoom.onSearch(listChat);
                    } else {
                        onSearchChatRoom.onSearch(null);
                        Log.e(TAG, "searchChatRoom: loi" + Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }

    public Task<QuerySnapshot> getNumberUser(int number){
        return db.collection("users")
                .limit(number)
                .get();
    }

    public String createChatRoomId(){
        return db.collection("chatroom")
                .document()
                .getId();
    }

    public Task<Void> createChatRoom(ChatRoom chatRoom){
        return db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .set(chatRoom);
    }

    public void scheduleDeleteRandomChat(Context context, String chatRoomId){
        Log.e("deleteRandomChat", "scheduleDeleteRandomChat: ");
        Data data = new Data.Builder()
                .putString("chatRoomId", chatRoomId)
                .build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DeleteWork.class)
                .setInputData(data)
                .setInitialDelay(15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork("deleteRandomChat" + chatRoomId, ExistingWorkPolicy.KEEP, workRequest);
    }

}
