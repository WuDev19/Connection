package com.example.connection.repository;

import android.content.Context;
import com.example.connection.listener.OnStateListener;
import com.example.connection.utils.SharePreUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangeEmailRepository{

    private final OnStateListener onStateListener;

    public ChangeEmailRepository (OnStateListener onStateListener){
        this.onStateListener = onStateListener;
    }

    public String getPassword (Context context){
        return SharePreUtil.getInstance(context.getApplicationContext()).getPassword();
    }

    public FirebaseUser getUser(){
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void verifyChange(FirebaseUser user, String newEmail){
        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                onStateListener.onSuccess("Vui lòng kiểm tra email và đăng nhập lại để xác nhận thay đổi");
            } else {
                Exception exception = task.getException();
                if(exception != null){
                    onStateListener.onFailure(exception.getMessage());
                }
            }
        });
    }
}
