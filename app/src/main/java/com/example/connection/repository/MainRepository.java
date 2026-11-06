package com.example.connection.repository;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.Image;
import com.example.connection.model.User;
import com.example.connection.model.WidgetModel;
import com.example.connection.constants.SharePreUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainRepository {

    private final FirebaseAuth mAuth;
    private final FirebaseFirestore db;
    private final String TAG = "MainRepository";
    private ListenerRegistration chatroomListenerRealTime, notificationListenerRealtime;
    private final List<ListenerRegistration> messageListenerRealTime = new ArrayList<>();
    private final HashMap<String, Integer> countUnseenMessage = new HashMap<>();

    public MainRepository() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public interface OnGetListFriendConfirm{
        void onGetListFriendConfirm(List<String> list);
    }

    public interface OnCountMessage{
        void onCountMessage(HashMap<String, Integer> countMessage);
    }

    public interface OnUpdateWidget{
        void onUpdateWidget(WidgetModel widgetModel);
    }

    public void setFirstUse(Activity activity) {
        SharePreUtil.getInstance(activity.getApplicationContext()).setFirstUseApp();
    }

    public LiveData<Boolean> getFirstUse(Context context){
        MutableLiveData<Boolean> firstUse = new MutableLiveData<>();
        firstUse.setValue(SharePreUtil.getInstance(context.getApplicationContext()).firstUseApp());
        return firstUse;
    }

    public void checkActive() {
        String id = mAuth.getUid();
        if (id == null) return;
        db.collection("users").document(id).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                User user1 = task.getResult().toObject(User.class);
                if (user1 == null) return;
                if (!user1.isActive()) {
                    FirebaseFirestore.getInstance().collection("users").document(id)
                            .update("active", true).addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()) {
                                    Log.e(TAG, "Đang hoạt động");
                                } else {
                                    Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task1.getException()).getMessage()));
                                }
                            });
                }
            } else
                Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
        });
    }

    public void setActive(){
        String id = mAuth.getUid();
        if(id == null) return;
        db.collection("users").document(id).update("active", false)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, Objects.requireNonNull(Objects.requireNonNull(task.getException()).getMessage()));
                    }
                });
    }

    public void getNotificationListenerRealtime(OnGetListFriendConfirm onGetListFriendConfirm){
        String currentUserId = mAuth.getUid();
        if (currentUserId == null) return;
        if (notificationListenerRealtime != null) {
            notificationListenerRealtime.remove();
        }
        notificationListenerRealtime = FirebaseFirestore.getInstance().collection("users")
                .document(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if(error != null) return;
                    if(value == null) return;
                    User user = value.toObject(User.class);
                    if (user != null) {
                        onGetListFriendConfirm.onGetListFriendConfirm(user.getFriendConfirm());
                    }
                });
    }

    public void getRealtimeUnseenMessage(OnCountMessage onCountMessage) {
        String currentUserId = mAuth.getUid();
        if(currentUserId == null) return;

        if(chatroomListenerRealTime != null){
            chatroomListenerRealTime.remove();
        }

        // Single snapshot listener on chatroom list instead of opening N subcollection listeners
        chatroomListenerRealTime = FirebaseFirestore.getInstance().collection("chatroom")
                .whereArrayContains("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if(error != null || value == null) {
                        return;
                    }
                    countUnseenMessage.clear();
                    for (DocumentSnapshot documentSnapshot : value) {
                        String chatRoomId = documentSnapshot.getId();
                        ChatRoom room = documentSnapshot.toObject(ChatRoom.class);
                        long unread = 0;
                        if (room != null && room.getUnreadCounts() != null && room.getUnreadCounts().containsKey(currentUserId)) {
                            Long count = room.getUnreadCounts().get(currentUserId);
                            if (count != null) {
                                unread = count;
                            }
                        }
                        countUnseenMessage.put(chatRoomId, (int) unread);
                    }
                    onCountMessage.onCountMessage(countUnseenMessage);
                });
    }

    public void unregisterListeningRealtime(){
        if (chatroomListenerRealTime != null) {
            chatroomListenerRealTime.remove();
            chatroomListenerRealTime = null;
        }
        for (ListenerRegistration listenerRegistration : messageListenerRealTime) {
            listenerRegistration.remove();
        }
        messageListenerRealTime.clear();
        countUnseenMessage.clear();
        if(notificationListenerRealtime != null){
            notificationListenerRealtime.remove();
            notificationListenerRealtime = null;
        }
    }

    public void scheduleJob(Context context, OnUpdateWidget onUpdateWidget) {
        if(!SharePreUtil.getInstance(context.getApplicationContext()).enableWidget()) return;
        Log.e(TAG, "scheduleJob: ");
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return;
        FirebaseFirestore.getInstance().collection("post_photo")
                .whereArrayContains("friendId", myId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Image> list = new ArrayList<>();
                    Image imageFirst = null;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.exists()) {
                            Image image = doc.toObject(Image.class);
                            if (image != null) {
                                if (imageFirst == null) imageFirst = image;
                                List<String> seen = image.getSeen();
                                if (seen != null && !seen.contains(myId)) {
                                    list.add(image);
                                }
                            }
                        }
                    }
                    if (imageFirst == null) {
                        Log.e(TAG, "Ko có dữ liệu");
                        return;
                    }
                    final Image finalImageFirst = imageFirst;
                    FirebaseFirestore.getInstance().collection("users").document(imageFirst.getPosterId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    User user = documentSnapshot.toObject(User.class);
                                    if (user != null) {
                                        onUpdateWidget.onUpdateWidget(new WidgetModel(list, user, finalImageFirst));
                                    }
                                }
                            });
                });
    }

}
