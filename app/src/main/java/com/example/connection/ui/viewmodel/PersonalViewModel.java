package com.example.connection.ui.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.ui.state.OnStateListener;
import com.example.connection.model.User;
import com.example.connection.repository.PersonalRepository;

public class PersonalViewModel extends ViewModel implements OnStateListener {

    private final PersonalRepository personalRepository;
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public PersonalViewModel (){
        personalRepository = new PersonalRepository(this);
    }

    public void showUserFromFirebase(){
        personalRepository.showUserFromFirebase(userLiveData::setValue);
    }

    public void unregisterUserListening(){
        personalRepository.unregisterUserListening();
    }

    public void updateEmail(Context context){
        personalRepository.updateNewEmailToFireStore(context);
    }

    public void signOut(){
        personalRepository.signOut();
    }

    public void deleteUser(){
        personalRepository.deleteUser();
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
