package com.example.connection.ui.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.ui.state.OnStateListener;
import com.example.connection.repository.ChangePasswordRepository;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordViewModel extends ViewModel implements OnStateListener {

    private final ChangePasswordRepository changePasswordRepository;
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> passwordConfirmLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();

    public ChangePasswordViewModel (){
        changePasswordRepository = new ChangePasswordRepository(this);
    }

    public void checkPasswordConfirm(Context context, String oldPassword, String newPassword, String newPasswordConfirm){
        String password = changePasswordRepository.getPassword(context);
        if(password == null || oldPassword.compareTo(password) != 0 || newPassword.compareTo(newPasswordConfirm) != 0
                || oldPassword.isEmpty()
                || newPassword.isEmpty()
                || newPasswordConfirm.isEmpty()){
            if(password == null || oldPassword.compareTo(password) != 0){
                errorLiveData.setValue("Mật khẩu cũ không chính xác");
            }
            if(newPassword.compareTo(newPasswordConfirm) != 0){
                errorLiveData.setValue("Mật khẩu xác nhận không chính xác");
            }
            if(oldPassword.isEmpty() || newPassword.isEmpty() || newPasswordConfirm.isEmpty()){
                errorLiveData.setValue("Vui lòng nhập đầy đủ thông tin");
            }
            return;
        }
        passwordConfirmLiveData.setValue(newPasswordConfirm);
    }

    public void setPassword(Context context, String newPassword){
        changePasswordRepository.setPassword(context, newPassword);
    }

    public void updatePassword(String newPasswordConfirm){
        FirebaseUser user = changePasswordRepository.getUser();
        if(user == null) {
            errorLiveData.setValue("Mạng có vẻ kém, vui lòng kiểm tra lại mạng");
            return;
        }
        changePasswordRepository.updatePassword(user, newPasswordConfirm);
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<String> getPasswordConfirmLiveData() {
        return passwordConfirmLiveData;
    }

    public LiveData<String> getSuccess() {
        return successLiveData;
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
