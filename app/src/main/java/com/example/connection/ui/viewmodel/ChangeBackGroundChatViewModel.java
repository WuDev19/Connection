package com.example.connection.ui.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.BackGround;
import com.example.connection.repository.ChatRepository;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.List;
import java.util.Objects;

public class ChangeBackGroundChatViewModel extends ViewModel {
    private final ChatRepository changeBackGroundChatRepository;
    private final String TAG = getClass().getName();
    private final MutableLiveData<Boolean> success = new MutableLiveData<>();

    public ChangeBackGroundChatViewModel() {
        changeBackGroundChatRepository = new ChatRepository();
    }

    public LiveData<List<BackGround>> getBackGround() {
        MutableLiveData<List<BackGround>> mutableLiveData = new MutableLiveData<>();
        changeBackGroundChatRepository.getListImageBackGround().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    mutableLiveData.setValue(querySnapshot.toObjects(BackGround.class));
                }
            }
        });
        return mutableLiveData;
    }

    public void updateBackGround(String chatId, String background) {
        if (chatId != null) {
            changeBackGroundChatRepository.updateBackGround(chatId, background).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "onComplete: " + Objects.requireNonNull(task.getException()).getMessage());
                }
                else {
                    success.setValue(true);
                }
            });
        }
    }

    public LiveData<Boolean> getSuccess() {
        return success;
    }

}
