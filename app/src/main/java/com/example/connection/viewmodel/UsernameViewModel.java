package com.example.connection.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.listener.OnStateListener;
import com.example.connection.model.User;
import com.example.connection.repository.UsernameRepository;

public class UsernameViewModel extends ViewModel {

    private final UsernameRepository usernameRepository;
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public UsernameViewModel() {
        usernameRepository = new UsernameRepository();
    }

    public LiveData<String> getSuccessLiveData() {
        return successLiveData;
    }

    public LiveData<String> getUsername() {
        return username;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public void getCurrentUsername() {
        usernameRepository.getCurrentUserName().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User user = task.getResult().toObject(User.class);
                if (user == null) return;
                username.setValue(user.getUserName());
            }
        });
    }

    public void setUsername(String name, String currentName) {
        if (usernameRepository.getCurrentUserId() == null) return;
        if (name.equals(currentName)) {
            successLiveData.setValue("setUserName");
        } else {
            usernameRepository.setUsername(name, new OnStateListener() {
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
            });
        }
    }

}
