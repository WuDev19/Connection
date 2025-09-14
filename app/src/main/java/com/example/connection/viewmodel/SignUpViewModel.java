package com.example.connection.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.repository.SignUpRepository;

public class SignUpViewModel extends ViewModel {

    private final SignUpRepository signUpRepository;
    private final MutableLiveData<Boolean> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> getEmailAndPassword = new MutableLiveData<>();
    private final MutableLiveData<String> confirmInfoLiveData = new MutableLiveData<>();

    public SignUpViewModel(){
        signUpRepository = new SignUpRepository();
    }

    public void signUp(String email, String password, String passwordConfirm, String username){
        if(password.compareTo(passwordConfirm) != 0){
            confirmInfoLiveData.setValue("distinct mk");
            return;
        }
        if (email.isEmpty() || passwordConfirm.isEmpty() || password.isEmpty()){
            confirmInfoLiveData.setValue("lack info");
            return;
        }
        confirmInfoLiveData.setValue("confirm");
        signUpRepository.signUp(email, passwordConfirm, username, new SignUpRepository.OnSignUpListener() {
            @Override
            public void onSuccess(boolean success) {
                successLiveData.setValue(success);
            }

            @Override
            public void onEmailAndPassword(String emailAndPassword) {
                getEmailAndPassword.setValue(emailAndPassword);
            }
        });
    }

    public LiveData<Boolean> getSignUpSuccess(){
        return successLiveData;
    }

    public LiveData<String> getEmailAndPassword(){
        return getEmailAndPassword;
    }

    public LiveData<String> getConfirmInfoLiveData() {
        return confirmInfoLiveData;
    }
}
