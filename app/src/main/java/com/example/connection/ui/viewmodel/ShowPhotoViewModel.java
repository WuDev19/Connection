package com.example.connection.ui.viewmodel;

import android.content.Context;
import android.util.Log;
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
    private final MutableLiveData<Query> query = new MutableLiveData<>();
    private final MutableLiveData<Boolean> expressDone = new MutableLiveData<>();
    private final String TAG = getClass().getName();

    public ShowPhotoViewModel() {
        showPhotoRepository = new ShowPhotoRepository(this);
    }

    public void updateSeenPost(Context context) {
        showPhotoRepository.updateSeenPostPhoto(context, updateWidget::setValue);
    }

    public void getQueryFireStore() {
        Query query1 = showPhotoRepository.getQuery();
        query.setValue(query1);
    }

    public void sendMessage(String message, String postUrl, String otherId, String otherUserName, String avtPoster) {
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

    public void expressEmote(String emote, String postId) {
        showPhotoRepository.modifyEmote(postId, emote)
                .addOnSuccessListener(unused -> {
                    Log.e(TAG, "onSuccess: ");
                    expressDone.setValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "onFailure: " + e.getMessage());
                    expressDone.setValue(true);
                });
    }

    public LiveData<Boolean> getUpdateWidget() {
        return updateWidget;
    }

    public LiveData<Boolean> getSendMessageSuccess() {
        return sendMessageSuccess;
    }

    public LiveData<Query> getQuery() {
        return query;
    }

    public LiveData<Boolean> getExpressDone() {
        return expressDone;
    }

    @Override
    public void onSuccess(boolean success) {
        sendMessageSuccess.setValue(success);
    }

}
