package com.example.connection.work;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import java.util.Objects;

public class DeleteWork extends Worker {
    private final String TAG = "deleteRandomChat";

    public DeleteWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.e(TAG, "doWork: ");
        String chatroomId = getInputData().getString("chatRoomId");
        Log.e(TAG, "doWork: chatid" + chatroomId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch writeBatch = db.batch();
        if (chatroomId != null) {
            db.collection("chatroom")
                    .document(chatroomId)
                    .collection("chats")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for(DocumentSnapshot documentSnapshot : task.getResult()){
                                writeBatch.delete(documentSnapshot.getReference());
                            }
                            writeBatch.delete(db.collection("chatroom").document(chatroomId));
                            writeBatch.commit().addOnCompleteListener(task1 -> {
                                if(!task1.isSuccessful()){
                                    Log.e("deleteRandomChat", "onComplete: " + Objects.requireNonNull(task1.getException()).getMessage());
                                }
                                else {
                                    Log.e(TAG, "doWork: success");
                                }
                            });
                        }
                    });
        }
        return Result.success();
    }

}
