package com.example.connection.ui.viewmodel;

import android.app.Activity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.repository.OTPRepository;

public class OTPViewModel extends ViewModel implements OTPRepository.OnLoginWithOtp {

    private final OTPRepository otpRepository;
    private final MutableLiveData<Boolean> checkOTPLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> verifyLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> validateLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> codeSentLiveData = new MutableLiveData<>();

    public OTPViewModel (){
        otpRepository = new OTPRepository(this);
    }

    public void loginWithOTP(String verify, String otp, String sdt){
        otpRepository.loginWithOTP(verify, otp, sdt);
    }

    public void resendOTP(String sdt, Activity activity){
        otpRepository.resendOTP(sdt, activity);
    }

    public LiveData<Boolean> getCheckOTP(){
        return checkOTPLiveData;
    }

    public LiveData<Boolean> getSuccess(){
        return successLiveData;
    }

    public LiveData<String> getVerify(){
        return verifyLiveData;
    }

    public LiveData<Boolean> getValidate(){
        return validateLiveData;
    }

    public LiveData<Boolean> getCodeSent(){
        return codeSentLiveData;
    }

    @Override
    public void onCheckOTP(boolean checkOTP) {
        checkOTPLiveData.setValue(checkOTP);
    }

    @Override
    public void onSuccess(boolean success) {
        successLiveData.setValue(success);
    }

    @Override
    public void onValidate(boolean validate) {
        validateLiveData.setValue(validate);
    }

    @Override
    public void onCodeSent(boolean codeSent) {
        codeSentLiveData.setValue(codeSent);
    }

    @Override
    public void onVerify(String verify) {
        verifyLiveData.setValue(verify);
    }
}
