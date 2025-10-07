package com.example.connection.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ChangeBackGroundChatRepository {
    private final FirebaseFirestore db;

    public ChangeBackGroundChatRepository(){
        db = FirebaseFirestore.getInstance();
    }

    public Task<QuerySnapshot> getListImageBackGround(){
        return db.collection("background_chat")
                .get();
    }

    public Task<Void> updateBackGround(String chatId, String background){
        return db.collection("chatroom")
                .document(chatId)
                .update("background", background);
    }

}
