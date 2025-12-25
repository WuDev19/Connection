package com.example.connection.ui.viewmodel;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.connection.repository.SplashRepository;
import com.google.firebase.auth.FirebaseUser;

public class SplashViewModel extends AndroidViewModel {

    private final SplashRepository splashRepository;
    private final String TAG = getClass().getName();

    public SplashViewModel(Application application) {
        super(application);
        splashRepository = new SplashRepository(application);
    }

    public LiveData<FirebaseUser> checkUser() {
        return splashRepository.checkUser();
    }

    public void fetchMusicFireStore() {
        splashRepository.fetchMusicFireStore();
    }

    public LiveData<Integer> getNumberCurrentUser() {
        MutableLiveData<Integer> numberOfUser = new MutableLiveData<>();
        splashRepository.getNumberCurrentUser()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if(queryDocumentSnapshots != null){
                        numberOfUser.setValue(queryDocumentSnapshots.size());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
        return numberOfUser;
    }

}
