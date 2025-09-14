package com.example.connection.viewmodel;

import android.app.Activity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.repository.LoginRepository;

public class LoginViewModel extends ViewModel {

    private final LoginRepository loginRepository;
    private final MutableLiveData<Boolean> checkPhoneLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> validateSms = new MutableLiveData<>();
    private final MutableLiveData<Boolean> signInSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> codeSentLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> sdtLiveData = new MutableLiveData<>();

    public LoginViewModel (){
        loginRepository = new LoginRepository(signInSuccess::setValue);
    }

    public void senOtp(String sdt, Activity activity){
        loginRepository.sendOTP(sdt, activity, new LoginRepository.OnSendOtp() {
            @Override
            public void onCheckPhone(boolean checkPhone) {
                checkPhoneLiveData.setValue(checkPhone);
            }

            @Override
            public void onFailure(String error) {
                errorLiveData.setValue(error);
            }

            @Override
            public void onCodeSent(String codeSent) {
                codeSentLiveData.setValue(codeSent);
            }

            @Override
            public void onSdt(String mSdt) {
                sdtLiveData.setValue(mSdt);
            }

            @Override
            public void onValidate(boolean validate) {
                validateSms.setValue(validate);
            }
        });
    }

    public LiveData<String> getError(){
        return errorLiveData;
    }

    public LiveData<Boolean> getValidateSms(){
        return validateSms;
    }

    public LiveData<Boolean> getSignInSuccess(){
        return signInSuccess;
    }

    public LiveData<String> getCodeSent(){
        return codeSentLiveData;
    }

    public LiveData<String> getSdt(){
        return sdtLiveData;
    }

    public LiveData<Boolean> getCheckPhone(){
        return checkPhoneLiveData;
    }
}
