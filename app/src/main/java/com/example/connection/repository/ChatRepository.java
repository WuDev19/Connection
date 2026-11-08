package com.example.connection.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.connection.data.worker.DeleteWork;
import com.example.connection.model.ChatMessage;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.ui.state.OnStateListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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

    public interface HandleChat {
        void onCreateChatRoom(ChatRoom chatroom);
    }

    public interface OnUpdateChatRoom {
        void onUpdate(ChatRoom chatRoom);
    }

    public interface OnShowInfo {
        void onShow(User otherUser);
    }

    public interface OnSearchChatRoom {
        void onSearch(List<ChatRoom> roomList);
    }

    public interface OnSeenChatRoom {
        void onSeen(boolean seen);
    }

    public ChatRepository() {
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId != null) {
            db.collection("users")
                    .document(currentUserId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            User user = task.getResult().toObject(User.class);
                            if (user != null) currentUser = user;
                        }
                    });
        }
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Task<DocumentSnapshot> getUser() {
        if (currentUserId == null) return null;
        return db.collection("users").document(currentUserId).get();
    }

    public Task<DocumentSnapshot> getUserFriend(String id) {
        return db.collection("users").document(id).get();
    }

    public Task<DocumentSnapshot> getOtherUser(String otherUserId) {
        return db.collection("users").document(otherUserId).get();
    }

    public LiveData<List<User>> searchUser(String username) {
        MutableLiveData<List<User>> userSearchLiveData = new MutableLiveData<>();
        db.collection("users")
                .whereGreaterThanOrEqualTo("userNameLowerCase", username.toLowerCase())
                .whereLessThanOrEqualTo("userNameLowerCase", username.toLowerCase() + "\uf8ff")
                .orderBy("userNameLowerCase")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
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

    public void setSeenMessage(String chatRoomId) {
        if (currentUserId == null || chatRoomId == null) return;

        db.collection("chatroom").document(chatRoomId)
                .update("unreadCounts." + currentUserId, 0L);

        if (seenMessageListening != null) seenMessageListening.remove();
        seenMessageListening = db.collection("chatroom").document(chatRoomId)
                .collection("chats")
                .whereNotEqualTo("senderId", currentUserId)
                .whereEqualTo("seen", false)
                .limit(20)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null || value.isEmpty()) return;
                    WriteBatch writeBatch = db.batch();
                    for (DocumentSnapshot documentSnapshot : value) {
                        writeBatch.update(documentSnapshot.getReference(), "seen", true);
                    }
                    writeBatch.commit().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.e(TAG, "cap nhat seen thanh cong");
                        } else {
                            Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
                        }
                    });
                });
    }

    public void unregisterSeenMessageListening() {
        if (seenMessageListening != null) {
            Log.e(TAG, "unregisterSeenMessageListening: ");
            seenMessageListening.remove();
            seenMessageListening = null;
        }
    }

    public void getOrCreateChatRoom(String chatRoomId, HandleChat handleChat) {
        db.collection("chatroom").document(chatRoomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                ChatRoom chatRoom = task.getResult().toObject(ChatRoom.class);
                handleChat.onCreateChatRoom(chatRoom);
            }
        });
    }

    public void sendMessage(String message, String chatRoomId, ChatRoom chatRoom) {
        if (chatRoom != null && chatRoomId != null) {
            Map<String, Long> unread = chatRoom.getUnreadCounts();
            if (unread == null) {
                unread = new HashMap<>();
            }
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
            if (task.isSuccessful()) {
                Log.e(TAG, "Gửi thành công");
            }
        });
    }

    public Task<Void> expressEmote(ChatRoom chatRoom, String idMessage, String idUser, String emote) {
        return getChatroomMessageReference(chatRoom.getChatRoomId())
                .document(idMessage)
                .update("emote." + idUser, emote);
    }

    public Task<Void> deleteEmote(ChatRoom chatRoom, String idMessage, String idUser) {
        return getChatroomMessageReference(chatRoom.getChatRoomId())
                .document(idMessage)
                .update("emote." + idUser, FieldValue.delete());
    }

    private CollectionReference getChatroomMessageReference(String chatRoomId) {
        return db.collection("chatroom")
                .document(chatRoomId)
                .collection("chats");
    }

    public Query getOptions(ChatRoom chatRoom1, String chatRoomId) {
        if (currentUserId == null || chatRoom1 == null || chatRoom1.getDeleteChat() == null) return null;
        Long timestamp = chatRoom1.getDeleteChat().get(currentUserId);
        if (timestamp == null) return null;
        return db.collection("chatroom")
                .document(chatRoomId)
                .collection("chats")
                .whereGreaterThanOrEqualTo("timestamp", timestamp)
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public void listeningChatRoom(ChatRoom chatRoom, OnUpdateChatRoom updateChatRoom) {
        if (chatRoomListening != null) {
            chatRoomListening.remove();
            Log.e(TAG, "listeningChatRoom: remove");
        }
        if (chatRoom == null || chatRoom.getChatRoomId() == null) return;
        chatRoomListening = db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && !value.exists()) {
                        updateChatRoom.onUpdate(null);
                        return;
                    }
                    if (value == null) return;
                    ChatRoom chatRoom1 = value.toObject(ChatRoom.class);
                    if (chatRoom1 != null) {
                        updateChatRoom.onUpdate(chatRoom1);
                    }
                });
    }

    public void unregisterListeningChatRoom() {
        if (chatRoomListening != null) {
            chatRoomListening.remove();
            chatRoomListening = null;
        }
    }

    public void deleteOneSide(ChatRoom model, List<String> userIds) {
        if (currentUserId == null || model == null || userIds == null) return;
        userIds.remove(currentUserId);
        userIds.add(currentUserId + "_delete");
        db.collection("chatroom").document(model.getChatRoomId())
                .update("userId", userIds)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Long> deleteChat = model.getDeleteChat();
                        if (deleteChat == null) deleteChat = new HashMap<>();
                        deleteChat.put(currentUserId, Timestamp.now().toDate().getTime());
                        db.collection("chatroom").document(model.getChatRoomId())
                                .update("deleteChat", deleteChat)
                                .addOnCompleteListener(task12 -> {
                                    if (task12.isSuccessful()) {
                                        Log.e(TAG, "Cập nhật thời gian xóa thành công");
                                    } else {
                                        Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task12.getException()).getMessage()));
                                    }
                                });
                    } else {
                        Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
                    }
                });
    }

    public void deleteTwoSides(ChatRoom model) {
        if (model == null || model.getChatRoomId() == null) return;
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
        } else {
            db.collection("chatroom").document(model.getChatRoomId()).delete().addOnCompleteListener(task2 -> {
                if (task2.isSuccessful()) {
                    Log.e(TAG, "Xóa thành công");
                } else {
                    Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task2.getException()).getMessage()));
                }
            });
        }
    }

    public LiveData<Map<String, String>> listeningReaction(ChatRoom chatRoom, String idMess) {
        MutableLiveData<Map<String, String>> mapMutableLiveData = new MutableLiveData<>();

        if (reactionListening != null) {
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

    public void unregisterListeningReaction() {
        if (reactionListening != null) {
            Log.e(TAG, "unregisterListeningReaction: ");
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

    // ==================== Recent Chat logic ====================

    public LiveData<Query> getQuery() {
        MutableLiveData<Query> queryMutableLiveData = new MutableLiveData<>();
        if (currentUserId == null) {
            queryMutableLiveData.setValue(null);
        } else {
            Query query = db.collection("chatroom")
                    .whereArrayContains("userId", currentUserId)
                    .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING);
            queryMutableLiveData.setValue(query);
        }
        return queryMutableLiveData;
    }

    public void showInfo(String otherUserId, OnShowInfo onShowInfo) {
        if (otherUserId == null || onShowInfo == null) return;
        db.collection("users").document(otherUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) return;
            User otherUser = documentSnapshot.toObject(User.class);
            if (otherUser == null) return;
            Log.e(TAG, otherUser.toString());
            onShowInfo.onShow(otherUser);
        });
    }

    public void getIsNotSeen(ChatRoom model, OnSeenChatRoom onSeenChatRoom) {
        if (model == null || onSeenChatRoom == null) return;
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId != null && model.getUnreadCounts() != null && model.getUnreadCounts().containsKey(myId)) {
            Long unread = model.getUnreadCounts().get(myId);
            onSeenChatRoom.onSeen(unread != null && unread > 0);
            return;
        }

        db.collection("chatroom").document(model.getChatRoomId())
                .collection("chats")
                .whereNotEqualTo("senderId", FirebaseAuth.getInstance().getUid())
                .whereEqualTo("seen", false)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        onSeenChatRoom.onSeen(true);
                    } else {
                        onSeenChatRoom.onSeen(false);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, Objects.requireNonNull(e.getMessage())));
    }

    public void searchChatRoom(OnSearchChatRoom onSearchChatRoom) {
        if (currentUserId == null || onSearchChatRoom == null) return;
        db.collection("chatroom")
                .whereArrayContains("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<ChatRoom> listChat = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : task.getResult()) {
                            listChat.add(documentSnapshot.toObject(ChatRoom.class));
                            Log.e(TAG, "searchChatRoom: " + documentSnapshot);
                        }
                        onSearchChatRoom.onSearch(listChat);
                    } else {
                        onSearchChatRoom.onSearch(null);
                        String error = task.getException() != null ? task.getException().getMessage() : "unknown";
                        Log.e(TAG, "searchChatRoom: loi " + error);
                    }
                });
    }

    public Task<QuerySnapshot> getActiveUsers(int limit) {
        return db.collection("users")
                .whereEqualTo("active", true)
                .limit(limit)
                .get();
    }

    public Task<QuerySnapshot> getRandomUsers(String randomKey, int limit) {
        return db.collection("users")
                .whereGreaterThanOrEqualTo(FieldPath.documentId(), randomKey)
                .limit(limit)
                .get();
    }

    public Task<QuerySnapshot> getNumberUser(int number) {
        return db.collection("users")
                .limit(number)
                .get();
    }

    public String createChatRoomId() {
        return db.collection("chatroom")
                .document()
                .getId();
    }

    public Task<Void> createChatRoom(ChatRoom chatRoom) {
        return db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .set(chatRoom);
    }

    public void scheduleDeleteRandomChat(Context context, String chatRoomId) {
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

    // ==================== Group & Info & Alias Chat logic ====================

    public void setChatRoomToFireStore(ChatRoom chatRoom, OnStateListener onStateListener) {
        DocumentReference docRef = db.collection("chatroom").document();
        chatRoom.setChatRoomId(docRef.getId());
        docRef.set(chatRoom).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                onStateListener.onSuccess("Tạo nhóm thành công");
            } else {
                Exception exception = task.getException();
                if (exception != null) {
                    onStateListener.onFailure(exception.getMessage());
                }
            }
        });
    }

    public Task<Void> updateAlias(ChatRoom chatRoom, String idUser, String newAlias) {
        return db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .update("alias." + idUser, newAlias);
    }

    public Task<Void> updateChatGroupName(String chatroomId, String name) {
        return db.collection("chatroom")
                .document(chatroomId)
                .update("chatName", name);
    }

    public void upload_avt(Uri uri, ChatRoom chatRoom, OnStateListener onStateListener) {
        if (uri == null) {
            if (onStateListener != null) onStateListener.onFailure("Ảnh không tồn tại");
            return;
        }
        MediaManager.get().upload(uri).unsigned("post_upload").option("folder", "avatar/").callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {}

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                if (imageUrl != null) {
                    updateChatRoomToFirebase(imageUrl, chatRoom, onStateListener);
                }
            }

            @Override
            public void onError(String requestId, ErrorInfo errorInfo) {
                if (onStateListener != null) onStateListener.onFailure(errorInfo.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void updateChatRoomToFirebase(String avtUrl, ChatRoom chatRoom, OnStateListener onStateListener) {
        db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .update("chatRoomAvt", avtUrl)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (onStateListener != null) onStateListener.onSuccess("Cập nhật avatar thành công");
                    } else {
                        Exception exception = task.getException();
                        if (exception != null && onStateListener != null) {
                            onStateListener.onFailure(exception.getMessage());
                        }
                    }
                });
    }

    public Task<Void> addMemberToGroup(ChatRoom chatRoom) {
        return db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .set(chatRoom);
    }

    public Task<Void> leaveChat(ChatRoom chatRoom) {
        return db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .set(chatRoom);
    }

    // ==================== Background Chat logic ====================

    public Task<QuerySnapshot> getListImageBackGround() {
        return db.collection("background_chat").get();
    }

    public Task<Void> updateBackGround(String chatId, String background) {
        return db.collection("chatroom")
                .document(chatId)
                .update("background", background);
    }
}
