package com.example.connection.ui.viewmodel;

import androidx.lifecycle.ViewModel;

import com.example.connection.repository.PostRepository;
import com.google.firebase.firestore.Query;

public class FragmentEmotePostViewModel extends ViewModel {
    private final PostRepository emotePostRepository;

    public FragmentEmotePostViewModel(){
        emotePostRepository = new PostRepository();
    }

    public Query getQuery(String postId){
        return emotePostRepository.getEmoteQuery(postId);
    }

}