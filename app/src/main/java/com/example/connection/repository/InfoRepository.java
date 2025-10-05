package com.example.connection.repository;

import android.net.Uri;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.connection.listener.OnStateListener;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.Comment;
import com.example.connection.model.EmoteModel;
import com.example.connection.model.Image;
import com.example.connection.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InfoRepository {

    private final OnStateListener onStateListener;
    private final String TAG = getClass().getName();
    private final FirebaseFirestore db;

    public InfoRepository(OnStateListener onStateListener) {
        this.onStateListener = onStateListener;
        db = FirebaseFirestore.getInstance();
    }

    public Task<DocumentSnapshot> getCurrentUser() {
        String id = FirebaseAuth.getInstance().getUid();
        if (id == null) return null;
        return db.collection("users").document(id)
                .get();
    }

    private void updateUserToFirebase(User user) {
        String id = FirebaseAuth.getInstance().getUid();
        if (id == null) return;
        db.collection("users").document(id)
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateRelativeInfo(id, user);
                        onStateListener.onSuccess("Cập nhật thông tin thành công");
                        onStateListener.onLoading(false);
                    } else {
                        Exception exception = task.getException();
                        if (exception != null) {
                            onStateListener.onFailure(exception.getMessage());
                        }
                    }
                });
    }

    // TODO: 10/3/2025 xu ly neu batchsize > 500 
    private void updateRelativeInfo(String id, User user) {
        WriteBatch writeBatch = db.batch();

        Task<QuerySnapshot> taskChat = db.collection("chatroom")
                .whereArrayContains("userId", id)
                .get();

        Task<QuerySnapshot> taskPost = db.collection("post_photo")
                .whereEqualTo("posterId", id)
                .get();

        Tasks.whenAllSuccess(taskChat, taskPost)
                .addOnSuccessListener(results -> {
                    QuerySnapshot chatResult = (QuerySnapshot) results.get(0);
                    QuerySnapshot postResult = (QuerySnapshot) results.get(1);

                    List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                    for (DocumentSnapshot documentSnapshot : chatResult) {
                        Log.e(TAG, "updateRelativeInfo: chat");
                        ChatRoom chatRoom = documentSnapshot.toObject(ChatRoom.class);
                        if (chatRoom != null) {
                            Map<String, String> map = chatRoom.getAvtSender();
                            map.put(user.getId(), user.getAvatarUser());
                            writeBatch.update(documentSnapshot.getReference(), "avtSender", map);
                        }
                    }
                    for (DocumentSnapshot documentSnapshot : postResult) {
                        Log.e(TAG, "updateRelativeInfo: post");
                        Image image = documentSnapshot.toObject(Image.class);
                        if (image != null) {
                            tasks.add(documentSnapshot.getReference().collection("emotes")
                                    .whereEqualTo("idUser", id)
                                    .get());
                            tasks.add(documentSnapshot.getReference().collection("comments")
                                    .whereEqualTo("idUser", id)
                                    .get());
                            writeBatch.update(documentSnapshot.getReference(), "avatarPoster", user.getAvatarUser());
                            writeBatch.update(documentSnapshot.getReference(), "userNamePoster", user.getUserName());
                        }
                    }
                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(objects -> {
                                for (Object object : objects) {
                                    if (object instanceof QuerySnapshot) {
                                        QuerySnapshot querySnapshot = (QuerySnapshot) object;
                                        for (DocumentSnapshot docSnap : querySnapshot) {
                                            if (docSnap.exists()) {
                                                if (docSnap.toObject(EmoteModel.class) != null) {
                                                    Log.e(TAG, "updateRelativeInfo: emote");
                                                    writeBatch.update(docSnap.getReference(), "urlAvt", user.getAvatarUser());
                                                    writeBatch.update(docSnap.getReference(), "username", user.getUserName());
                                                } else if (docSnap.toObject(Comment.class) != null) {
                                                    Log.e(TAG, "updateRelativeInfo: comment");
                                                    writeBatch.update(docSnap.getReference(), "avtComment", user.getAvatarUser());
                                                    writeBatch.update(docSnap.getReference(), "userNameComment", user.getUserName());
                                                }
                                            }
                                        }
                                    }
                                }
                                writeBatch.commit().addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Log.e(TAG, "Update avatar thành công cho cả chatroom và post_photo");
                                    }
                                });
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
    }

    public void upload_avt(Uri uri, String username, User user) {
        if (uri == null) {
            onStateListener.onLoading(true);
            if (!username.isEmpty()) {
                user.setUserName(username);
                user.setUserNameLowerCase(username.toLowerCase());
            }
            updateUserToFirebase(user);
            return;
        }
        Log.e(TAG, "upload_avt: " + uri);
        MediaManager.get().upload(uri).unsigned("post_upload").option("folder", "avatar/").callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                onStateListener.onLoading(true);
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {

            }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                if (imageUrl != null) user.setAvatarUser(imageUrl);
                if (!username.isEmpty()) {
                    user.setUserName(username);
                    user.setUserNameLowerCase(username.toLowerCase());
                }
                updateUserToFirebase(user);
            }

            @Override
            public void onError(String requestId, ErrorInfo errorInfo) {
                onStateListener.onFailure(errorInfo.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {

            }
        }).dispatch();
    }

}