package com.example.connection.repository.impl;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.connection.constants.SharePreUtil;
import com.example.connection.model.ChatMessage;
import com.example.connection.model.ChatRoom;
import com.example.connection.model.Comment;
import com.example.connection.model.EmoteModel;
import com.example.connection.model.Image;
import com.example.connection.model.SelectionFriendModel;
import com.example.connection.model.User;
import com.example.connection.repository.base.PostRepository;
import com.example.connection.ui.state.OnStateListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PostRepositoryImpl implements PostRepository {

    private final String TAG = getClass().getName();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String currentUserId;
    private User currentUser;

    private OnStateListener onStateListener;
    private OnSendMessage onSendMessage;

    // Listeners & Paging state
    private ListenerRegistration topPostsListener;
    private ListenerRegistration listenListFriend;
    private ListenerRegistration postListenerRealTime;
    private DocumentSnapshot lastTopDoc;
    private DocumentSnapshot lastPagingDoc;
    private boolean isLastPage = false;
    private boolean isLoading = false;

    // TakePhoto helper cache & thread pool
    private final ExecutorService downloadExecutor = Executors.newFixedThreadPool(2);
    private final Map<String, SelectionFriendModel> cacheFriend = new HashMap<>();
    private List<String> listFriendId = new ArrayList<>();
    private final List<SelectionFriendModel> listChooseFriend = new ArrayList<>();

    // ShowPhoto helper state
    private ChatRoom chatRoom;

    // Constructors
    public PostRepositoryImpl() {
        this.currentUserId = FirebaseAuth.getInstance().getUid();
        initCurrentUser();
    }

    public PostRepositoryImpl(OnStateListener onStateListener) {
        this.currentUserId = FirebaseAuth.getInstance().getUid();
        this.onStateListener = onStateListener;
        initCurrentUser();
    }

    public PostRepositoryImpl(OnSendMessage onSendMessage) {
        this.currentUserId = FirebaseAuth.getInstance().getUid();
        this.onSendMessage = onSendMessage;
        initCurrentUser();
    }

    private void initCurrentUser() {
        if (currentUserId != null) {
            db.collection("users").document(currentUserId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            User user = task.getResult().toObject(User.class);
                            if (user != null) {
                                currentUser = user;
                            }
                        }
                    });
        }
    }

    @Override
    public String getCurrentUserId() {
        return currentUserId;
    }

    @Override
    public String getMyId() {
        return currentUserId;
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }

    // ==================== Post List & Pagination (PostRepository) ====================

    @Override
    public void listenTopPosts(String userId, OnTopPostsCallback callback) {
        listenTopPosts(userId, 10, callback);
    }

    @Override
    public void listenTopPosts(String userId, int limit, OnTopPostsCallback callback) {
        if (topPostsListener != null) {
            topPostsListener.remove();
        }
        isLastPage = false;
        lastPagingDoc = null;
        topPostsListener = db.collection("post_photo")
                .whereArrayContains("friendId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    List<Image> topPosts = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Image post = doc.toObject(Image.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            topPosts.add(post);
                        }
                    }
                    if (!snapshots.isEmpty()) {
                        lastTopDoc = snapshots.getDocuments().get(snapshots.size() - 1);
                    }
                    callback.onNewPosts(topPosts);
                });
    }

    @Override
    public void loadMorePosts(String userId, OnLoadMoreCallback callback) {
        loadMorePosts(userId, 10, callback);
    }

    @Override
    public void loadMorePosts(String userId, int limit, OnLoadMoreCallback callback) {
        DocumentSnapshot cursor = lastPagingDoc != null ? lastPagingDoc : lastTopDoc;
        if (isLoading || isLastPage || cursor == null) return;
        isLoading = true;

        db.collection("post_photo")
                .whereArrayContains("friendId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(cursor)
                .limit(limit)
                .get()
                .addOnSuccessListener(snapshots -> {
                    isLoading = false;
                    if (snapshots.isEmpty()) {
                        isLastPage = true;
                        callback.onLoaded(Collections.emptyList());
                        return;
                    }
                    lastPagingDoc = snapshots.getDocuments().get(snapshots.size() - 1);
                    List<Image> olderPosts = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots) {
                        Image post = doc.toObject(Image.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            olderPosts.add(post);
                        }
                    }
                    callback.onLoaded(olderPosts);
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    callback.onError(e);
                });
    }

    @Override
    public void removeListener() {
        if (topPostsListener != null) {
            topPostsListener.remove();
            topPostsListener = null;
        }
    }

    // ==================== TakePhoto logic (TakePhotoRepository) ====================

    @Override
    public void getUserInfoFromFirebase(OnGetListFriendCallback callback) {
        if (currentUserId == null || callback == null) return;

        if (listenListFriend != null) {
            listenListFriend.remove();
        }

        listenListFriend = db.collection("users").document(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    currentUser = value.toObject(User.class);
                    if (currentUser != null && currentUser.getFriend() != null) {
                        if (!currentUser.getFriend().equals(listFriendId)) {
                            for (String id : listFriendId) {
                                if (!currentUser.getFriend().contains(id)) {
                                    cacheFriend.remove(id);
                                }
                            }
                            listFriendId = currentUser.getFriend();
                            callback.getListFriendId(listFriendId);
                            listChooseFriend.clear();
                            List<Task<DocumentSnapshot>> tasksList = new ArrayList<>();
                            for (String id : listFriendId) {
                                if (cacheFriend.containsKey(id)) {
                                    SelectionFriendModel model = cacheFriend.get(id);
                                    if (model != null) {
                                        model.setSelected(false);
                                        listChooseFriend.add(model);
                                    }
                                } else {
                                    Task<DocumentSnapshot> task = db.collection("users").document(id).get();
                                    tasksList.add(task);
                                }
                            }
                            Tasks.whenAllSuccess(tasksList).addOnSuccessListener(objects -> {
                                for (Task<DocumentSnapshot> task : tasksList) {
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        User userTmp = task.getResult().toObject(User.class);
                                        if (userTmp != null) {
                                            String avt = userTmp.getAvatarUser();
                                            String userTmpId = userTmp.getId();
                                            String username = userTmp.getUserName();
                                            SelectionFriendModel model = new SelectionFriendModel(userTmpId, avt, username, false);
                                            listChooseFriend.add(model);
                                            cacheFriend.put(userTmpId, model);
                                        }
                                    }
                                }
                                callback.getListFriend(listChooseFriend);
                            });
                        }
                    }
                });
    }

    @Override
    public void unregisterListening() {
        if (listenListFriend != null) {
            listenListFriend.remove();
            listenListFriend = null;
        }
        if (postListenerRealTime != null) {
            postListenerRealTime.remove();
            postListenerRealTime = null;
        }
    }

    @Override
    public void uploadBitmap(Bitmap bitmap, String urlMusic, String name, String singer, String status, List<String> mList, Context context) {
        uploadBitmap(bitmap, urlMusic, name, singer, status, mList, context, this.onStateListener);
    }

    @Override
    public void uploadBitmap(Bitmap bitmap, String urlMusic, String name, String singer, String status, List<String> mList, Context context, OnStateListener listener) {
        try {
            Log.e("status", "uploadBitmap: " + status);
            File file = new File(context.getCacheDir(), "temp_img.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, fos);
            fos.close();
            MediaManager.get().upload(file.getAbsolutePath())
                    .unsigned("post_upload")
                    .option("folder", "post/")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d("CLOUDINARY", "Upload started");
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String urlImageCloudinary = (String) resultData.get("secure_url");
                            List<String> list = new ArrayList<>();
                            list.add(currentUserId);
                            DocumentReference postRef = db.collection("post_photo").document();
                            String userName = (currentUser != null) ? currentUser.getUserName() : "";
                            String userAvt = (currentUser != null) ? currentUser.getAvatarUser() : "";
                            Image image = new Image(postRef.getId(), mList, urlImageCloudinary, null, status, currentUserId,
                                    Timestamp.now(), list, urlMusic, name, singer, userName,
                                    userAvt, 0, 0, new HashMap<>());
                            postRef.set(image)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            if (listener != null) listener.onSuccess("Đăng thành công");
                                            if (file.delete())
                                                Log.e(TAG, "onSuccess: xoa cache thanh cong");
                                        } else {
                                            Exception exception = task.getException();
                                            if (exception != null && listener != null) {
                                                listener.onFailure(exception.getMessage());
                                            }
                                        }
                                    });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e("CLOUDINARY", "Upload error: " + error.getDescription());
                            if (listener != null) listener.onFailure(error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {}
                    })
                    .dispatch();

        } catch (IOException e) {
            Log.e("CLOUDINARY", Objects.requireNonNull(e.getMessage()));
            if (listener != null) listener.onFailure(e.getMessage());
        }
    }

    @Override
    public void uploadVideo(File file, String urlMusic, String name, String singer, String status, List<String> mList) {
        uploadVideo(file, urlMusic, name, singer, status, mList, this.onStateListener);
    }

    @Override
    public void uploadVideo(File file, String urlMusic, String name, String singer, String status, List<String> mList, OnStateListener listener) {
        if (file == null) return;
        MediaManager.get().upload(file.getAbsolutePath())
                .unsigned("post_upload")
                .option("resource_type", "video")
                .option("folder", "post/")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String urlVideoCloudinary = (String) resultData.get("secure_url");
                        Log.e("fileVideo", "onSuccess: " + urlVideoCloudinary);
                        List<String> list = new ArrayList<>();
                        list.add(currentUserId);
                        DocumentReference postRef = db.collection("post_photo").document();
                        String userName = (currentUser != null) ? currentUser.getUserName() : "";
                        String userAvt = (currentUser != null) ? currentUser.getAvatarUser() : "";
                        Image image = new Image(postRef.getId(), mList, null, urlVideoCloudinary, status, currentUserId,
                                Timestamp.now(), list, urlMusic, name, singer, userName,
                                userAvt, 0, 0, new HashMap<>());
                        postRef.set(image)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        if (listener != null) listener.onSuccess("Đăng thành công");
                                        if (file.delete())
                                            Log.e(TAG, "onSuccess: xoa cache thanh cong");
                                    } else {
                                        Exception exception = task.getException();
                                        if (exception != null && listener != null) {
                                            listener.onFailure(exception.getMessage());
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (listener != null) listener.onFailure(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    @Override
    public void createFolderConnectionExternalStorage(Bitmap photo, Context context) {
        createFolderConnectionExternalStorage(photo, context, this.onStateListener);
    }

    @Override
    public void createFolderConnectionExternalStorage(Bitmap photo, Context context, OnStateListener listener) {
        downloadExecutor.execute(() -> {
            try {
                File folderConnection = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Connection");
                if (!folderConnection.exists()) {
                    boolean init = folderConnection.mkdirs();
                    if (init) {
                        Log.e(TAG, "Tạo folder thành công");
                    } else {
                        Log.e(TAG, "Tạo folder không thành công");
                    }
                } else {
                    Log.e(TAG, "Folder đã tồn tại");
                }
                String uID = UUID.randomUUID().toString();
                File file = new File(folderConnection, uID + ".jpg");
                Log.e(TAG, file.getAbsolutePath());
                FileOutputStream fileOutputStream;
                try {
                    fileOutputStream = new FileOutputStream(file);
                    photo.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(file));
                    context.sendBroadcast(intent);
                    if (listener != null) listener.onSuccess("Lưu ảnh thành công");
                } catch (IOException e) {
                    if (listener != null) listener.onFailure(e.getMessage());
                }
            } catch (Exception e) {
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    @Override
    public void listenForNewPosts(NewPostCallBack newPostCallBack) {
        if (currentUserId == null || newPostCallBack == null) return;

        if (postListenerRealTime != null) {
            postListenerRealTime.remove();
        }

        postListenerRealTime = db.collection("post_photo")
                .whereArrayContains("friendId", currentUserId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Lỗi khi lắng nghe bài viết: " + e.getMessage());
                        return;
                    }
                    if (queryDocumentSnapshots == null) return;
                    List<Image> unseenPosts = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Image image = documentSnapshot.toObject(Image.class);
                        if (image != null) {
                            List<String> seenUsers = image.getSeen();
                            if (seenUsers == null || !seenUsers.contains(currentUserId)) {
                                unseenPosts.add(image);
                            }
                        }
                    }
                    newPostCallBack.updateUI(unseenPosts);
                });
    }

    @Override
    public void downloadPhoto(Bitmap photo, Context context) {
        downloadPhoto(photo, context, this.onStateListener);
    }

    @Override
    public void downloadPhoto(Bitmap photo, Context context, OnStateListener listener) {
        downloadExecutor.execute(() -> {
            String fileName = UUID.randomUUID().toString();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, fileName + ".jpg");
            contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Connection");
            Uri uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            try {
                if (uri != null) {
                    OutputStream outStream = context.getContentResolver().openOutputStream(uri);
                    if (outStream != null) {
                        photo.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                        outStream.flush();
                        outStream.close();
                    }
                    if (listener != null) listener.onSuccess("Lưu ảnh thành công");
                }
            } catch (IOException e) {
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    @Override
    public void downloadVideo(File videoFile, Context context) {
        downloadVideo(videoFile, context, this.onStateListener);
    }

    @Override
    public void downloadVideo(File videoFile, Context context, OnStateListener listener) {
        downloadExecutor.execute(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, UUID.randomUUID() + ".mp4");
                contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Connection");
                Uri uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (uri != null) {
                    try {
                        OutputStream outStream = context.getContentResolver().openOutputStream(uri);
                        downloadFile(videoFile, outStream, listener);
                    } catch (IOException e) {
                        if (listener != null) listener.onFailure(e.getMessage());
                    }
                }
            } else {
                File folderConnection = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Connection");
                if (!folderConnection.exists()) {
                    boolean init = folderConnection.mkdirs();
                    if (init) {
                        Log.e(TAG, "Tạo folder thành công");
                    } else {
                        Log.e(TAG, "Tạo folder không thành công");
                    }
                } else {
                    Log.e(TAG, "Folder đã tồn tại");
                }
                File video = new File(folderConnection, UUID.randomUUID() + ".mp4");
                try {
                    FileOutputStream outputStream = new FileOutputStream(video);
                    downloadFile(videoFile, outputStream, listener);
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(Uri.fromFile(video));
                    context.sendBroadcast(scanIntent);
                } catch (IOException e) {
                    if (listener != null) listener.onFailure(e.getMessage());
                }
            }
        });
    }

    private void downloadFile(File videoFile, OutputStream outStream, OnStateListener listener) {
        try {
            if (outStream != null) {
                Log.e("download", "downloadVideo: stream");
                FileInputStream fileInputStream = new FileInputStream(videoFile);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fileInputStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, len);
                }
                outStream.flush();
                outStream.close();
                fileInputStream.close();
                if (listener != null) listener.onSuccess("Lưu video thành công");
            }
        } catch (IOException e) {
            if (listener != null) listener.onFailure(e.getMessage());
        }
    }

    @Override
    public void shutdownExecutor() {
        downloadExecutor.shutdown();
    }

    // ==================== ShowPhoto logic (ShowPhotoRepository) ====================

    @Override
    public void updateSeenPostPhoto(Context context, OnUpdateWidget onUpdateWidget) {
        if (currentUserId == null) return;
        Log.e(TAG, "updateSeenPostPhoto: ");
        db.collection("post_photo")
                .whereArrayContains("friendId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Image image = documentSnapshot.toObject(Image.class);
                        if (image == null) continue;
                        List<String> list = image.getSeen();
                        if (list == null || !list.contains(currentUserId)) {
                            documentSnapshot.getReference().update("seen", FieldValue.arrayUnion(currentUserId))
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.e(TAG, "Cập nhật seen thành công");
                                        } else if (task1.getException() != null) {
                                            Log.e(TAG, task1.getException().getMessage());
                                        }
                                    });
                        }
                    }
                    if (SharePreUtil.getInstance(context.getApplicationContext()).enableWidget()) {
                        if (onUpdateWidget != null) onUpdateWidget.onUpdate(true);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
    }

    @Override
    public Query getQuery() {
        if (currentUserId == null) return null;
        return db.collection("post_photo")
                .whereArrayContains("friendId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10);
    }

    @Override
    public void sendMessage(String chatRoomId, String otherId, String otherUserName, String avtPoster, String mess) {
        sendMessage(chatRoomId, otherId, otherUserName, avtPoster, mess, this.onSendMessage);
    }

    @Override
    public void sendMessage(String chatRoomId, String otherId, String otherUserName, String avtPoster, String mess, OnSendMessage callback) {
        db.collection("chatroom").document(chatRoomId)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        chatRoom = task.getResult().toObject(ChatRoom.class);
                        if (chatRoom == null) {
                            Map<String, Long> mapDelete = new HashMap<>();
                            mapDelete.put(currentUserId, Timestamp.now().toDate().getTime());
                            mapDelete.put(otherId, Timestamp.now().toDate().getTime());

                            Map<String, String> avtMap = new HashMap<>();
                            avtMap.put(otherId, avtPoster);
                            String myAvt = (currentUser != null) ? currentUser.getAvatarUser() : "";
                            String myName = (currentUser != null) ? currentUser.getUserName() : "";
                            avtMap.put(currentUserId, myAvt);

                            Map<String, String> aliasMap = new HashMap<>();
                            aliasMap.put(otherId, otherUserName);
                            aliasMap.put(currentUserId, myName);

                            List<String> searchChat = new ArrayList<>();
                            searchChat.add(otherUserName.toLowerCase());
                            searchChat.add(myName.toLowerCase());

                            chatRoom = new ChatRoom(chatRoomId,
                                    null,
                                    null,
                                    Arrays.asList(currentUserId, otherId),
                                    null,
                                    null,
                                    null,
                                    mapDelete,
                                    avtMap,
                                    aliasMap,
                                    searchChat,
                                    "",
                                    0);
                        }
                        sendMessage1(mess, chatRoomId, currentUserId, otherId, callback);
                    }
                });
    }

    private void sendMessage1(String message, String chatRoomId, String currentUserId, String otherUserId, OnSendMessage callback) {
        List<String> userId = chatRoom.getUserId();
        String idDelete = (userId == null || userId.isEmpty()) ? null : userId.get(0).contains("delete") ? userId.get(0) : (userId.size() > 1 ? userId.get(1) : null);
        if (idDelete != null && idDelete.contains("delete")) {
            chatRoom.setUserId(Arrays.asList(currentUserId, otherUserId));
            Map<String, Long> map = chatRoom.getDeleteChat();
            if (map != null) {
                Long timeCurrent = map.get(currentUserId);
                Long timeOther = map.get(otherUserId);
                if (timeCurrent != null && timeOther != null) {
                    if (timeCurrent > timeOther) {
                        map.put(currentUserId, Timestamp.now().toDate().getTime());
                    } else {
                        map.put(otherUserId, Timestamp.now().toDate().getTime());
                    }
                    chatRoom.setDeleteChat(map);
                }
            }
        }
        chatRoom.setLastMessageSenderId(currentUserId);
        chatRoom.setLastMessageTimestamp(Timestamp.now());
        chatRoom.setLastMessage(message);

        Map<String, Long> unread = chatRoom.getUnreadCounts();
        if (unread == null) unread = new HashMap<>();
        long currentUnread = unread.getOrDefault(otherUserId, 0L);
        unread.put(otherUserId, currentUnread + 1);
        chatRoom.setUnreadCounts(unread);

        db.collection("chatroom").document(chatRoomId).set(chatRoom);

        DocumentReference docRefChatMessage = getChatroomMessageReference(chatRoomId).document();
        ChatMessage chatMessage = new ChatMessage(docRefChatMessage.getId(), message, currentUserId, Timestamp.now().toDate().getTime(), false, new HashMap<>());
        docRefChatMessage.set(chatMessage).addOnCompleteListener(task -> {
            if (task.isSuccessful() && callback != null) {
                callback.onSuccess(true);
            }
        });
    }

    private CollectionReference getChatroomMessageReference(String chatRoomId) {
        return db.collection("chatroom").document(chatRoomId).collection("chats");
    }

    @Override
    public String getIdEmote(String postId) {
        DocumentReference emoteRef = db.collection("post_photo").document(postId).collection("emotes").document();
        return emoteRef.getId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Task<Void> modifyEmote(String postId, String emote) {
        DocumentReference postRef = db.collection("post_photo").document(postId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot postSnap = transaction.get(postRef);
            Map<String, String> emoteMap = (Map<String, String>) postSnap.get("emoteMap");
            Long count = postSnap.getLong("numberEmote");
            if (emoteMap != null) {
                if (emoteMap.containsKey(currentUserId)) {
                    String emoteIdAndEmote = emoteMap.get(currentUserId);
                    if (emoteIdAndEmote != null) {
                        String emoteId = emoteIdAndEmote.split("_")[0];
                        DocumentReference emoteRef = db.collection("post_photo").document(postId).collection("emotes").document(emoteId);
                        DocumentSnapshot emoteSnap = transaction.get(emoteRef);
                        String oldEmote = emoteIdAndEmote.split("_")[1];
                        if (oldEmote.equals(emote)) {
                            Log.e(TAG, "modifyEmote: xoa");
                            emoteMap.remove(currentUserId);
                            long newCount = (count != null ? count : 0) - 1;
                            if (newCount < 0) newCount = 0;
                            if (emoteSnap.exists()) {
                                transaction.delete(emoteRef);
                            }
                            transaction.update(postRef, "numberEmote", newCount);
                            transaction.update(postRef, "emoteMap", emoteMap);
                        } else {
                            Log.e(TAG, "modifyEmote: cap nhat");
                            transaction.update(emoteRef, "emote", emote);
                            transaction.update(emoteRef, "timestamp", Timestamp.now());
                            transaction.update(postRef, "emoteMap." + currentUserId, emoteId + "_" + emote);
                        }
                    }
                } else {
                    Log.e(TAG, "modifyEmote: tao moi");
                    String emoteId = getIdEmote(postId);
                    DocumentReference emoteRef = db.collection("post_photo").document(postId).collection("emotes").document(emoteId);
                    String myAvt = (currentUser != null) ? currentUser.getAvatarUser() : "";
                    String myName = (currentUser != null) ? currentUser.getUserName() : "";
                    EmoteModel model = new EmoteModel(emoteId,
                            currentUserId,
                            myAvt,
                            myName,
                            emote,
                            Timestamp.now());
                    long newCount = (count != null ? count : 0) + 1;
                    transaction.update(postRef, "numberEmote", newCount);
                    transaction.update(postRef, "emoteMap." + currentUserId, model.getIdEmote() + "_" + emote);
                    transaction.set(emoteRef, model);
                }
            }
            return null;
        });
    }

    // ==================== Comment logic (CommentRepository) ====================

    @Override
    public Query getCommentQuery(String postId) {
        return db.collection("post_photo")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    @Override
    public Task<Void> setComment(String postId, String comment) {
        DocumentReference commentRef = db.collection("post_photo")
                .document(postId)
                .collection("comments")
                .document();
        String myAvt = (currentUser != null) ? currentUser.getAvatarUser() : "";
        String myName = (currentUser != null) ? currentUser.getUserName() : "";
        Comment model = new Comment(commentRef.getId(),
                currentUserId,
                myAvt,
                myName,
                comment,
                Timestamp.now());
        return db.collection("post_photo")
                .document(postId)
                .collection("comments")
                .document(commentRef.getId())
                .set(model);
    }

    @Override
    public Task<Void> deleteComment(String postId, String commentId) {
        return db.collection("post_photo")
                .document(postId)
                .collection("comments")
                .document(commentId)
                .delete();
    }

    @Override
    public Task<DocumentSnapshot> getComment(String postId, String commentId) {
        return db.collection("post_photo")
                .document(postId)
                .collection("comments")
                .document(commentId)
                .get();
    }

    @Override
    public Task<Void> decreaseNumberComment(String postId) {
        DocumentReference postRef = db.collection("post_photo").document(postId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot postSnap = transaction.get(postRef);
            Long count = postSnap.getLong("numberComment");
            long newCount = (count != null ? count : 0) - 1;
            if (newCount < 0) newCount = 0;
            transaction.update(postRef, "numberComment", newCount);
            return null;
        });
    }

    @Override
    public Task<Void> increaseNumberComment(String postId) {
        DocumentReference postRef = db.collection("post_photo").document(postId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot postSnap = transaction.get(postRef);
            Long count = postSnap.getLong("numberComment");
            long newCount = (count != null ? count : 0) + 1;
            transaction.update(postRef, "numberComment", newCount);
            return null;
        });
    }

    // ==================== Emote Query logic (EmotePostRepository) ====================

    @Override
    public Query getEmoteQuery(String postId) {
        return db.collection("post_photo")
                .document(postId)
                .collection("emotes")
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    // ==================== Options logic (OptionsPostRepository) ====================

    @Override
    public void deletePost(String postId) {
        deletePost(postId, this.onStateListener);
    }

    @Override
    public void deletePost(String postId, OnStateListener listener) {
        WriteBatch writeBatch = db.batch();
        Task<QuerySnapshot> taskEmote = db.collection("post_photo")
                .document(postId)
                .collection("emotes")
                .get();

        Task<QuerySnapshot> taskComment = db.collection("post_photo")
                .document(postId)
                .collection("comments")
                .get();

        Tasks.whenAllSuccess(taskEmote, taskComment)
                .addOnSuccessListener(objects -> {
                    QuerySnapshot queryEmote = (QuerySnapshot) objects.get(0);
                    QuerySnapshot queryComment = (QuerySnapshot) objects.get(1);

                    for (DocumentSnapshot documentSnapshot : queryEmote) {
                        writeBatch.delete(documentSnapshot.getReference());
                    }

                    for (DocumentSnapshot documentSnapshot : queryComment) {
                        writeBatch.delete(documentSnapshot.getReference());
                    }

                    writeBatch.delete(db.collection("post_photo").document(postId));

                    writeBatch.commit().addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "onComplete: " + Objects.requireNonNull(task.getException()).getMessage());
                            Exception exception = task.getException();
                            if (exception != null && listener != null) {
                                listener.onFailure(exception.getMessage());
                            }
                        } else {
                            if (listener != null) listener.onSuccess("deleteSuccess");
                        }
                    });
                });
    }

    @Override
    public Task<DocumentSnapshot> getPreparePostDelete(String postId) {
        return db.collection("post_photo")
                .document(postId)
                .get();
    }

    @Override
    public void downloadPost(String url, Context context, OnStateListener listener) {
        downloadExecutor.execute(() -> {
            try {
                URL urlDownload = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
                InputStream inputStream = connection.getInputStream();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues contentValues = new ContentValues();
                    Uri uri = null;
                    if (url.contains(".mp4")) {
                        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, UUID.randomUUID() + ".mp4");
                        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Connection");
                        uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
                    } else if (url.contains(".jpg")) {
                        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, UUID.randomUUID() + ".jpg");
                        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Connection");
                        uri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                    }
                    if (uri != null) {
                        OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                        ghiFile(inputStream, outputStream, listener);
                        if (outputStream != null) {
                            if (listener != null) listener.onSuccess("Tải về thành công");
                            outputStream.flush();
                            outputStream.close();
                        }
                    }
                } else {
                    File folderConnection = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Connection");
                    if (!folderConnection.exists()) {
                        boolean init = folderConnection.mkdirs();
                        if (init) {
                            Log.e(TAG, "Tạo folder thành công");
                        } else {
                            Log.e(TAG, "Tạo folder không thành công");
                        }
                    } else {
                        Log.e(TAG, "Folder đã tồn tại");
                    }
                    FileOutputStream outputStream = null;
                    File photoFile = null, videoFile = null;
                    if (url.contains(".mp4")) {
                        videoFile = new File(folderConnection, UUID.randomUUID() + ".mp4");
                        outputStream = new FileOutputStream(videoFile);
                    } else if (url.contains(".jpg")) {
                        photoFile = new File(folderConnection, UUID.randomUUID() + ".jpg");
                        outputStream = new FileOutputStream(photoFile);
                    }
                    ghiFile(inputStream, outputStream, listener);
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        if (url.contains(".mp4") && videoFile != null) {
                            intent.setData(Uri.fromFile(videoFile));
                        } else if (url.contains(".jpg") && photoFile != null) {
                            intent.setData(Uri.fromFile(photoFile));
                        }
                        context.sendBroadcast(intent);
                        if (listener != null) listener.onSuccess("Tải về thành công");
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "downloadPost: ");
                if (listener != null) listener.onFailure(e.getMessage());
            }
        });
    }

    private void ghiFile(InputStream inputStream, OutputStream outputStream, OnStateListener listener) {
        try {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            Log.e(TAG, "ghiFile: ");
            if (listener != null) listener.onFailure(e.getMessage());
        }
    }
}
