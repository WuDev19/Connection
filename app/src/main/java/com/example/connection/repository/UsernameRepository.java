package com.example.connection.repository;

import com.example.connection.listener.OnStateListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UsernameRepository {

    private final FirebaseFirestore firestore;
    private final String currentUserId;

    public UsernameRepository() {
        firestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public Task<DocumentSnapshot> getCurrentUserName() {
        if (currentUserId != null) {
            return firestore.collection("users").document(currentUserId).get();
        }
        return null;
    }

    public void setUsername(String name, OnStateListener onStateListener) {
        firestore.collection("users").document(currentUserId).update("userName", name).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                onStateListener.onSuccess("setUserName");
            } else {
                Exception exception = task.getException();
                if(exception != null){
                    onStateListener.onFailure(exception.getMessage());
                }
            }
        });
    }

}

