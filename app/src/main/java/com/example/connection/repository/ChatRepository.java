package com.example.connection.repository;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.connection.model.ChatMessage;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ChatRepository {

    private final FirebaseFirestore db;
    private final String TAG = getClass().getName();
    private final String currentUserId;
    private User currentUser;
    private ListenerRegistration seenMessageListening, chatRoomListening, reactionListening, recentMessagesRegistration;

    public interface OnRecentMessagesListener {
        void onInitialLoaded(List<ChatMessage> initialMessages);
        void onMessageAdded(ChatMessage message);
        void onMessageModified(ChatMessage message);
        void onMessageRemoved(String idMessage);
        void onError(Exception e);
    }

    public interface OnOlderMessagesLoadedListener {
        void onLoaded(List<ChatMessage> olderMessages);
        void onError(Exception e);
    }

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

    public interface OnUpdateChatRoom{
        void onUpdate(ChatRoom chatRoom);
    }

    public Task<DocumentSnapshot> getOtherUser(String otherUserId){
        return db.collection("users").document(otherUserId).get();
    }

    public void setSeenMessage(String chatRoomId) {
        if(currentUserId == null || chatRoomId == null) return;

        db.collection("chatroom").document(chatRoomId)
                .update("unreadCounts." + currentUserId, 0L);

        if(seenMessageListening != null) seenMessageListening.remove();
        seenMessageListening = db.collection("chatroom").document(chatRoomId)
                .collection("chats")
                .whereNotEqualTo("senderId", currentUserId)
                .whereEqualTo("seen", false)
                .limit(20)
                .addSnapshotListener((value, error) -> {
                    if(error != null || value == null || value.isEmpty()) return;
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
        if (chatRoom != null && chatRoomId != null) {
            Map<String, Long> unread = chatRoom.getUnreadCounts();
            if (chatRoom.getUserId() != null) {
                for (String uid : chatRoom.getUserId()) {
                    String cleanUid = uid.split("_")[0];
                    if (!cleanUid.equals(currentUserId)) {
                        long count = unread.getOrDefault(cleanUid, 0L);
                        unread.put(cleanUid, count + 1);
                    }
                }
            }
            chatRoom.setUnreadCounts(unread);
            db.collection("chatroom").document(chatRoomId).set(chatRoom);
        }
        DocumentReference docRefChatMessage = getChatroomMessageReference(chatRoomId).document();
        ChatMessage chatMessage = new ChatMessage(docRefChatMessage.getId(), message, currentUserId, Timestamp.now().toDate().getTime(), false, new HashMap<>());
        docRefChatMessage.set(chatMessage).addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                Log.e(TAG, "Gửi thành công");
            }
        });
    }

    public Task<Void> expressEmote(ChatRoom chatRoom, String idMessage, String idUser, String emote){
        return getChatroomMessageReference(chatRoom.getChatRoomId())
                .document(idMessage)
                .update("emote." + idUser, emote);
    }

    public Task<Void> deleteEmote(ChatRoom chatRoom, String idMessage, String idUser){
        return getChatroomMessageReference(chatRoom.getChatRoomId())
                .document(idMessage)
                .update("emote." + idUser, FieldValue.delete());
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

    public void listeningChatRoom(ChatRoom chatRoom, OnUpdateChatRoom updateChatRoom){
        if(chatRoomListening != null) {
            chatRoomListening.remove();
            Log.e("ChatActivity", "listeningChatRoom: remove");
        }
        chatRoomListening = db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .addSnapshotListener((value, error) -> {
                    if(error != null) return;
                    if(value != null && !value.exists()){
                        updateChatRoom.onUpdate(null);
                        return;
                    }
                    if(value == null) return;
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

    public LiveData<Map<String, String>> listeningReaction(ChatRoom chatRoom, String idMess){
        MutableLiveData<Map<String, String>> mapMutableLiveData = new MutableLiveData<>();

        if(reactionListening != null){
            reactionListening.remove();
            reactionListening = null;
        }
        if (chatRoom == null || chatRoom.getChatRoomId() == null || idMess == null) {
            return mapMutableLiveData;
        }

        reactionListening = getChatroomMessageReference(chatRoom.getChatRoomId())
                .document(idMess)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;
                    ChatMessage chatMessage = snapshot.toObject(ChatMessage.class);
                    if (chatMessage != null && chatMessage.getEmote() != null) {
                        mapMutableLiveData.setValue(chatMessage.getEmote());
                        Log.d(TAG, "listeningReaction updated for: " + idMess);
                    }
                });
        return mapMutableLiveData;
    }

    public void unregisterListeningReaction(){
        if(reactionListening != null){
            Log.e("ChatActivity", "unregisterListeningReaction: ");
            reactionListening.remove();
            reactionListening = null;
        }
    }

    public ListenerRegistration listenRecentMessages(String chatRoomId, Long minTimestamp, int limit, OnRecentMessagesListener listener) {
        if (recentMessagesRegistration != null) {
            recentMessagesRegistration.remove();
            recentMessagesRegistration = null;
        }
        if (chatRoomId == null || listener == null) return null;
        long filterTimestamp = (minTimestamp != null) ? minTimestamp : 0L;

        Query query = db.collection("chatroom").document(chatRoomId)
                .collection("chats")
                .whereGreaterThanOrEqualTo("timestamp", filterTimestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit);

        final boolean[] isInitial = {true};

        recentMessagesRegistration = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                listener.onError(error);
                return;
            }
            if (snapshots == null) return;

            if (isInitial[0]) {
                isInitial[0] = false;
                List<ChatMessage> list = new ArrayList<>();
                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                    ChatMessage msg = doc.toObject(ChatMessage.class);
                    if (msg != null) {
                        list.add(msg);
                    }
                }
                listener.onInitialLoaded(list);
            } else {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    ChatMessage msg = dc.getDocument().toObject(ChatMessage.class);
                    if (msg == null) continue;
                    switch (dc.getType()) {
                        case ADDED:
                            listener.onMessageAdded(msg);
                            break;
                        case MODIFIED:
                            listener.onMessageModified(msg);
                            break;
                        case REMOVED:
                            listener.onMessageRemoved(msg.getIdMessage());
                            break;
                    }
                }
            }
        });
        return recentMessagesRegistration;
    }

    public void unregisterRecentMessages() {
        if (recentMessagesRegistration != null) {
            recentMessagesRegistration.remove();
            recentMessagesRegistration = null;
        }
    }

    public void loadOlderMessages(String chatRoomId, Long minTimestamp, Long oldestTimestamp, int limit, OnOlderMessagesLoadedListener callback) {
        if (chatRoomId == null || oldestTimestamp == null || callback == null) {
            if (callback != null) callback.onLoaded(new ArrayList<>());
            return;
        }
        long filterTimestamp = (minTimestamp != null) ? minTimestamp : 0L;

        db.collection("chatroom").document(chatRoomId)
                .collection("chats")
                .whereGreaterThanOrEqualTo("timestamp", filterTimestamp)
                .whereLessThan("timestamp", oldestTimestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChatMessage> olderMessages = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ChatMessage msg = doc.toObject(ChatMessage.class);
                            if (msg != null) {
                                olderMessages.add(msg);
                            }
                        }
                    }
                    callback.onLoaded(olderMessages);
                })
                .addOnFailureListener(callback::onError);
    }

}
