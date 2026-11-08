package com.example.connection.ui.viewmodel;

import android.app.Activity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.ui.state.OnStateListener;
import com.example.connection.repository.AuthRepository;

public class SignInViewModel extends ViewModel implements OnStateListener {

    private final AuthRepository signInRepository;
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isEmptyInfo = new MutableLiveData<>();

    public SignInViewModel() {
        signInRepository = new AuthRepository(this);
    }

    public void getSignInWithEmailAndPassword(String email, String password, Activity activity) {
        if (email.isEmpty() || password.isEmpty()) {
            isEmptyInfo.setValue(true);
            return;
        }
        isEmptyInfo.setValue(false);
        signInRepository.getSignInWithEmailAndPassword(email, password, activity);
    }

    public void sendPasswordReset(String email) {
        signInRepository.sendPasswordReset(email);
    }

    public LiveData<String> getSuccessLiveData() {
        return successLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Boolean> getIsEmptyInfo() {
        return isEmptyInfo;
    }

    @Override
    public void onSuccess(String success) {
        successLiveData.setValue(success);
    }

    @Override
    public void onFailure(String error) {
        errorLiveData.setValue(error);
    }

    @Override
    public void onLoading(boolean isLoading) {

    }
}
