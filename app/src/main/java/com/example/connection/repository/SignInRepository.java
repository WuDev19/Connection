package com.example.connection.repository;

import android.app.Activity;
import android.util.Log;
import com.example.connection.listener.OnStateListener;
import com.example.connection.utils.SharePreUtil;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class SignInRepository {

    private final FirebaseAuth mAuth;
    private final String TAG = getClass().getName();
    private final OnStateListener onStateListener;

    public SignInRepository(OnStateListener onStateListener) {
        mAuth = FirebaseAuth.getInstance();
        this.onStateListener = onStateListener;
    }

    public void getSignInWithEmailAndPassword(String email, String password, Activity activity) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(activity, task -> {
            if (task.isSuccessful()) {
                SharePreUtil.getInstance(activity.getApplicationContext()).writeEmailAndPassword(password, email);
                Log.e(TAG, "getSignInWithEmailAndPassword: success");
                onStateListener.onSuccess("Success");
            } else {
                Log.e(TAG, "getSignInWithEmailAndPassword: fail");
                Exception exception = task.getException();
                if (exception != null) {
                    if (exception instanceof FirebaseNetworkException) {
                        onStateListener.onFailure("Không có kết nối mạng");
                    } else {
                        onStateListener.onFailure("Email hoặc mật khẩu không đúng");
                    }
                }
            }
        });
    }

    public void sendPasswordReset(String email) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                onStateListener.onSuccess("Vui lòng kiểm tra email để xác nhận");
            } else {
                Exception exception = task.getException();
                if (exception != null) {
                    if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                        onStateListener.onFailure("Email không hợp lệ");
                    } else if (exception instanceof FirebaseAuthInvalidUserException) {
                        onStateListener.onFailure("Tài khoản đang bị vô hiệu hóa, hãy liên hệ với admin");
                    } else if (exception instanceof FirebaseTooManyRequestsException) {
                        onStateListener.onFailure("Quá nhiều thao tác");
                    } else if (exception instanceof FirebaseNetworkException) {
                        onStateListener.onFailure("Không có kết nối mạng");
                    } else {
                        onStateListener.onFailure(exception.getMessage());
                    }
                }
            }
        });
    }

}
