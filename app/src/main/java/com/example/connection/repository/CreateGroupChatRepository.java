package com.example.connection.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.connection.listener.OnStateListener;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupChatRepository {

    private final FirebaseFirestore db;

    public CreateGroupChatRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<DocumentSnapshot> getUser() {
        String id = FirebaseAuth.getInstance().getUid();
        if (id == null) return null;
        return db.collection("users")
                .document(id)
                .get();
    }

    public Task<DocumentSnapshot> getUserFriend(String id) {
        return db.collection("users")
                .document(id)
                .get();
    }

    public LiveData<List<User>> searchUser(String username) {
        MutableLiveData<List<User>> userSearchLiveData = new MutableLiveData<>();
        db.collection("users")
                .whereGreaterThanOrEqualTo("userName", username)
                .whereLessThanOrEqualTo("userName", username + "\uf8ff")
                .orderBy("userName")
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

    public void setChatRoomToFireStore(ChatRoom chatRoom, OnStateListener onStateListener) {
        DocumentReference docRef = db.collection("chatroom")
                .document();
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

}
