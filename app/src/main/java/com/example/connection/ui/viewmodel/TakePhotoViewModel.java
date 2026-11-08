package com.example.connection.ui.viewmodel;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.ui.state.OnStateListener;
import com.example.connection.model.Image;
import com.example.connection.model.SelectionFriendModel;
import com.example.connection.repository.PostRepository;
import java.io.File;
import java.util.List;

public class TakePhotoViewModel extends ViewModel implements OnStateListener {

    private final PostRepository takePhotoRepository;
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> failureLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Image>> numberOfNewPost = new MutableLiveData<>();
    private final MutableLiveData<List<String>> listFriendIdLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<SelectionFriendModel>> selectionFriendLiveData = new MutableLiveData<>();

    public TakePhotoViewModel (){
        takePhotoRepository = new PostRepository(this);
    }

    public void getUserInfoFromFirebase(){
        takePhotoRepository.getUserInfoFromFirebase(new PostRepository.OnGetListFriendCallback() {
            @Override
            public void getListFriend(List<SelectionFriendModel> friends) {
                selectionFriendLiveData.setValue(friends);
            }

            @Override
            public void getListFriendId(List<String> listFriendId) {
                listFriendIdLiveData.setValue(listFriendId);
            }

            @Override
            public void error(String err) {
                failureLiveData.setValue(err);
            }
        });
    }

    public void unregisterListening(){
        takePhotoRepository.unregisterListening();
    }

    public void uploadPost(Bitmap bitmap, File fileVideo, String urlMusic, String name, String singer,
                           String status, Context context, boolean isVideo, List<String> listFriend){
        if(isVideo){
            Log.e("fileVideo", "uploadBitmap: " + fileVideo);
            if(fileVideo == null) {
                Log.e("fileVideo", "uploadBitmap: ");
                return;
            }
            takePhotoRepository.uploadVideo(fileVideo, urlMusic, name, singer, status, listFriend);
        }
        else {
            takePhotoRepository.uploadBitmap(bitmap, urlMusic, name, singer, status, listFriend, context);
        }
    }

    public void createFolderConnectionExternalStorage(Bitmap photo, Context context){
        takePhotoRepository.createFolderConnectionExternalStorage(photo, context);
    }

    public void listenForNewPosts(){
        takePhotoRepository.listenForNewPosts(numberOfNewPost::setValue);
    }

    public void downloadPhoto(Bitmap photo, Context context){
        takePhotoRepository.downloadPhoto(photo, context);
    }

    public void downloadVideo(File videoFile, Context context){
        takePhotoRepository.downloadVideo(videoFile, context);
    }

    public LiveData<String> getSuccessLiveData() {
        return successLiveData;
    }

    public LiveData<String> getFailureLiveData() {
        return failureLiveData;
    }

    public LiveData<List<Image>> getNumberOfNewPost() {
        return numberOfNewPost;
    }

    public LiveData<List<String>> getListFriendLiveData() {
        return listFriendIdLiveData;
    }

    public String getCurrentUserId(){
        return takePhotoRepository.getCurrentUserId();
    }

    public LiveData<List<SelectionFriendModel>> getSelectionFriendLiveData() {
        return selectionFriendLiveData;
    }

    @Override
    public void onSuccess(String success) {
        successLiveData.postValue(success);
    }

    @Override
    public void onFailure(String error) {
        failureLiveData.postValue(error);
    }

    @Override
    public void onLoading(boolean isLoading) {

    }

    @Override
    protected void onCleared() {
        super.onCleared();
        takePhotoRepository.shutdownExecutor();
    }
}
