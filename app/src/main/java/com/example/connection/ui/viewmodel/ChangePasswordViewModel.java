package com.example.connection.ui.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.ui.state.OnStateListener;
import com.example.connection.repository.UserRepository;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordViewModel extends ViewModel implements OnStateListener {

    private final UserRepository changePasswordRepository;
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> passwordConfirmLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();

    public ChangePasswordViewModel (){
        changePasswordRepository = new UserRepository(this);
    }

    public void checkPasswordConfirm(Context context, String oldPassword, String newPassword, String newPasswordConfirm){
        if(oldPassword == null || newPassword == null || newPasswordConfirm == null
                || oldPassword.isEmpty()
                || newPassword.isEmpty()
                || newPasswordConfirm.isEmpty()){
            errorLiveData.setValue("Vui lòng nhập đầy đủ thông tin");
            return;
        }
        if(newPassword.compareTo(newPasswordConfirm) != 0){
            errorLiveData.setValue("Mật khẩu xác nhận không chính xác");
            return;
        }
        FirebaseUser user = changePasswordRepository.getUser();
        if(user == null) {
            errorLiveData.setValue("Mạng có vẻ kém, vui lòng kiểm tra lại mạng");
            return;
        }
        changePasswordRepository.updatePassword(user, oldPassword, newPasswordConfirm);
    }

    public void setPassword(Context context, String newPassword){
        // No-op: Plaintext passwords are no longer saved to SharedPreferences.
    }

    public void updatePassword(String newPasswordConfirm){
        // Deprecated: Handled in checkPasswordConfirm with reauthentication.
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
    public void onFailure(String failure) {
        errorLiveData.setValue(failure);
    }

    @Override
    public void onLoading(boolean isLoading) {
    }
}
