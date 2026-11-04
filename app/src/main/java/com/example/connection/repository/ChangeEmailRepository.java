package com.example.connection.repository;

import com.example.connection.ui.state.OnStateListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangeEmailRepository {

    private final OnStateListener onStateListener;

    public ChangeEmailRepository(OnStateListener onStateListener) {
        this.onStateListener = onStateListener;
    }

    public interface OnReauthCallback {
        void onSuccess();
    }

    public void reauthenticate(FirebaseUser user, String password, OnReauthCallback callback) {
        if (user == null || user.getEmail() == null) {
            onStateListener.onFailure("Không tìm thấy thông tin tài khoản");
            return;
        }
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                onStateListener.onFailure("Mật khẩu không chính xác");
            }
        });
    }

    public FirebaseUser getUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void verifyChange(FirebaseUser user, String newEmail) {
        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                onStateListener.onSuccess("Vui lòng kiểm tra email và đăng nhập lại để xác nhận thay đổi");
            } else {
                Exception exception = task.getException();
                if (exception != null) {
                    onStateListener.onFailure(exception.getMessage());
                }
            }
        });
    }
}
