package com.example.connection.repository;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.connection.listener.OnStateListener;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FragmentChangeInfoChatRepository {
    private final FirebaseFirestore db;
    private final OnStateListener onStateListener;
    private final String TAG = getClass().getName();
    private ListenerRegistration chatRoomListening;

    public FragmentChangeInfoChatRepository(OnStateListener onStateListener){
        db = FirebaseFirestore.getInstance();
        this.onStateListener = onStateListener;
    }

    public interface OnUpdateChatRoom{
        void onUpdate(ChatRoom chatRoom);
    }

    public Task<Void> updateChatGroupName(String chatroomId, String name){
        return db.collection("chatroom")
                .document(chatroomId)
                .update("chatName", name);
    }

    public void upload_avt(Uri uri, ChatRoom chatRoom) {
        if (uri == null) {
            onStateListener.onFailure("Ảnh không tồn tại");
            return;
        }
        MediaManager.get().upload(uri).unsigned("post_upload").option("folder", "avatar/").callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {}

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {

            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                if (imageUrl != null){
                    updateChatRoomToFirebase(imageUrl, chatRoom);
                }
            }

            @Override
            public void onError(String requestId, ErrorInfo errorInfo) {
                onStateListener.onFailure(errorInfo.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {

            }
        }).dispatch();
    }

    private void updateChatRoomToFirebase(String avtUrl, ChatRoom chatRoom) {
        db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .update("chatRoomAvt", avtUrl)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        onStateListener.onSuccess("Cập nhật avatar thành công");
                    }
                    else {
                        Exception exception = task.getException();
                        if(exception != null){
                            onStateListener.onFailure(exception.getMessage());
                        }
                    }
                });
    }

    public LiveData<List<User>> searchUser(String username) {
        MutableLiveData<List<User>> userSearchLiveData = new MutableLiveData<>();
        db.collection("users")
                .whereGreaterThanOrEqualTo("userNameLowerCase", username.toLowerCase())
                .whereLessThanOrEqualTo("userNameLowerCase", username.toLowerCase() + "\uf8ff")
                .orderBy("userNameLowerCase")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<User> listUser = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : task.getResult()) {
                            listUser.add(documentSnapshot.toObject(User.class));
                        }
                        userSearchLiveData.setValue(listUser);
                    } else {
                        userSearchLiveData.setValue(null);
                    }
                });
        return userSearchLiveData;
    }

    public Task<Void> addMemberToGroup(ChatRoom chatRoom){
        return db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .set(chatRoom);
    }

    public Task<Void> leaveChat(ChatRoom chatRoom){
        return db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .set(chatRoom);
    }

    public void listeningChatRoom(ChatRoom chatRoom, OnUpdateChatRoom updateChatRoom){
        if(chatRoomListening != null) {
            chatRoomListening.remove();
            Log.e("ChatActivity", "listeningChatRoom: remove");
        }
        chatRoomListening = db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .addSnapshotListener((value, error) -> {
                    if(value == null || error != null) return;
                    ChatRoom chatRoom1 = value.toObject(ChatRoom.class);
                    if(chatRoom1 != null){
                        updateChatRoom.onUpdate(chatRoom1);
                    }
                });
    }

    public void unregisterListeningChatRoom(){
        if(chatRoomListening != null) {
            chatRoomListening.remove();
        }
    }

    public void deleteOneSide(ChatRoom model, List<String> userIds){
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if(currentUserId == null) return;
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

}
