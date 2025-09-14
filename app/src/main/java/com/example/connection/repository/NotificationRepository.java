package com.example.connection.repository;

import android.util.Log;
import com.example.connection.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.List;

public class NotificationRepository {

    private final String TAG = getClass().getName();
    private final FirebaseFirestore db;

    public NotificationRepository(){
        db = FirebaseFirestore.getInstance();
    }

    public Task<DocumentSnapshot> getUser(String id){
        if(id == null) return null;
        return db.collection("users").document(id)
                .get();
    }

    public interface HandleFriendNotification{
        void onSuccess(String success);
        void onFailure(String error);
    }

    public String getUserId(){
        return FirebaseAuth.getInstance().getUid();
    }

    public void acceptFriend(String otherUserId, String currentUserId, User user, HandleFriendNotification handleFriend){
        WriteBatch writeBatch = db.batch();
        writeBatch.set(db.collection("users").document(currentUserId), user);
        db.collection("users").document(otherUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User otherUser = task.getResult().toObject(User.class);
                if (otherUser == null) return;
                List<String> friend = otherUser.getFriend();
                friend.add(currentUserId);
                otherUser.setFriend(friend);
                writeBatch.set(db.collection("users").document(otherUserId), otherUser);
                writeBatch.commit().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Log.e(TAG, "onComplete: accept thanh cong");
                        Log.e("kiemtrasuccess", "acceptFriend: ");
                        handleFriend.onSuccess("Kết bạn thành công");
                    } else {
                        Exception exception = task1.getException();
                        if (exception != null) {
                            handleFriend.onFailure(exception.getMessage());
                        }
                    }
                });
            }
            else {
                Exception exception = task.getException();
                if(exception != null){
                    handleFriend.onFailure(exception.getMessage());
                }
            }
        });
    }

    public void declineFriend(String currentUserId, User user, HandleFriendNotification handleFriendNotifacation){
        db.collection("users").document(currentUserId).set(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.e(TAG, user.toString());
            } else {
                Exception exception = task.getException();
                if(exception != null){
                    handleFriendNotifacation.onFailure(exception.getMessage());
                }
            }
        });
    }

}
