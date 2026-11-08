package com.example.connection.ui.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.ui.state.OnStateListener;
import com.example.connection.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

public class ChangeEmailViewModel extends ViewModel implements OnStateListener {

    private final UserRepository changeEmailRepository;
    private final MutableLiveData<String> verifySuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> checkPassWordLiveData = new MutableLiveData<>();

    public ChangeEmailViewModel() {
        changeEmailRepository = new UserRepository(this);
    }

    public void verifyChange(String newEmail){
        FirebaseUser user = changeEmailRepository.getUser();
        if(user == null) {
            errorLiveData.setValue("Mạng có vẻ kém, vui lòng kiểm tra lại mạng");
            return;
        }
        changeEmailRepository.verifyChange(user, newEmail);
    }

    public void checkPassword(String newEmail, String password, Context context){
        if(newEmail == null || newEmail.isEmpty()){
            errorLiveData.setValue("Vui lòng nhập email");
            return;
        }
        if(password == null || password.isEmpty()){
            errorLiveData.setValue("Vui lòng nhập mật khẩu");
            return;
        }
        FirebaseUser user = changeEmailRepository.getUser();
        if(user == null) {
            errorLiveData.setValue("Mạng có vẻ kém, vui lòng kiểm tra lại mạng");
            return;
        }
        changeEmailRepository.reauthenticate(user, password, () -> checkPassWordLiveData.setValue(newEmail));
    }

    public LiveData<String> getVerifySuccess() {
        return verifySuccess;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<String> getCheckPassWordLiveData() {
        return checkPassWordLiveData;
    }

    @Override
    public void onSuccess(String success) {
        verifySuccess.setValue(success);
    }

    @Override
    public void onFailure(String failure) {
        errorLiveData.setValue(failure);
    }

    @Override
    public void onLoading(boolean isLoading) {
    }
}
