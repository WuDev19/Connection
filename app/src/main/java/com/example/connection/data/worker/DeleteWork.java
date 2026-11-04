package com.example.connection.data.worker;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.concurrent.TimeUnit;

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
        Log.e(TAG, "doWork: chatid " + chatroomId);
        if (chatroomId == null) {
            return Result.success();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        try {
            QuerySnapshot chatsSnapshot = Tasks.await(
                    db.collection("chatroom")
                            .document(chatroomId)
                            .collection("chats")
                            .get(),
                    30, TimeUnit.SECONDS
            );

            WriteBatch writeBatch = db.batch();
            for (DocumentSnapshot documentSnapshot : chatsSnapshot) {
                writeBatch.delete(documentSnapshot.getReference());
            }
            writeBatch.delete(db.collection("chatroom").document(chatroomId));
            Tasks.await(writeBatch.commit(), 30, TimeUnit.SECONDS);
            Log.e(TAG, "doWork: success");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "doWork error: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}
