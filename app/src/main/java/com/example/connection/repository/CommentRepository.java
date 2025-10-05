package com.example.connection.repository;

import com.example.connection.model.Comment;
import com.example.connection.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class CommentRepository {
    private final FirebaseFirestore db;
    private User currentUser;
    private final String myId;

    public CommentRepository(){
        db = FirebaseFirestore.getInstance();
        myId = FirebaseAuth.getInstance().getUid();
        if(myId != null){
            db.collection("users")
                    .document(myId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            User user = task.getResult().toObject(User.class);
                            if(user != null) {
                                currentUser = user;
                            }
                        }
                    });
        }

    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getMyId() {
        return myId;
    }

    public Query getQuery(String postId){
        return db.collection("post_photo")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    public Task<Void> setComment(String postId, String comment){
        DocumentReference commentRef = db.collection("post_photo")
                .document(postId)
                .collection("comments")
                .document();
        Comment model = new Comment(commentRef.getId(),
                currentUser.getId(),
                currentUser.getAvatarUser(),
                currentUser.getUserName(),
                comment,
                Timestamp.now());
        return db.collection("post_photo")
                .document(postId)
                .collection("comments")
                .document(commentRef.getId())
                .set(model);
    }

    public Task<Void> deleteComment(String postId, String commentId){
        return db.collection("post_photo")
                .document(postId)
                .collection("comments")
                .document(commentId)
                .delete();
    }

    public Task<Void> decreaseNumberComment(String postId){
        DocumentReference postRef = db.collection("post_photo")
                .document(postId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot postSnap = transaction.get(postRef);
            Long count = postSnap.getLong("numberComment");
            long newCount = (count != null ? count : 0) - 1;
            if(newCount < 0) newCount = 0;
            transaction.update(postRef, "numberComment", newCount);
            return null;
        });
    }

    public Task<Void> increaseNumberComment(String postId){
        DocumentReference postRef = db.collection("post_photo")
                .document(postId);
        return db.runTransaction(transaction -> {
            DocumentSnapshot postSnap = transaction.get(postRef);
            Long count = postSnap.getLong("numberComment");
            long newCount = (count != null ? count : 0) + 1;
            transaction.update(postRef, "numberComment", newCount);
            return null;
        });
    }

}
