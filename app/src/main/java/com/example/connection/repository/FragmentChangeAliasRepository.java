package com.example.connection.repository;

import com.example.connection.model.ChatRoom;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

public class FragmentChangeAliasRepository {

    public final FirebaseFirestore db;

    public FragmentChangeAliasRepository(){
        db = FirebaseFirestore.getInstance();
    }

    public Task<Void> updateAlias(ChatRoom chatRoom){
        return db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .set(chatRoom);
    }

}
