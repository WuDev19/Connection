package com.example.connection.repository;

import android.util.Log;
import com.example.connection.model.ChatRoom;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class FragmentChangeAliasRepository {

    private final FirebaseFirestore db;
    private ListenerRegistration chatRoomListening;

    public FragmentChangeAliasRepository(){
        db = FirebaseFirestore.getInstance();
    }

    public Task<Void> updateAlias(ChatRoom chatRoom){
        return db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .set(chatRoom);
    }

    public void listeningChatRoom(ChatRoom chatRoom, ChatRepository.OnUpdateChatRoom updateChatRoom){
        if(chatRoomListening != null) {
            chatRoomListening.remove();
            Log.e("ChatActivity", "listeningChatRoom: remove");
        }
        chatRoomListening = db.collection("chatroom")
                .document(chatRoom.getChatRoomId())
                .addSnapshotListener((value, error) -> {
                    if(value == null || error != null) return;
                    ChatRoom chatRoom1 = value.toObject(ChatRoom.class);
                    if(chatRoom1 != null){
                        updateChatRoom.onUpdate(chatRoom1);
                    }
                });
    }

    public void unregisterListeningChatRoom(){
        if(chatRoomListening != null) {
            chatRoomListening.remove();
            Log.e("FragmentChangeAlias", "unregisterListeningChatRoom: " + chatRoomListening);
        }
    }

}
