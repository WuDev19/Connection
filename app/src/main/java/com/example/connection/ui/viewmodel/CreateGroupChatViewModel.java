package com.example.connection.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.connection.ui.state.OnStateListener;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.User;
import com.example.connection.repository.base.ChatRepository;
import com.example.connection.repository.impl.ChatRepositoryImpl;
import java.util.List;

public class CreateGroupChatViewModel extends ViewModel {

    private final ChatRepository createGroupChatRepository;
    private final MutableLiveData<List<String>> listFriendLiveData = new MutableLiveData<>();
    private final MutableLiveData<User> userFriendLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<User> currentUserLiveData = new MutableLiveData<>();

    public CreateGroupChatViewModel(){
        createGroupChatRepository = new ChatRepositoryImpl();
    }

    public void getListFriend(){
        createGroupChatRepository.getUser().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                User user = task.getResult().toObject(User.class);
                if(user != null){
                    currentUserLiveData.setValue(user);
                    listFriendLiveData.setValue(user.getFriend());
                }
            }
        });
    }

    public void getUserFriend(String id){
        if(id == null) return;
        createGroupChatRepository.getUserFriend(id).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                User userFriend = task.getResult().toObject(User.class);
                if(userFriend != null){
                    userFriendLiveData.setValue(userFriend);
                }
            }
        });

    }

    public void setChatRoomToFireStore(ChatRoom chatRoom){
        createGroupChatRepository.setChatRoomToFireStore(chatRoom, new OnStateListener() {
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

    public LiveData<User> getCurrentUserLiveData() {
        return currentUserLiveData;
    }

    public LiveData<User> getUserFriendLiveData() {
        return userFriendLiveData;
    }

    public LiveData<List<User>> searchUser(String username){
        return createGroupChatRepository.searchUser(username);
    }

    public LiveData<List<String>> getListFriendLiveData() {
        return listFriendLiveData;
    }

    public LiveData<String> getSuccessLiveData() {
        return successLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }
}
