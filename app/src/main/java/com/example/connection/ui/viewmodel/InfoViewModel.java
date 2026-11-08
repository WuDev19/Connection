package com.example.connection.ui.viewmodel;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.connection.ui.state.OnStateListener;
import com.example.connection.model.User;
import com.example.connection.repository.UserRepository;

public class InfoViewModel extends ViewModel implements OnStateListener {

    private final UserRepository infoRepository;
    private User user;
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>();
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();

    public InfoViewModel() {
        infoRepository = new UserRepository(this);
    }

    public void upload_avt(Uri uri, String username) {
        if (user == null) {
            errorLiveData.setValue("Kiểm tra lại kết nối");
            return;
        }
        infoRepository.upload_avt(uri, username, user);
    }

    public void getCurrentUser() {
        infoRepository.getCurrentUser().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user = task.getResult().toObject(User.class);
                if (user != null) {
                    userLiveData.setValue(user);
                }
            }
        });
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getSuccess() {
        return successLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
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
        loading.setValue(isLoading);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        infoRepository.shutdownService();
    }

}
