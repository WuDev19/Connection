package com.example.connection.ui.viewmodel;

import android.app.Activity;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.connection.model.WidgetModel;
import com.example.connection.repository.base.UserRepository;
import com.example.connection.repository.impl.UserRepositoryImpl;

import java.util.HashMap;
import java.util.List;

public class MainViewModel extends ViewModel {

    private final UserRepository mainRepository;
    private final MutableLiveData<List<String>> listFriendConfirmLiveData = new MutableLiveData<>();
    private final MutableLiveData<HashMap<String, Integer>> countMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<WidgetModel> widgetLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cameraMusicPlaying = new MutableLiveData<>();

    public MainViewModel(){
        mainRepository = new UserRepositoryImpl();
    }

    public void setFirstUse(Activity activity){
        mainRepository.setFirstUse(activity);
    }

    public void checkActive(){
        mainRepository.checkActive();
    }

    public void setActive(){
        mainRepository.setActive();
    }

    public void getNotificationListenerRealtime(){
        mainRepository.getNotificationListenerRealtime(listFriendConfirmLiveData::setValue);
    }

    public void getRealtimeUnseenMessage(){
        mainRepository.getRealtimeUnseenMessage(countMessageLiveData::setValue);
    }

    public void unregisterListeningRealtime(){
        mainRepository.unregisterListeningRealtime();
    }

    public void scheduleJob(Context context){
        mainRepository.scheduleJob(context, widgetLiveData::setValue);
    }

    public LiveData<List<String>> getListFriendConfirmLiveData(){
        return listFriendConfirmLiveData;
    }

    public LiveData<HashMap<String, Integer>> getCountMessageLiveData(){
        return countMessageLiveData;
    }

    public LiveData<WidgetModel> getWidgetLiveData(){
        return widgetLiveData;
    }

    public LiveData<Boolean> getFirstUse(Context context){
        return mainRepository.getFirstUse(context);
    }

    public LiveData<Boolean> getCameraMusicPlaying() {
        return cameraMusicPlaying;
    }

    public void setCameraMusicPlaying(boolean playing) {
        cameraMusicPlaying.setValue(playing);
    }

}
