package com.example.connection.repository;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import com.example.connection.listener.OnStateListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OptionsPostRepository {
    private final FirebaseFirestore db;
    private final String TAG = getClass().getName();
    private final OnStateListener onStateListener;
    private final String currentUserId;
    private final ExecutorService downloadPostExecutor = Executors.newSingleThreadExecutor();

    public OptionsPostRepository(OnStateListener onStateListener) {
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        this.onStateListener = onStateListener;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void deletePost(String postId) {
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
                            if (exception != null) {
                                onStateListener.onFailure(exception.getMessage());
                            }
                        } else {
                            onStateListener.onSuccess("deleteSuccess");
                        }
                    });

                });
    }

    public Task<DocumentSnapshot> getPreparePostDelete(String postId) {
        return db.collection("post_photo")
                .document(postId)
                .get();
    }

    public void downloadPost(String url, Context context, OnStateListener onStateListener) {
        downloadPostExecutor.execute(() -> {
            try {
                URL urlDownload = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlDownload.openConnection();
                InputStream inputStream = connection.getInputStream();
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    ContentValues contentValues = new ContentValues();
                    Uri uri = null;
                    if(url.contains(".mp4")){
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
                    if(uri != null){
                        OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                        ghiFile(inputStream, outputStream, onStateListener);
                        if (outputStream != null) {
                            onStateListener.onSuccess("Tải về thành công");
                            outputStream.flush();
                            outputStream.close();
                        }

                    }
                }
                else {
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
                    if(url.contains(".mp4")){
                        videoFile = new File(folderConnection, UUID.randomUUID() + ".mp4");
                        outputStream = new FileOutputStream(videoFile);
                    } else if (url.contains(".jpg")) {
                        photoFile = new File(folderConnection, UUID.randomUUID() + ".jpg");
                        outputStream = new FileOutputStream(photoFile);
                    }
                    ghiFile(inputStream, outputStream, onStateListener);
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        if (url.contains(".mp4") && videoFile != null){
                            intent.setData(Uri.fromFile(videoFile));
                        } else if (url.contains(".jpg") && photoFile != null) {
                            intent.setData(Uri.fromFile(photoFile));
                        }
                        context.sendBroadcast(intent);
                        onStateListener.onSuccess("Tải về thành công");
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "downloadPost: ");
                onStateListener.onFailure(e.getMessage());
            }
        });
    }

    private void ghiFile(InputStream inputStream, OutputStream outputStream, OnStateListener onStateListener) {
        try {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            Log.e(TAG, "ghiFile: ");
            onStateListener.onFailure(e.getMessage());
        }
    }

    public void shutdownExecutor() {
        downloadPostExecutor.shutdown();
    }

}
