package com.example.connection.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.listener.OnStateListener;
import com.example.connection.repository.ChangeEmailRepository;
import com.google.firebase.auth.FirebaseUser;

public class ChangeEmailViewModel extends ViewModel implements OnStateListener {

    private final ChangeEmailRepository changeEmailRepository;
    private final MutableLiveData<String> verifySuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> checkPassWordLiveData = new MutableLiveData<>();

    public ChangeEmailViewModel() {
        changeEmailRepository = new ChangeEmailRepository(this);
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
        String mPassword = changeEmailRepository.getPassword(context);
        if(password == null) return;
        if(password.compareTo(mPassword) != 0 || newEmail.isEmpty()){
            if(password.compareTo(mPassword) != 0){
                errorLiveData.setValue("Mật khẩu không chính xác");
            }
            if(newEmail.isEmpty()){
                errorLiveData.setValue("Vui lòng nhập email");
            }
            return;
        }
        checkPassWordLiveData.setValue(newEmail);
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
    public void onFailure(String error) {
        errorLiveData.setValue(error);
    }

    @Override
    public void onLoading(boolean isLoading) {

    }
}
