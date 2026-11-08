package com.example.connection.repository.base;

import android.app.Activity;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.connection.ui.state.OnStateListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QuerySnapshot;

public interface AuthRepository {

    interface OnSignUpListener {
        void onSuccess(boolean success);
        void onEmailAndPassword(String emailAndPassword);
    }

    interface OnSendOtp {
        void onCheckPhone(boolean checkPhone);
        void onFailure(String error);
        void onCodeSent(String codeSent);
        void onSdt(String mSdt);
        void onValidate(boolean validate);
    }

    interface OnSignIn {
        void onSuccess(boolean success);
    }

    interface OnLoginWithOtp {
        void onCheckOTP(boolean checkOTP);
        void onSuccess(boolean success);
        void onValidate(boolean validate);
        void onCodeSent(boolean codeSent);
        void onVerify(String verify);
    }

    void getSignInWithEmailAndPassword(String email, String password, Activity activity);

    void getSignInWithEmailAndPassword(String email, String password, Activity activity, OnStateListener listener);

    void sendPasswordReset(String email);

    void sendPasswordReset(String email, OnStateListener listener);

    void signUp(String email, String passwordConfirm, String username, OnSignUpListener onSignUpListener);

    void sendOTP(String mSdt, Activity activity, OnSendOtp onSendOtp);

    void loginWithOTP(String idVerify, String otp, String sdt);

    void loginWithOTP(String idVerify, String otp, String sdt, OnLoginWithOtp callback);

    void resendOTP(String mSdt, Activity activity);

    void resendOTP(String mSdt, Activity activity, OnLoginWithOtp callback);

    void fetchMusicFireStore();

    void fetchMusicFireStore(Context context);

    LiveData<FirebaseUser> checkUser();

    Task<QuerySnapshot> getNumberCurrentUser();
}
