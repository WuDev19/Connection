package com.example.connection.repository.base;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.connection.model.Image;
import com.example.connection.model.SelectionFriendModel;
import com.example.connection.model.User;
import com.example.connection.ui.state.OnStateListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.util.List;

public interface PostRepository {

    // Interfaces
    interface OnTopPostsCallback {
        void onNewPosts(List<Image> posts);
    }

    interface OnLoadMoreCallback {
        void onLoaded(List<Image> posts);
        void onError(Exception e);
    }

    interface NewPostCallBack {
        void updateUI(List<Image> unseenImage);
    }

    interface OnGetListFriendCallback {
        void getListFriend(List<SelectionFriendModel> friends);
        void getListFriendId(List<String> listFriendId);
        void error(String err);
    }

    interface OnUpdateWidget {
        void onUpdate(boolean isUpdate);
    }

    interface OnSendMessage {
        void onSuccess(boolean success);
    }

    String getCurrentUserId();
    String getMyId();
    User getCurrentUser();

    // ==================== Post List & Pagination ====================
    void listenTopPosts(String userId, OnTopPostsCallback callback);
    void listenTopPosts(String userId, int limit, OnTopPostsCallback callback);
    void loadMorePosts(String userId, OnLoadMoreCallback callback);
    void loadMorePosts(String userId, int limit, OnLoadMoreCallback callback);
    void removeListener();

    // ==================== TakePhoto logic ====================
    void getUserInfoFromFirebase(OnGetListFriendCallback callback);
    void unregisterListening();
    void uploadBitmap(Bitmap bitmap, String urlMusic, String name, String singer, String status, List<String> mList, Context context);
    void uploadBitmap(Bitmap bitmap, String urlMusic, String name, String singer, String status, List<String> mList, Context context, OnStateListener listener);
    void uploadVideo(File file, String urlMusic, String name, String singer, String status, List<String> mList);
    void uploadVideo(File file, String urlMusic, String name, String singer, String status, List<String> mList, OnStateListener listener);
    void listenForNewPosts(NewPostCallBack newPostCallBack);
    void downloadPhoto(Bitmap photo, Context context);
    void downloadPhoto(Bitmap photo, Context context, OnStateListener listener);
    void downloadVideo(File videoFile, Context context);
    void downloadVideo(File videoFile, Context context, OnStateListener listener);
    void shutdownExecutor();

    // ==================== ShowPhoto logic ====================
    void updateSeenPostPhoto(Context context, OnUpdateWidget onUpdateWidget);
    void sendMessage(String chatRoomId, String otherId, String otherUserName, String avtPoster, String mess);
    void sendMessage(String chatRoomId, String otherId, String otherUserName, String avtPoster, String mess, OnSendMessage callback);
    String getIdEmote(String postId);
    Task<Void> modifyEmote(String postId, String emote);

    // ==================== Comment logic ====================
    Query getCommentQuery(String postId);
    Task<Void> setComment(String postId, String comment);
    Task<Void> deleteComment(String postId, String commentId);
    Task<DocumentSnapshot> getComment(String postId, String commentId);
    Task<Void> decreaseNumberComment(String postId);
    Task<Void> increaseNumberComment(String postId);

    // ==================== Emote Query logic ====================
    Query getEmoteQuery(String postId);

    // ==================== Options logic ====================
    void deletePost(String postId);
    void deletePost(String postId, OnStateListener listener);
    Task<DocumentSnapshot> getPreparePostDelete(String postId);
    void downloadPost(String url, Context context, OnStateListener listener);
}
