package com.example.connection.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public void deleteTwoSides(ChatRoom model){
        db.collection("chatroom").document(model.getChatRoomId()).collection("chats")
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> lists = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot1 : queryDocumentSnapshots) {
                        lists.add(documentSnapshot1);
                    }
                    deleteMessagesInBatches(lists, model);
                });
    }

    public void deleteOneSide(ChatRoom model, List<String> userIds){
        userIds.remove(currentUserId);
        userIds.add(currentUserId + "_delete");
        db.collection("chatroom").document(model.getChatRoomId())
                .update("userId", userIds)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Long> deleteChat = model.getDeleteChat();
                        deleteChat.put(currentUserId, Timestamp.now().toDate().getTime());
                        db.collection("chatroom").document(model.getChatRoomId())
                                .update("deleteChat", deleteChat)
                                .addOnCompleteListener(task12 -> {
                                    if (task12.isSuccessful()) {
                                        Log.e(TAG, "Cập nhật thời gian xóa thành công");
                                    } else
                                        Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task12.getException()).getMessage()));
                                });
                    } else {
                        Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
                    }
                });
    }

    private void deleteMessagesInBatches(List<DocumentSnapshot> snapshots, ChatRoom model) {
        if (snapshots.isEmpty()) {
            Log.e(TAG, "Không có tin nhắn nào để xóa.");
            return;
        }

        WriteBatch batch = db.batch();
        int batchSize = 0;

        for (DocumentSnapshot doc : snapshots) {
            batch.delete(doc.getReference());
            batchSize++;

            if (batchSize == 499) {
                batch.commit().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.e(TAG, "Batch đã commit thành công.");
                    } else {
                        Log.e(TAG, "Lỗi khi commit batch: " + Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
                batch = db.batch();
                batchSize = 0;
            }
        }

        if (batchSize > 0) {
            batch.commit().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.e(TAG, "Batch cuối cùng đã commit thành công.");
                    db.collection("chatroom").document(model.getChatRoomId()).delete().addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            Log.e(TAG, "Xóa thành công");
                        } else {
                            Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task2.getException()).getMessage()));
                        }
                    });
                } else {
                    Log.e(TAG, "Lỗi khi commit batch cuối cùng: " + Objects.requireNonNull(task.getException()).getMessage());
                }
            });
        } else if (batchSize == 0) {
            db.collection("chatroom").document(model.getChatRoomId()).delete().addOnCompleteListener(task2 -> {
                if (task2.isSuccessful()) {
                    Log.e(TAG, "Xóa thành công");
                } else {
                    Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task2.getException()).getMessage()));
                }
            });
        }
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

    public LiveData<List<ChatRoom>> searchChatRoom(String chatName){
        MutableLiveData<List<ChatRoom>> chatRoomMutableLiveData = new MutableLiveData<>();
        db.collection("chatroom")
                .whereArrayContains("userId", currentUserId)
                .whereGreaterThanOrEqualTo("chatName", chatName)
                .whereLessThanOrEqualTo("chatName", chatName + "\uf8ff")
                .orderBy("chatName")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        List<ChatRoom> listChat = new ArrayList<>();
                        for(DocumentSnapshot documentSnapshot : task.getResult()){
                            listChat.add(documentSnapshot.toObject(ChatRoom.class));
                        }
                        chatRoomMutableLiveData.setValue(listChat);
                    }
                    else {
                        chatRoomMutableLiveData.setValue(null);
                    }
                });
        return chatRoomMutableLiveData;
    }

}
