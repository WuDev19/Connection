package com.example.connection.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class EmotePostRepository {

    private final FirebaseFirestore db;

    public EmotePostRepository(){
        db = FirebaseFirestore.getInstance();
    }

    public Query getQuery(String postId){
        return db.collection("post_photo")
                .document(postId)
                .collection("emotes")
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

}
