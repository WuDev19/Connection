package com.example.connection.repository.impl;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.connection.constants.SharePreUtil;
import com.example.connection.data.worker.FetchMusicFireStore;
import com.example.connection.model.User;
import com.example.connection.repository.base.AuthRepository;
import com.example.connection.ui.state.OnStateListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class AuthRepositoryImpl implements AuthRepository {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final String TAG = getClass().getName();

    private OnStateListener onStateListener;
    private OnSignIn onSignIn;
    private OnLoginWithOtp onLoginWithOtp;
    private Application application;

    private PhoneAuthProvider.ForceResendingToken mForceResendingToken;

    public AuthRepositoryImpl() {
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public AuthRepositoryImpl(OnStateListener onStateListener) {
        this();
        this.onStateListener = onStateListener;
    }

    public AuthRepositoryImpl(OnSignIn onSignIn) {
        this();
        this.onSignIn = onSignIn;
    }

    public AuthRepositoryImpl(OnLoginWithOtp onLoginWithOtp) {
        this();
        this.onLoginWithOtp = onLoginWithOtp;
    }

    public AuthRepositoryImpl(Application application) {
        this();
        this.application = application;
    }

    @Override
    public void getSignInWithEmailAndPassword(String email, String password, Activity activity) {
        getSignInWithEmailAndPassword(email, password, activity, this.onStateListener);
    }

    @Override
    public void getSignInWithEmailAndPassword(String email, String password, Activity activity, OnStateListener listener) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(activity, task -> {
            if (task.isSuccessful()) {
                Log.e(TAG, "getSignInWithEmailAndPassword: success");
                if (listener != null) listener.onSuccess("Success");
            } else {
                Log.e(TAG, "getSignInWithEmailAndPassword: fail");
                Exception exception = task.getException();
                if (exception != null && listener != null) {
                    if (exception instanceof FirebaseNetworkException) {
                        listener.onFailure("Không có kết nối mạng");
                    } else {
                        listener.onFailure("Email hoặc mật khẩu không đúng");
                    }
                }
            }
        });
    }

    @Override
    public void sendPasswordReset(String email) {
        sendPasswordReset(email, this.onStateListener);
    }

    @Override
    public void sendPasswordReset(String email, OnStateListener listener) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (listener != null) listener.onSuccess("Vui lòng kiểm tra email để xác nhận");
            } else {
                Exception exception = task.getException();
                if (exception != null && listener != null) {
                    if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                        listener.onFailure("Email không hợp lệ");
                    } else if (exception instanceof FirebaseAuthInvalidUserException) {
                        listener.onFailure("Tài khoản đang bị vô hiệu hóa, hãy liên hệ với admin");
                    } else if (exception instanceof FirebaseTooManyRequestsException) {
                        listener.onFailure("Quá nhiều thao tác");
                    } else if (exception instanceof FirebaseNetworkException) {
                        listener.onFailure("Không có kết nối mạng");
                    } else {
                        listener.onFailure(exception.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public void signUp(String email, String passwordConfirm, String username, OnSignUpListener onSignUpListener) {
        mAuth.createUserWithEmailAndPassword(email, passwordConfirm)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uID = mAuth.getUid();
                        if (uID == null) {
                            if (onSignUpListener != null) onSignUpListener.onSuccess(false);
                            return;
                        }
                        List<String> mList = new ArrayList<>();
                        mList.add(uID);
                        User user = new User(uID, null, username, username.toLowerCase(), email, null, mList, mList, false);
                        db.collection("users").document(Objects.requireNonNull(uID)).set(user).addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                Log.e("SignUp", "Save successful");
                                if (onSignUpListener != null) {
                                    onSignUpListener.onSuccess(true);
                                    onSignUpListener.onEmailAndPassword(email + "_" + passwordConfirm);
                                }
                            } else {
                                FirebaseUser user1 = mAuth.getCurrentUser();
                                if (user1 != null) {
                                    user1.delete()
                                            .addOnCompleteListener(deleteTask -> {
                                                if (!deleteTask.isSuccessful()) {
                                                    Log.e("SignUp", "Rollback Auth fail", deleteTask.getException());
                                                }
                                                if (onSignUpListener != null) onSignUpListener.onSuccess(false);
                                            });
                                }
                            }
                        });
                    } else {
                        if (onSignUpListener != null) onSignUpListener.onSuccess(false);
                    }
                });
    }

    @Override
    public void sendOTP(String mSdt, Activity activity, OnSendOtp onSendOtp) {
        if (mSdt.isEmpty()) {
            if (onSendOtp != null) onSendOtp.onCheckPhone(false);
            return;
        }
        if (onSendOtp != null) onSendOtp.onCheckPhone(true);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(mSdt)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                        signInWithPhoneAuthCredential(phoneAuthCredential, mSdt, onSignIn);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        if (onSendOtp == null) return;
                        if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            onSendOtp.onFailure("Số điện thoại không hợp lệ");
                        } else if (e instanceof FirebaseTooManyRequestsException) {
                            onSendOtp.onFailure("Quá nhiều thao tác");
                        } else if (e instanceof FirebaseAuthMissingActivityForRecaptchaException) {
                            onSendOtp.onFailure("MissingCaptcha");
                        } else {
                            onSendOtp.onFailure(e.getMessage());
                        }
                    }

                    @Override
                    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                        super.onCodeSent(s, forceResendingToken);
                        if (onSendOtp != null) {
                            onSendOtp.onCodeSent(s);
                            onSendOtp.onSdt(mSdt);
                            onSendOtp.onValidate(true);
                        }
                        Log.e("LoginActivity", "onCodeSent: ");
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    @Override
    public void loginWithOTP(String idVerify, String otp, String sdt) {
        loginWithOTP(idVerify, otp, sdt, this.onLoginWithOtp);
    }

    @Override
    public void loginWithOTP(String idVerify, String otp, String sdt, OnLoginWithOtp callback) {
        if (otp.isEmpty()) {
            if (callback != null) callback.onCheckOTP(false);
            return;
        }
        if (callback != null) callback.onCheckOTP(true);
        PhoneAuthCredential phoneAuthCredential = PhoneAuthProvider.getCredential(idVerify, otp);
        signInWithPhoneAuthCredential(phoneAuthCredential, sdt, success -> {
            if (callback != null) callback.onSuccess(success);
        });
    }

    @Override
    public void resendOTP(String mSdt, Activity activity) {
        resendOTP(mSdt, activity, this.onLoginWithOtp);
    }

    @Override
    public void resendOTP(String mSdt, Activity activity, OnLoginWithOtp callback) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(mSdt)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setForceResendingToken(mForceResendingToken)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                if (callback != null) callback.onValidate(true);
                                signInWithPhoneAuthCredential(phoneAuthCredential, mSdt, success -> {
                                    if (callback != null) callback.onSuccess(success);
                                });
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                if (callback != null) callback.onValidate(false);
                            }

                            @Override
                            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                super.onCodeSent(s, forceResendingToken);
                                if (callback != null) {
                                    callback.onVerify(s);
                                    callback.onCodeSent(true);
                                }
                                Log.e("OTP", "onCodeSent: ");
                                mForceResendingToken = forceResendingToken;
                            }
                        })
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential, String mSdt, OnSignIn callback) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uID = mAuth.getUid();
                        if (uID == null) return;
                        db.collection("users").document(uID).get().addOnCompleteListener(task12 -> {
                            if (task12.isSuccessful()) {
                                User user = task12.getResult().toObject(User.class);
                                if (user == null) {
                                    List<String> mList = new ArrayList<>();
                                    mList.add(uID);
                                    user = new User(uID, null, null, null, null, mSdt, mList, mList, true);
                                    db.collection("users").document(Objects.requireNonNull(uID)).set(user).addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.e("Phone", "Save successful");
                                        } else {
                                            Log.e("Phone", Objects.requireNonNull(task1.getException()).toString());
                                        }
                                    });
                                }
                                if (callback != null) callback.onSuccess(true);
                            }
                        });
                    } else {
                        if (callback != null) callback.onSuccess(false);
                    }
                });
    }

    @Override
    public void fetchMusicFireStore() {
        if (application != null) {
            fetchMusicFireStore(application.getApplicationContext());
        }
    }

    @Override
    public void fetchMusicFireStore(Context context) {
        if (SharePreUtil.getInstance(context).firstUseApp()) {
            OneTimeWorkRequest fetchMusicSchedule = new OneTimeWorkRequest.Builder(FetchMusicFireStore.class)
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .build();
            WorkManager.getInstance(context).enqueueUniqueWork("fetchMusic", ExistingWorkPolicy.KEEP, fetchMusicSchedule);
        }
    }

    @Override
    public LiveData<FirebaseUser> checkUser() {
        MutableLiveData<FirebaseUser> userMutableLiveData = new MutableLiveData<>();
        FirebaseUser user = mAuth.getCurrentUser();
        userMutableLiveData.setValue(user);
        return userMutableLiveData;
    }

    @Override
    public Task<QuerySnapshot> getNumberCurrentUser() {
        return db.collection("users").get();
    }
}
