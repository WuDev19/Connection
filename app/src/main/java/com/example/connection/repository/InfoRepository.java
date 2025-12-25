package com.example.connection.repository;

import android.net.Uri;
import android.util.Log;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.connection.ui.state.OnStateListener;
import com.example.connection.model.ChatRoom;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InfoRepository {

    private final OnStateListener onStateListener;
    private final String TAG = getClass().getName();
    private final FirebaseFirestore db;
    private final ExecutorService updateEmoteAndCommentService = Executors.newSingleThreadExecutor();

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
                        updateEmoteAndComment(id, user);
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

        Task<QuerySnapshot> taskChat = db.collection("chatroom")
                .whereArrayContains("userId", id)
                .get();

        Task<QuerySnapshot> taskPost = db.collection("post_photo")
                .whereEqualTo("posterId", id)
                .get();

        Tasks.whenAllSuccess(taskChat, taskPost)
                .addOnSuccessListener(results -> {
                    WriteBatch batchChatAndPost = db.batch();

                    QuerySnapshot chatResult = (QuerySnapshot) results.get(0);
                    QuerySnapshot postResult = (QuerySnapshot) results.get(1);

                    for (DocumentSnapshot documentSnapshot : chatResult) {
                        Log.e(TAG, "updateRelativeInfo: chat");
                        ChatRoom chatRoom = documentSnapshot.toObject(ChatRoom.class);
                        if (chatRoom != null) {
                            Map<String, String> avtMap = chatRoom.getAvtSender();
                            avtMap.put(user.getId(), user.getAvatarUser());
                            Map<String, String> aliasMap = chatRoom.getAlias();
                            String alias = aliasMap.get(user.getId());
                            if (alias != null) {
                                if (alias.contains("_")) {
                                    alias = user.getUserName() + "_" + alias.split("_")[1];
                                } else {
                                    alias = user.getUserName();
                                }
                                aliasMap.put(user.getId(), alias);
                                batchChatAndPost.update(documentSnapshot.getReference(), "alias", aliasMap);
                            }
                            batchChatAndPost.update(documentSnapshot.getReference(), "avtSender", avtMap);
                        }
                    }
                    for (DocumentSnapshot documentSnapshot : postResult) {
                        Log.e(TAG, "updateRelativeInfo: post");
                        Image image = documentSnapshot.toObject(Image.class);
                        if (image != null) {
                            batchChatAndPost.update(documentSnapshot.getReference(), "avatarPoster", user.getAvatarUser());
                            batchChatAndPost.update(documentSnapshot.getReference(), "userNamePoster", user.getUserName());
                        }
                    }
                    batchChatAndPost.commit().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            Log.e(TAG, "Update avatar thành công cho cả chatroom và post_photo");
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
    }

    private void updateEmoteAndComment(String id, User user) {
        updateEmoteAndCommentService.execute(() -> db.collection("post_photo")
                .whereArrayContains("friendId", id)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task<QuerySnapshot>> taskList = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        taskList.add(documentSnapshot.getReference().collection("emotes")
                                .whereEqualTo("idUser", id)
                                .get());
                        taskList.add(documentSnapshot.getReference().collection("comments")
                                .whereEqualTo("idUser", id)
                                .get());
                    }
                    if (taskList.isEmpty()) return;
                    Tasks.whenAllSuccess(taskList)
                            .addOnSuccessListener(objects1 -> {
                                WriteBatch batchEmoteAndComment = db.batch();
                                for (Object object : objects1) {
                                    if (object instanceof QuerySnapshot) {
                                        QuerySnapshot querySnapshot = (QuerySnapshot) object;
                                        for (DocumentSnapshot docSnap : querySnapshot) {
                                            if (docSnap.exists()) {
                                                String path = docSnap.getReference().getPath();
                                                Log.e(TAG, "onSuccess: " + path);
                                                if (path.contains("emotes")) {
                                                    batchEmoteAndComment.update(docSnap.getReference(), "urlAvt", user.getAvatarUser());
                                                    batchEmoteAndComment.update(docSnap.getReference(), "username", user.getUserName());
                                                } else if (path.contains("comments")) {
                                                    batchEmoteAndComment.update(docSnap.getReference(), "avtComment", user.getAvatarUser());
                                                    batchEmoteAndComment.update(docSnap.getReference(), "userNameComment", user.getUserName());
                                                }
                                            }
                                        }
                                    }
                                }
                                batchEmoteAndComment.commit().addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Log.e(TAG, "Update avatar thành công cho cả emote và comment");
                                    } else {
                                        Log.e(TAG, "updateRelativeInfo: " + Objects.requireNonNull(task1.getException()).getMessage());
                                    }
                                });
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage())));
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

    public void shutdownService(){
        Log.e(TAG, "shutdownService: ");
        updateEmoteAndCommentService.shutdown();
    }

}