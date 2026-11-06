package com.example.connection.repository;

import android.content.Context;
import android.util.Log;
import com.example.connection.model.ChatMessage;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.EmoteModel;
import com.example.connection.model.Image;
import com.example.connection.model.User;
import com.example.connection.constants.SharePreUtil;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShowPhotoRepository {

    private final String TAG = getClass().getName();
    private final FirebaseFirestore db;
    private final String myId;
    private ChatRoom chatRoom;
    private final OnSendMessage onSendMessage;
    private User currentUser;

    public ShowPhotoRepository(OnSendMessage onSendMessage) {
        db = FirebaseFirestore.getInstance();
        myId = FirebaseAuth.getInstance().getUid();
        if (myId != null) {
            db.collection("users").document(myId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            User user = task.getResult().toObject(User.class);
                            if (user != null) {
                                currentUser = user;
                            }
                        }
                    });
        }
        this.onSendMessage = onSendMessage;
    }

    public interface OnUpdateWidget {
        void onUpdate(boolean isUpdate);
    }

    public interface OnSendMessage {
        void onSuccess(boolean success);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void updateSeenPostPhoto(Context context, OnUpdateWidget onUpdateWidget) {
        if (myId == null) return;
        Log.e(TAG, "updateSeenPostPhoto: ");
        db.collection("post_photo")
                .whereArrayContains("friendId", myId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Image image = documentSnapshot.toObject(Image.class);
                        if (image == null) continue;
                        List<String> list = image.getSeen();
                        if (list == null || !list.contains(myId)) {
                            documentSnapshot.getReference().update("seen", FieldValue.arrayUnion(myId))
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.e(TAG, "Cập nhật seen thành công");
                                        } else if (task1.getException() != null) {
                                            Log.e(TAG, task1.getException().getMessage());
                                        }
                                    });
                        }
                    }
                    if (SharePreUtil.getInstance(context.getApplicationContext()).enableWidget()) {
                        onUpdateWidget.onUpdate(true);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
    }

    public Query getQuery() {
        if (myId == null) return null;
        return db.collection("post_photo")
                .whereArrayContains("friendId", myId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10);
    }

    public void sendMessage(String chatRoomId, String otherId, String otherUserName, String avtPoster, String mess) {
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
                                    searchChat,
                                    "",
                                    0);
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

        Map<String, Long> unread = chatRoom.getUnreadCounts();
        long currentUnread = unread.getOrDefault(otherUserId, 0L);
        unread.put(otherUserId, currentUnread + 1);
        chatRoom.setUnreadCounts(unread);

        db.collection("chatroom").document(chatRoomId).set(chatRoom);

        DocumentReference docRefChatMessage = getChatroomMessageReference(chatRoomId).document();
        ChatMessage chatMessage = new ChatMessage(docRefChatMessage.getId(), message, currentUserId, Timestamp.now().toDate().getTime(), false, new HashMap<>());
        docRefChatMessage.set(chatMessage).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                onSendMessage.onSuccess(true);
            }
        });
    }

    private CollectionReference getChatroomMessageReference(String chatRoomId) {
        return db.collection("chatroom").document(chatRoomId).collection("chats");
    }

    public String getIdEmote(String postId) {
        DocumentReference emoteRef = db.collection("post_photo").document(postId).collection("emotes").document();
        return emoteRef.getId();
    }

    @SuppressWarnings("unchecked")
    public Task<Void> modifyEmote(String postId, String emote) {
        DocumentReference postRef = db.collection("post_photo").document(postId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot postSnap = transaction.get(postRef);
            Map<String, String> emoteMap = (Map<String, String>) postSnap.get("emoteMap");
            Long count = postSnap.getLong("numberEmote");
            if (emoteMap != null) {
                if (emoteMap.containsKey(myId)) {
                    String emoteIdAndEmote = emoteMap.get(myId);
                    if (emoteIdAndEmote != null) {
                        String emoteId = emoteIdAndEmote.split("_")[0];
                        DocumentReference emoteRef = db.collection("post_photo").document(postId).collection("emotes").document(emoteId);
                        DocumentSnapshot emoteSnap = transaction.get(emoteRef);
                        String oldEmote = emoteIdAndEmote.split("_")[1];
                        if (oldEmote.equals(emote)) {
                            Log.e(TAG, "modifyEmote: xoa");
                            emoteMap.remove(myId);
                            long newCount = (count != null ? count : 0) - 1;
                            if (newCount < 0) newCount = 0;
                            if (emoteSnap.exists()) {
                                transaction.delete(emoteRef);
                            }
                            transaction.update(postRef, "numberEmote", newCount);
                            transaction.update(postRef, "emoteMap", emoteMap);
                        } else {
                            Log.e(TAG, "modifyEmote: cap nhat");
                            transaction.update(emoteRef, "emote", emote);
                            transaction.update(emoteRef, "timestamp", Timestamp.now());
                            transaction.update(postRef, "emoteMap." + myId, emoteId + "_" + emote);
                        }
                    }
                } else {
                    Log.e(TAG, "modifyEmote: tao moi");
                    String emoteId = getIdEmote(postId);
                    DocumentReference emoteRef = db.collection("post_photo").document(postId).collection("emotes").document(emoteId);
                    EmoteModel model = new EmoteModel(emoteId,
                            myId,
                            currentUser.getAvatarUser(),
                            currentUser.getUserName(),
                            emote,
                            Timestamp.now());
                    long newCount = (count != null ? count : 0) + 1;
                    transaction.update(postRef, "numberEmote", newCount);
                    transaction.update(postRef, "emoteMap." + myId, model.getIdEmote() + "_" + emote);
                    transaction.set(emoteRef, model);
                }
            }
            return null;
        });
    }

}
