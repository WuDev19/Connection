package com.example.connection.repository;

import com.example.connection.model.Image;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PostRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration topPostsListener;
    private DocumentSnapshot lastTopDoc;
    private DocumentSnapshot lastPagingDoc;
    private boolean isLastPage = false;
    private boolean isLoading = false;

    public interface OnTopPostsCallback {
        void onNewPosts(List<Image> posts);
    }

    public interface OnLoadMoreCallback {
        void onLoaded(List<Image> posts);
        void onError(Exception e);
    }

    // Lắng nghe Realtime cho các bài mới nhất (mặc định 10 bài)
    public void listenTopPosts(String userId, OnTopPostsCallback callback) {
        listenTopPosts(userId, 10, callback);
    }

    // Lắng nghe Realtime với số lượng bài chỉ định
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

    // Phân trang tải thêm bài cũ (mặc định 10 bài)
    public void loadMorePosts(String userId, OnLoadMoreCallback callback) {
        loadMorePosts(userId, 10, callback);
    }

    // Phân trang tải thêm bài cũ với số lượng chỉ định
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

    public void removeListener() {
        if (topPostsListener != null) {
            topPostsListener.remove();
            topPostsListener = null;
        }
    }
}
