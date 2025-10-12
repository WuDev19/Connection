package com.example.connection.viewmodel;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.listener.OnStateListener;
import com.example.connection.model.Image;
import com.example.connection.repository.OptionsPostRepository;

public class OptionsPostViewModel extends ViewModel implements OnStateListener {
    private final OptionsPostRepository optionsPostRepository;
    private final MutableLiveData<String> successLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public OptionsPostViewModel(){
        optionsPostRepository = new OptionsPostRepository(this);
    }

    public void deletePost(String postId){
        optionsPostRepository.getPreparePostDelete(postId).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Image image = task.getResult().toObject(Image.class);
                if(image != null){
                    if(image.getPosterId().equals(optionsPostRepository.getCurrentUserId())){
                        optionsPostRepository.deletePost(postId);
                    }
                    else {
                        errorLiveData.setValue("Bạn không thể xóa bài đăng của người khác");
                    }
                }
            }
        });
    }

    public void downloadPost(String url, Context context){
        optionsPostRepository.downloadPost(url, context, new OnStateListener() {
            @Override
            public void onSuccess(String success) {
                successLiveData.postValue(success);
            }

            @Override
            public void onFailure(String error) {
                errorLiveData.postValue(error);
            }

            @Override
            public void onLoading(boolean isLoading) {

            }
        });
    }

    public LiveData<String> getSuccessLiveData() {
        return successLiveData;
    }

    public LiveData<String> getErrorLiveData() {
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

    @Override
    protected void onCleared() {
        super.onCleared();
        optionsPostRepository.shutdownExecutor();
    }

}
