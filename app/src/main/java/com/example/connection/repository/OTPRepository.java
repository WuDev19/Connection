package com.example.connection.repository;

import android.app.Activity;
import android.util.Log;
import androidx.annotation.NonNull;
import com.example.connection.model.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class OTPRepository {

    private final FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken mForceResendingToken;
    private final OnLoginWithOtp onLoginWithOtp;

    public OTPRepository(OnLoginWithOtp onLoginWithOtp) {
        mAuth = FirebaseAuth.getInstance();
        this.onLoginWithOtp = onLoginWithOtp;
    }

    public interface OnLoginWithOtp{
        void onCheckOTP(boolean checkOTP);
        void onSuccess(boolean success);
        void onValidate(boolean validate);
        void onCodeSent(boolean codeSent);
        void onVerify(String verify);
    }

    public void loginWithOTP(String idVerify, String otp, String sdt) {
        if (otp.isEmpty()) {
            onLoginWithOtp.onCheckOTP(false);
            return;
        }
        onLoginWithOtp.onCheckOTP(true);
        PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(idVerify, otp);
        signInWithPhoneAuthCredential(phoneAuthCredential, sdt);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential, String mSdt) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uID = mAuth.getUid();
                        if (uID == null) return;
                        FirebaseFirestore.getInstance().collection("users").document(uID).get().addOnCompleteListener(task12 -> {
                            if (task12.isSuccessful()) {
                                User user = task12.getResult().toObject(User.class);
                                if (user == null) {
                                    List<String> mList = new ArrayList<>();
                                    mList.add(uID);
                                    user = new User(uID, null, null, null, mSdt, mList, mList, true);
                                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                                    db.collection("users").document(Objects.requireNonNull(uID)).set(user).addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.e("OTP", "Save successful");
                                        } else {
                                            Log.e("OTP", Objects.requireNonNull(task1.getException()).toString());
                                        }
                                    });
                                }
                                onLoginWithOtp.onSuccess(true);
                            }
                        });
                    } else {
                        onLoginWithOtp.onSuccess(false);
                    }
                });
    }

    public void resendOTP(String mSdt, Activity activity) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(mSdt)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setForceResendingToken(mForceResendingToken)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                onLoginWithOtp.onValidate(true);
                                signInWithPhoneAuthCredential(phoneAuthCredential, mSdt);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                onLoginWithOtp.onValidate(false);
                            }

                            @Override
                            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                super.onCodeSent(s, forceResendingToken);
                                onLoginWithOtp.onVerify(s);
                                onLoginWithOtp.onCodeSent(true);
                                Log.e("OTP", "onCodeSent: ");
                                mForceResendingToken = forceResendingToken;
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
}
