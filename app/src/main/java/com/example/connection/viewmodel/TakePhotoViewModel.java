package com.example.connection.viewmodel;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.listener.OnStateListener;
import com.example.connection.model.Image;
import com.example.connection.repository.TakePhotoRepository;
import java.io.File;
import java.util.List;

public class TakePhotoViewModel extends ViewModel implements OnStateListener {

    private final TakePhotoRepository takePhotoRepository;
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> failureLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Image>> numberOfNewPost = new MutableLiveData<>();

    public TakePhotoViewModel (){
        takePhotoRepository = new TakePhotoRepository(this);
    }

    public void getUserInfoFromFirebase(){
        takePhotoRepository.getUserInfoFromFirebase();
    }

    public void unregisterListening(){
        takePhotoRepository.unregisterListening();
    }

    public void uploadPost(Bitmap bitmap, File fileVideo, String urlMusic, String name, String singer, String status, Context context, boolean isVideo){
        if(isVideo){
            Log.e("fileVideo", "uploadBitmap: " + fileVideo);
            if(fileVideo == null) {
                Log.e("fileVideo", "uploadBitmap: ");
                return;
            }
            takePhotoRepository.uploadVideo(fileVideo, urlMusic, name, singer, status);
        }
        else {
            takePhotoRepository.uploadBitmap(bitmap, urlMusic, name, singer, status, context);
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
