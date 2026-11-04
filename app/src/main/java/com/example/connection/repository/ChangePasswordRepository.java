package com.example.connection.repository;

import com.example.connection.ui.state.OnStateListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordRepository {

    private final OnStateListener onStateListener;

    public ChangePasswordRepository(OnStateListener onStateListener){
        this.onStateListener = onStateListener;
    }

    public FirebaseUser getUser(){
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void updatePassword(FirebaseUser user, String oldPassword, String newPasswordConfirm){
        if (user == null || user.getEmail() == null) {
            onStateListener.onFailure("Không tìm thấy thông tin tài khoản");
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (reauthTask.isSuccessful()) {
                user.updatePassword(newPasswordConfirm).addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        onStateListener.onSuccess(newPasswordConfirm);
                    }
                    else{
                        Exception exception = task.getException();
                        if(exception != null){
                            onStateListener.onFailure(exception.getMessage());
                        }
                    }
                });
            } else {
                onStateListener.onFailure("Mật khẩu cũ không chính xác");
            }
        });
    }

}
