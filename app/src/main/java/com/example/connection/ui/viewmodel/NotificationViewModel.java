package com.example.connection.ui.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.User;
import com.example.connection.repository.base.UserRepository;
import com.example.connection.repository.impl.UserRepositoryImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationViewModel extends ViewModel {

    private final List<String> listFriendConfirm;
    private List<String> friendCurrentUser;
    private final UserRepository notificationRepository;
    private User user;
    private final String currentUserId;
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> setAdapter = new MutableLiveData<>();
    private final MutableLiveData<String> setNotification = new MutableLiveData<>();
    private final Map<String, MutableLiveData<User>> cacheUser = new HashMap<>();

    public NotificationViewModel(SavedStateHandle handle) {
        listFriendConfirm = handle.get("confirm_friend");
        Log.e("CHECKK", "NotificationViewModel: " + listFriendConfirm);
        notificationRepository = new UserRepositoryImpl();
        currentUserId = notificationRepository.getUserId();
    }

    public void getFriendCurrentUser() {
        if (currentUserId == null) return;
        notificationRepository.getUser(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                user = task.getResult().toObject(User.class);
                if (user != null) {
                    friendCurrentUser = user.getFriend();
                }
            }
        });
    }

    public LiveData<String> onAccept(String otherUserId, int position) {
        MutableLiveData<String> successLiveData = new MutableLiveData<>();
        if (otherUserId != null){
            listFriendConfirm.remove(position);
            setAdapter.setValue(listFriendConfirm);
            friendCurrentUser.add(otherUserId);
            user.setFriend(friendCurrentUser);
            user.setFriendConfirm(listFriendConfirm);
            Log.e("kiemtrasuccess", "onAccept: success");
            notificationRepository.acceptFriend(otherUserId, currentUserId, user, new UserRepository.HandleFriendNotification() {
                @Override
                public void onSuccess(String success) {
                    successLiveData.setValue(success);
                }

                @Override
                public void onFailure(String error) {
                    errorLiveData.setValue(error);
                }
            });
        }
        return successLiveData;
    }

    public void onCancel(int position) {
        listFriendConfirm.remove(position);
        setAdapter.setValue(listFriendConfirm);
        user.setFriendConfirm(listFriendConfirm);
        notificationRepository.declineFriend(currentUserId, user, new UserRepository.HandleFriendNotification() {
            @Override
            public void onSuccess(String success) {
            }

            @Override
            public void onFailure(String error) {
                errorLiveData.setValue(error);
            }
        });
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public LiveData<User> getUser(String id) {
        if(cacheUser.containsKey(id)){
            return cacheUser.get(id);
        }
        MutableLiveData<User> getCurrentUser = new MutableLiveData<>();
        cacheUser.put(id, getCurrentUser);
        notificationRepository.getUser(id).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User user1 = task.getResult().toObject(User.class);
                if (user1 != null) {
                    getCurrentUser.setValue(user1);
                }
            }
        });
        return getCurrentUser;
    }


    public void numberOfNotification() {
        if (listFriendConfirm != null) {
            listFriendConfirm.remove(currentUserId);
            if (listFriendConfirm.isEmpty()) {
                Log.e("CHECKK", "numberOfNotification: ");
                setNotification.setValue("Hiện không có thông báo");
            }
        }
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public LiveData<List<String>> getSetAdapter() {
        return setAdapter;
    }

    public LiveData<String> getSetNotification() {
        return setNotification;
    }

}
