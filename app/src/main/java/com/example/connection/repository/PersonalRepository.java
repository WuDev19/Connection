package com.example.connection.repository;

import android.content.Context;
import android.util.Log;

import com.example.connection.ui.state.OnStateListener;
import com.example.connection.model.User;
import com.example.connection.constants.SharePreUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class PersonalRepository {

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;
    private final String TAG = getClass().getName();
    private final OnStateListener onStateListener;
    private ListenerRegistration userListening;

    public PersonalRepository(OnStateListener onStateListener){
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        this.onStateListener = onStateListener;
    }

    public interface OnShowUser{
        void showUser(User user);
    }

    public void showUserFromFirebase(OnShowUser onShowUser) {
        String id = auth.getUid();
        if(id == null) return;

        if(userListening != null){
            userListening.remove();
        }
        userListening = firestore.collection("users").document(id).addSnapshotListener((value, error) -> {
            if(value == null) return;
            if(error != null) return;
            User user = value.toObject(User.class);
            if(user != null){
                onShowUser.showUser(user);
            }
        });
    }

    public void unregisterUserListening(){
        if(userListening != null){
            userListening.remove();
        }
    }

    public void signOut(){
        auth.signOut();
    }

    public void updateNewEmailToFireStore(Context context){
        String newEmail = SharePreUtil.getInstance(context.getApplicationContext()).getEmail();
        String id = auth.getUid();
        if(id == null || newEmail == null) return;
        firestore.collection("users").document(id).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User user1 = task.getResult().toObject(User.class);
                if (user1 == null) return;
                String oldEmail = user1.getEmail();
                if (oldEmail == null) return;
                if (!oldEmail.equals(newEmail)) {
                    firestore.collection("users").document(id)
                            .update("email", newEmail)
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    Log.e(TAG, "Cập nhật email lên firestore thành công");
                                }
                            });
                }
            }
        });
    }

    public void deleteUser(){
        FirebaseUser user = auth.getCurrentUser();
        String id = auth.getUid();
        if(user == null || id == null) return;
        DocumentReference userReference = firestore.collection("users").document(id);
        userReference.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                User userFireStore = task.getResult().toObject(User.class);
                if(userFireStore != null){
                    userReference.delete().addOnCompleteListener(task1 -> {
                        if(task1.isSuccessful()){
                            user.delete().addOnCompleteListener(task11 -> {
                                if(task11.isSuccessful()){
                                    onStateListener.onSuccess("Đã xóa tài khoản");
                                }
                                else {
                                    userReference.set(userFireStore).addOnCompleteListener(task111 -> {
                                        if(task111.isSuccessful()){
                                            Log.e(TAG, "onComplete: rollback thanh cong");
                                        }
                                        else {
                                            Exception exception = task111.getException();
                                            if(exception != null){
                                                onStateListener.onFailure(exception.getMessage());
                                            }                                        }
                                    });
                                    if (task11.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                                        onStateListener.onFailure("Dang nhap lai");
                                    } else {
                                        Exception exception = task11.getException();
                                        if(exception != null){
                                            onStateListener.onFailure(exception.getMessage());
                                        }
                                    }
                                }
                            });
                        }
                        else {
                            Exception exception = task1.getException();
                            if(exception != null){
                                onStateListener.onFailure(exception.getMessage());
                            }
                        }
                    });
                }
            }
            else {
                Exception exception = task.getException();
                if(exception != null){
                    onStateListener.onFailure(exception.getMessage());
                }
            }
        });
    }
}
