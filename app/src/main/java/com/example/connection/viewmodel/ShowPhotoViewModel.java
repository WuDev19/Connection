package com.example.connection.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.repository.ShowPhotoRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;

public class ShowPhotoViewModel extends ViewModel implements ShowPhotoRepository.OnSendMessage {

    private final ShowPhotoRepository showPhotoRepository;
    private final MutableLiveData<Boolean> updateWidget = new MutableLiveData<>();
    private final MutableLiveData<Boolean> sendMessageSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isExpressHeart = new MutableLiveData<>();
    private final MutableLiveData<Query> query = new MutableLiveData<>();

    public ShowPhotoViewModel(){
        showPhotoRepository = new ShowPhotoRepository(this);
    }

    public void updateSeenPost(Context context){
        showPhotoRepository.updateSeenPostPhoto(context, updateWidget::setValue);
    }

    public void getQueryFireStore(){
        Query query1 = showPhotoRepository.getQuery();
        query.setValue(query1);
    }

    public void sendMessage(String message, String postUrl, String otherId, String otherUserName, String avtPoster){
        if (message.isEmpty()) return;
        String mess = message + "_" + postUrl;
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return;
        String chatRoomId = getChatroomId(myId, otherId);
        showPhotoRepository.sendMessage(chatRoomId, otherId, otherUserName, avtPoster, mess);
    }

    private String getChatroomId(String userId1, String userId2) {
        if (userId1.hashCode() < userId2.hashCode()) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    public LiveData<Boolean> getUpdateWidget() {
        return updateWidget;
    }

    public LiveData<Boolean> getSendMessageSuccess() {
        return sendMessageSuccess;
    }

    public LiveData<Boolean> getIsExpressHeart() {
        return isExpressHeart;
    }

    public LiveData<Query> getQuery() {
        return query;
    }

    @Override
    public void onSuccess(boolean success) {
        sendMessageSuccess.setValue(success);
    }

    @Override
    public void onExpressHeart(boolean isExpress) {
        isExpressHeart.setValue(isExpress);
    }
}
