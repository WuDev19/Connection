package com.example.connection.repository;

import android.util.Log;
import com.example.connection.listener.OnStateListener;
import com.example.connection.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import java.util.List;
import java.util.Objects;

public class BottomSheetRepository {

    private final String currentUserId;
    private final CollectionReference collectionUser;
    private final String TAG = getClass().getName();
    private final OnStateListener onStateListener;

    public BottomSheetRepository(OnStateListener onStateListener) {
        currentUserId = FirebaseAuth.getInstance().getUid();
        collectionUser = FirebaseFirestore.getInstance().collection("users");
        this.onStateListener = onStateListener;
    }

    public Task<DocumentSnapshot> getUserFromFirebase() {
        if (currentUserId == null) return null;
        return collectionUser.document(currentUserId).get();
    }

    public void addFriend(String otherUserId, User currentUser) {
        WriteBatch writeBatch = FirebaseFirestore.getInstance().batch();
        writeBatch.set(collectionUser.document(currentUserId), currentUser);
        collectionUser.document(otherUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User otherUser = documentSnapshot.toObject(User.class);
                if (otherUser != null) {
                    List<String> friendOtherUser = otherUser.getFriend();
                    friendOtherUser.add(currentUserId);
                    writeBatch.update(collectionUser.document(otherUserId), "friend", friendOtherUser);
                    writeBatch.commit().addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
                        } else {
                            onStateListener.onSuccess("Kết bạn thành công");
                        }
                    });
                }
            }
        });
    }

    public void sendFriendRequest(String otherUserId) {
        collectionUser.document(otherUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User otherUser = task.getResult().toObject(User.class);
                if (otherUser != null) {
                    List<String> friendConfirmOtherUser = otherUser.getFriendConfirm();
                    List<String> friendOtherUser = otherUser.getFriend();
                    if (friendOtherUser == null || friendConfirmOtherUser == null) {
                        Log.e(TAG, "sendFriendRequest: ko tim thay danh sach ban be cua otheruser");
                        return;
                    }
                    if (friendConfirmOtherUser.contains(currentUserId)) {
                        onStateListener.onSuccess("Bạn đã gửi kết bạn đến người dùng này");
                        return;
                    } else if (friendOtherUser.contains(currentUserId)) {
                        onStateListener.onSuccess("Hai người đã là bạn bè");
                        return;
                    }
                    friendConfirmOtherUser.add(currentUserId);
                    otherUser.setFriendConfirm(friendConfirmOtherUser);
                    collectionUser.document(otherUserId)
                            .set(otherUser).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    onStateListener.onSuccess("Đã gửi lời mời kết bạn thành công");
                                } else {
                                    Exception exception = task1.getException();
                                    if (exception != null) {
                                        onStateListener.onFailure(exception.getMessage());
                                    }
                                }
                            });
                }
            } else {
                Exception exception = task.getException();
                if (exception != null) {
                    onStateListener.onFailure(exception.getMessage());
                }
            }
        });
    }


    public void delete(String friendId, List<String> friendCurrentUser) {
        if (friendId == null || currentUserId == null) return;
        friendCurrentUser.remove(friendId);
        WriteBatch writeBatch = FirebaseFirestore.getInstance().batch();
        writeBatch.update(collectionUser.document(currentUserId), "friend", friendCurrentUser);
        collectionUser.document(friendId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User userOther = task.getResult().toObject(User.class);
                if (userOther == null) return;
                List<String> listFriendOtherUser = userOther.getFriend();
                listFriendOtherUser.remove(currentUserId);
                writeBatch.update(collectionUser.document(friendId), "friend", listFriendOtherUser);
                writeBatch.commit().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        onStateListener.onSuccess("Xóa thành công");
                    } else {
                        Exception exception = task1.getException();
                        if (exception != null) {
                            onStateListener.onFailure(exception.getMessage());
                        }
                    }
                });
            }
        });
    }

    public Query getQueryFriend() {
        if (currentUserId != null) {
            return collectionUser
                    .whereArrayContains("friend", currentUserId)
                    .orderBy("userName", Query.Direction.ASCENDING);
        }
        return null;
    }

    public Query getQuerySearch(String username) {
        if (currentUserId != null) {
            return collectionUser
                    .whereGreaterThanOrEqualTo("userNameLowerCase", username.toLowerCase())
                    .whereLessThanOrEqualTo("userNameLowerCase", username.toLowerCase() + "\uf8ff")
                    .orderBy("userNameLowerCase");
        }
        return null;
    }

}
