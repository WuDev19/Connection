package com.example.connection.ui.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.connection.model.Comment;
import com.example.connection.repository.CommentRepository;
import com.google.firebase.firestore.Query;

public class CommentViewModel extends ViewModel {
    private final CommentRepository commentRepository;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final String TAG = getClass().getName();

    public CommentViewModel() {
        commentRepository = new CommentRepository();
    }

    public Query getQuery(String postId) {
        return commentRepository.getQuery(postId);
    }

    public void setComment(String postId, String comment) {
        commentRepository.setComment(postId, comment).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                error.setValue("Có sự cố xảy ra, vui lòng kiểm tra lại kết nối");
            } else {
                commentRepository.increaseNumberComment(postId)
                        .addOnSuccessListener(unused -> Log.e(TAG, "onSuccess: "))
                        .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
            }
        });
    }

    public void deleteComment(String postId, String commentId){
        commentRepository.getComment(postId, commentId).addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Comment comment = task.getResult().toObject(Comment.class);
                if(comment != null){
                    if(comment.getIdUser().equals(commentRepository.getMyId())){
                        commentRepository.deleteComment(postId, commentId)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        commentRepository.decreaseNumberComment(postId)
                                                .addOnSuccessListener(unused -> Log.e(TAG, "onSuccess: "))
                                                .addOnFailureListener(e -> Log.e(TAG, "onFailure: " + e.getMessage()));
                                    } else {
                                        error.setValue("Có sự cố xảy ra, vui lòng kiểm tra lại kết nối");
                                    }
                                });
                    }
                    else {
                        error.setValue("Bạn không thể xóa bình luận của người khác");
                    }
                }
            }
        });

    }

    public LiveData<String> getError() {
        return error;
    }

}
