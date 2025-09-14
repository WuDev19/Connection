package com.example.connection.repository;

import android.content.Context;
import com.example.connection.listener.OnStateListener;
import com.example.connection.utils.SharePreUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordRepository {

    private final OnStateListener onStateListener;

    public ChangePasswordRepository(OnStateListener onStateListener){
        this.onStateListener = onStateListener;
    }

    public String getPassword(Context context){
        return SharePreUtil.getInstance(context.getApplicationContext()).getPassword();
    }

    public void setPassword(Context context, String newPassword){
        SharePreUtil.getInstance(context.getApplicationContext()).setNewPassword(newPassword);
    }

    public FirebaseUser getUser(){
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void updatePassword(FirebaseUser user, String newPasswordConfirm){
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
    }

}
