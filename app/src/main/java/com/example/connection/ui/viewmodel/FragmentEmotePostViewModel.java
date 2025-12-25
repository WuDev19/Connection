package com.example.connection.ui.viewmodel;

import androidx.lifecycle.ViewModel;

import com.example.connection.repository.EmotePostRepository;
import com.google.firebase.firestore.Query;

public class FragmentEmotePostViewModel extends ViewModel {
    private final EmotePostRepository emotePostRepository;

    public FragmentEmotePostViewModel(){
        emotePostRepository = new EmotePostRepository();
    }

    public Query getQuery(String postId){
        return emotePostRepository.getQuery(postId);
    }

}