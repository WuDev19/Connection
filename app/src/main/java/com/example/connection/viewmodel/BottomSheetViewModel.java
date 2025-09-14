package com.example.connection.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.listener.OnStateListener;
import com.example.connection.model.User;
import com.example.connection.repository.BottomSheetRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;
import java.util.List;

public class BottomSheetViewModel extends ViewModel implements OnStateListener {

    private final BottomSheetRepository bottomSheetRepository;
    private User currentUser;
    private List<String> friendCurrentUser, friendConfirmCurrentUser;
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Query> queryFriendLiveData = new MutableLiveData<>();
    private final MutableLiveData<Query> querySearchLiveData = new MutableLiveData<>();

    public BottomSheetViewModel(){
        bottomSheetRepository = new BottomSheetRepository(this);
    }

    public void getUserFromFirebase(){
        bottomSheetRepository.getUserFromFirebase().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                currentUser = task.getResult().toObject(User.class);
                if (currentUser == null) return;
                friendCurrentUser = currentUser.getFriend();
                friendConfirmCurrentUser = currentUser.getFriendConfirm();
            }
        });
    }

    public void addFriend(String otherUserId){
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (otherUserId == null || friendCurrentUser == null || friendConfirmCurrentUser == null || currentUserId == null)
            return;
        if (friendConfirmCurrentUser.contains(otherUserId)) {
            friendCurrentUser.add(otherUserId);
            friendConfirmCurrentUser.remove(otherUserId);
            friendConfirmCurrentUser.remove(currentUserId);
            currentUser.setFriend(friendCurrentUser);
            currentUser.setFriendConfirm(friendConfirmCurrentUser);
            bottomSheetRepository.addFriend(otherUserId, currentUser);
        }
        else {
            bottomSheetRepository.sendFriendRequest(otherUserId);
        }
    }

    public void delete(String friendId){
        bottomSheetRepository.delete(friendId, friendCurrentUser);
    }

    public void getQueryFriend(){
        Query query = bottomSheetRepository.getQueryFriend();
        queryFriendLiveData.setValue(query);
    }

    public void getQuerySearch(String username){
        Query query = bottomSheetRepository.getQuerySearch(username);
        querySearchLiveData.setValue(query);
    }

    public LiveData<String> getSuccessLiveData() {
        return successLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Query> getQueryFriendLiveData() {
        return queryFriendLiveData;
    }

    public LiveData<Query> getQuerySearchLiveData() {
        return querySearchLiveData;
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
