package com.example.connection.viewmodel;

import android.util.Log;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.ChatRoom;
import com.example.connection.repository.FragmentChangeAliasRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FragmentChangeAliasViewModel extends ViewModel {
    private final FragmentChangeAliasRepository fragmentChangeAliasRepository;
    private final String TAG = getClass().getName();

    public FragmentChangeAliasViewModel() {
        fragmentChangeAliasRepository = new FragmentChangeAliasRepository();
    }

    public void updateAlias(ChatRoom chatRoom, String id, String alias, Map<String, String> aliasMap) {
        List<String> searchChat = chatRoom.getSearchChat();
        String oldAlias = aliasMap.get(id);
        String newAlias = null;
        if (oldAlias != null) {
            if (oldAlias.contains("_")) {
                if (alias != null) {
                    newAlias = oldAlias.split("_")[0] + "_" + alias;
                    searchChat.add(alias.toLowerCase());
                    searchChat.remove(oldAlias.split("_")[1].toLowerCase());
                } else {
                    newAlias = oldAlias.split("_")[0];
                    searchChat.remove(oldAlias.split("_")[1].toLowerCase());
                }
            } else {
                if (alias != null) {
                    newAlias = oldAlias + "_" + alias;
                    searchChat.add(alias.toLowerCase());
                }
            }
        }
        if (newAlias != null) aliasMap.put(id, newAlias);
        chatRoom.setAlias(aliasMap);
        chatRoom.setSearchChat(searchChat);
        fragmentChangeAliasRepository.updateAlias(chatRoom).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "onComplete: " + Objects.requireNonNull(task.getException()).getMessage());
            }
        });
    }

}
