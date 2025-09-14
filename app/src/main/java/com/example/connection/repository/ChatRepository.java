package com.example.connection.repository;

import android.util.Log;
import com.example.connection.model.ChatMessage;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import java.util.Objects;

public class ChatRepository {

    private final FirebaseFirestore db;
    private final String TAG = getClass().getName();
    private final String currentUserId;
    private User currentUser;
    private ListenerRegistration seenMessageListening;

    public ChatRepository(){
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        if(currentUserId != null){
            db.collection("users")
                    .document(currentUserId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            User user = task.getResult().toObject(User.class);
                            if(user != null) currentUser = user;
                        }
                    });
        }
    }

    public String getCurrentUserId(){
        return currentUserId;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public interface HandleChat{
        void onCreateChatRoom(ChatRoom chatroom);
    }

    public Task<DocumentSnapshot> getOtherUser(String otherUserId){
        return db.collection("users").document(otherUserId).get();
    }

    public void setSeenMessage(String chatRoomId) {
        if(currentUserId == null) return;
        seenMessageListening = db.collection("chatroom").document(chatRoomId)
                .collection("chats")
                .whereNotEqualTo("senderId", currentUserId)
                .whereEqualTo("seen", false)
                .addSnapshotListener((value, error) -> {
                    if(error != null) return;
                    if(value == null) return;
                    WriteBatch writeBatch = db.batch();
                    for(DocumentSnapshot documentSnapshot : value){
                        writeBatch.update(documentSnapshot.getReference(), "seen", true);
                    }
                    writeBatch.commit().addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            Log.e(TAG, "cap nhat seen thanh cong");
                        }
                        else {
                            Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
                        }
                    });
                });
    }

    public void unregisterSeenMessageListening(){
        if(seenMessageListening != null){
            Log.e(TAG, "unregisterSeenMessageListening: ");
            seenMessageListening.remove();
        }
    }

    public void getOrCreateChatRoom(String chatRoomId, HandleChat handleChat) {
        db.collection("chatroom").document(chatRoomId).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                ChatRoom chatRoom = task.getResult().toObject(ChatRoom.class);
                handleChat.onCreateChatRoom(chatRoom);
            }
        });
    }

    public void sendMessage(String message, String chatRoomId, ChatRoom chatRoom) {
        db.collection("chatroom").document(chatRoomId).set(chatRoom);
        ChatMessage chatMessage = new ChatMessage(message, currentUserId, Timestamp.now().toDate().getTime(), false);
        getChatroomMessageReference(chatRoomId).add(chatMessage).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                Log.e(TAG, "Gửi thành công");
            }
        });
    }

    private CollectionReference getChatroomMessageReference(String chatRoomId){
        return db.collection("chatroom")
                .document(chatRoomId)
                .collection("chats");
    }

    public Query getOptions(ChatRoom chatRoom1, String chatRoomId) {
        if(currentUserId == null) return null;
        Long timestamp = chatRoom1.getDeleteChat().get(currentUserId);
        if (timestamp == null) return null;
        return db.collection("chatroom")
                .document(chatRoomId)
                .collection("chats")
                .whereGreaterThanOrEqualTo("timestamp", timestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

}
