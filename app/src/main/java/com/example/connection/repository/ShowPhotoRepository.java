package com.example.connection.repository;

import android.content.Context;
import android.util.Log;
import com.example.connection.model.ChatMessage;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.Image;
import com.example.connection.model.User;
import com.example.connection.utils.SharePreUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ShowPhotoRepository {

    private final String TAG = getClass().getName();
    private final FirebaseFirestore db;
    private final String myId;
    private ChatRoom chatRoom;
    private final OnSendMessage onSendMessage;
    private User currentUser;

    public ShowPhotoRepository(OnSendMessage onSendMessage){
        db = FirebaseFirestore.getInstance();
        myId = FirebaseAuth.getInstance().getUid();
        if(myId != null){
            db.collection("users").document(myId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            User user = task.getResult().toObject(User.class);
                            if(user != null) {
                                currentUser = user;
                            }
                        }
                    });
        }
        this.onSendMessage = onSendMessage;
    }

    public interface OnUpdateWidget{
        void onUpdate(boolean isUpdate);
    }

    public interface OnSendMessage{
        void onSuccess(boolean success);
        void onExpressHeart(boolean isExpress);
    }

    public void updateSeenPostPhoto(Context context, OnUpdateWidget onUpdateWidget) {
        if (myId == null) return;
        Log.e(TAG, "updateSeenPostPhoto: ");
        db.collection("post_photo")
                .whereArrayContains("friendId", myId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        documentSnapshot.getReference().get().addOnCompleteListener(task -> {
                            if(task.isSuccessful()){
                                Image image = documentSnapshot.toObject(Image.class);
                                if (image == null) return;
                                List<String> list = image.getSeen();
                                if(list == null) {
                                    return;
                                }
                                if (list.contains(myId)) return;
                                list.add(myId);
                                documentSnapshot.getReference().update("seen", list).addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Log.e(TAG, "Câp nhật thành công");
                                    } else {
                                        Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task1.getException()).getMessage()));
                                    }
                                });
                            }
                            else Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
                        });
                    }
                    if(SharePreUtil.getInstance(context.getApplicationContext()).enableWidget()){
                        onUpdateWidget.onUpdate(true);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
    }

    public Query getQuery(){
        if(myId == null) return null;
        return db.collection("post_photo")
                .whereArrayContains("friendId", myId)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public void sendMessage(String chatRoomId, String otherId, String otherUserName, String avtPoster, String mess){
        db.collection("chatroom").document(chatRoomId)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chatRoom = task.getResult().toObject(ChatRoom.class);
                        if (chatRoom == null) {
                            Map<String, Long> mapDelete = new HashMap<>();
                            mapDelete.put(myId, Timestamp.now().toDate().getTime());
                            mapDelete.put(otherId, Timestamp.now().toDate().getTime());
                            Map<String, String> avtMap = new HashMap<>();
                            avtMap.put(otherId, avtPoster);
                            avtMap.put(currentUser.getId(), currentUser.getAvatarUser());
                            Map<String, String> aliasMap = new HashMap<>();
                            aliasMap.put(otherId, otherUserName);
                            aliasMap.put(currentUser.getId(), currentUser.getUserName());
                            List<String> searchChat = new ArrayList<>();
                            searchChat.add(otherUserName.toLowerCase());
                            searchChat.add(currentUser.getUserName().toLowerCase());
                            chatRoom = new ChatRoom(chatRoomId,
                                    null,
                                    null,
                                    Arrays.asList(myId, otherId),
                                    null,
                                    null,
                                    null,
                                    mapDelete,
                                    avtMap,
                                    aliasMap,
                                    searchChat);
                        }
                        sendMessage1(mess, chatRoomId, myId, otherId);
                    }
                });
    }

    private void sendMessage1(String message, String chatRoomId, String currentUserId, String otherUserId) {
        List<String> userId = chatRoom.getUserId();
        String idDelete = userId == null ? null : userId.get(0).contains("delete") ? userId.get(0) : userId.get(1);
        if (idDelete != null && idDelete.contains("delete")) {
            chatRoom.setUserId(Arrays.asList(currentUserId, otherUserId));
            Map<String, Long> map = chatRoom.getDeleteChat();
            Long timeCurrent = map.get(currentUserId);
            Long timeOther = map.get(otherUserId);
            if (timeCurrent != null && timeOther != null) {
                if (timeCurrent > timeOther) {
                    map.put(currentUserId, Timestamp.now().toDate().getTime());
                } else {
                    map.put(otherUserId, Timestamp.now().toDate().getTime());
                }
                chatRoom.setDeleteChat(map);
            }
        }
        chatRoom.setLastMessageSenderId(currentUserId);
        chatRoom.setLastMessageTimestamp(Timestamp.now());
        chatRoom.setLastMessage(message);
        Log.e(TAG, "sendMessage1: " + message);
        db.collection("chatroom").document(chatRoomId).set(chatRoom);

        DocumentReference docRefChatMessage = getChatroomMessageReference(chatRoomId).document();
        ChatMessage chatMessage = new ChatMessage(docRefChatMessage.getId(),message, currentUserId, Timestamp.now().toDate().getTime(), false, new HashMap<>());
        docRefChatMessage.set(chatMessage).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                onSendMessage.onSuccess(true);
                onSendMessage.onExpressHeart(message.contains("\u2764\uFE0F"));
            }
        });
    }

    private CollectionReference getChatroomMessageReference(String chatRoomId) {
        return db.collection("chatroom").document(chatRoomId).collection("chats");
    }

}
