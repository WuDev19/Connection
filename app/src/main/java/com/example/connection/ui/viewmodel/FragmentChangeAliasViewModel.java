package com.example.connection.ui.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.ChatRoom;
import com.example.connection.repository.FragmentChangeAliasRepository;
import java.util.Map;
import java.util.Objects;

public class FragmentChangeAliasViewModel extends ViewModel {
    private final FragmentChangeAliasRepository fragmentChangeAliasRepository;
    private final String TAG = getClass().getName();
    private final MutableLiveData<ChatRoom> chatRoomListeningLiveData = new MutableLiveData<>();

    public FragmentChangeAliasViewModel() {
        fragmentChangeAliasRepository = new FragmentChangeAliasRepository();
    }

    public void updateAlias(ChatRoom chatRoom, String id, String alias, Map<String, String> aliasMap) {
        String oldAlias = aliasMap.get(id);
        String newAlias = null;
        if (oldAlias != null) {
            if (oldAlias.contains("_")) {
                if (alias != null) {
                    newAlias = oldAlias.split("_")[0] + "_" + alias;
                } else {
                    newAlias = oldAlias.split("_")[0];
                }
            } else {
                if (alias != null) {
                    newAlias = oldAlias + "_" + alias;
                }
            }
        }
        if (newAlias != null) {
            fragmentChangeAliasRepository.updateAlias(chatRoom, id, newAlias).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "onComplete: " + Objects.requireNonNull(task.getException()).getMessage());
                }
            });
        }
    }

    public void listeningChatRoom(ChatRoom chatRoom) {
        fragmentChangeAliasRepository.listeningChatRoom(chatRoom, chatRoomListeningLiveData::setValue);
    }

    public void unregisterListeningChatRoom() {
        fragmentChangeAliasRepository.unregisterListeningChatRoom();
    }

    public LiveData<ChatRoom> getChatRoomListeningLiveData() {
        return chatRoomListeningLiveData;
    }

}
