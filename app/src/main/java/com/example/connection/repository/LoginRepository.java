package com.example.connection.repository;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;
import com.example.connection.model.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class LoginRepository {

    private final FirebaseAuth mAuth;
    private final OnSignIn onSignIn;

    public interface OnSendOtp{
        void onCheckPhone(boolean checkPhone);
        void onFailure(String error);
        void onCodeSent(String codeSent);
        void onSdt(String mSdt);
        void onValidate(boolean validate);
    }

    public interface OnSignIn{
        void onSuccess(boolean success);
    }

    public LoginRepository (OnSignIn onSignIn){
        mAuth = FirebaseAuth.getInstance();
        this.onSignIn = onSignIn;
    }

    public void sendOTP(String mSdt, Activity activity, OnSendOtp onSendOtp){
        if(mSdt.isEmpty()) {
            onSendOtp.onCheckPhone(false);
            return;
        }
        onSendOtp.onCheckPhone(true);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(mSdt)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signInWithPhoneAuthCredential(phoneAuthCredential, mSdt);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            onSendOtp.onFailure("Số điện thoại không hợp lệ");
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            onSendOtp.onFailure("Quá nhiều thao tác");
                        } else if (e instanceof FirebaseAuthMissingActivityForRecaptchaException) {
                            onSendOtp.onFailure("MissingCaptcha");
                        }
                        else {
                            onSendOtp.onFailure(e.getMessage());
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        onSendOtp.onCodeSent(s);
                        onSendOtp.onSdt(mSdt);
                        onSendOtp.onValidate(true);
                        Log.e("LoginActivity", "onCodeSent: ");
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential, String mSdt) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uID = mAuth.getUid();
                        if(uID == null) return;
                        FirebaseFirestore.getInstance().collection("users").document(uID).get().addOnCompleteListener(task12 -> {
                            if(task12.isSuccessful()){
                                User user = task12.getResult().toObject(User.class);
                                if(user == null){
                                    List<String> mList = new ArrayList<>();
                                    mList.add(uID);
                                    user = new User(uID, null, null, null, null, mSdt, mList, mList, true);
                                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                                    db.collection("users").document(Objects.requireNonNull(uID)).set(user).addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.e("Phone", "Save successful");
                                        } else {
                                            Log.e("Phone", Objects.requireNonNull(task1.getException()).toString());
                                        }
                                    });
                                }
                                onSignIn.onSuccess(true);
                            }
                        });

                    } else {
                        onSignIn.onSuccess(false);
                    }
                });
    }

}
