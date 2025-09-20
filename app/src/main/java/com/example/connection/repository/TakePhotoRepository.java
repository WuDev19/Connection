package com.example.connection.repository;

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
import com.example.connection.listener.OnStateListener;
import com.example.connection.model.Image;
import com.example.connection.model.User;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TakePhotoRepository {

    private final String currentUserId;
    private User user;
    private List<String> mList;
    private final FirebaseFirestore db;
    private ListenerRegistration listenListFriend, postListenerRealTime;
    private final String TAG = getClass().getName();
    private final OnStateListener onStateListener;
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();

    public TakePhotoRepository(OnStateListener onStateListener){
        currentUserId = FirebaseAuth.getInstance().getUid();
        db = FirebaseFirestore.getInstance();
        this.onStateListener = onStateListener;
    }

    public interface NewPostCallBack{
        void updateUI(List<Image> unseenImage);
    }

    public void getUserInfoFromFirebase() {

        if (currentUserId == null) return;

        if(listenListFriend != null){
            listenListFriend.remove();
        }

        listenListFriend = db.collection("users").document(currentUserId)
                .addSnapshotListener((value, error) -> {

                    if(error != null) return;
                    if(value == null) return;

                    user = value.toObject(User.class);
                    if (user != null) {
                        mList = user.getFriend();
                        Log.e(TAG, "onEvent: " + mList);
                    }

                });
    }

    public void unregisterListening(){
        if(listenListFriend != null){
            listenListFriend.remove();
        }
        if(postListenerRealTime != null){
            postListenerRealTime.remove();
        }
    }

    public void uploadBitmap(Bitmap bitmap, String urlMusic, String name, String singer, String status, Context context) {
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
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String urlImageCloudinary = (String) resultData.get("secure_url");
                            List<String> list = new ArrayList<>();
                            list.add(currentUserId);
                            Image image = new Image(mList, urlImageCloudinary, null, status, currentUserId,
                                    Timestamp.now(), list, urlMusic, name, singer, user.getUserName(),
                                    user.getAvatarUser());
                            Log.e("status", "onSuccess: " + status);
                            db.collection("post_photo")
                                    .add(image)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            onStateListener.onSuccess("Đăng thành công");
                                            if(file.delete()) Log.e(TAG, "onSuccess: xoa cache thanh cong");
                                        } else {
                                            Exception exception = task.getException();
                                            if(exception != null){
                                                onStateListener.onFailure(task.getException().getMessage());
                                            }
                                        }
                                    });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e("CLOUDINARY", "Upload error: " + error.getDescription());
                            onStateListener.onFailure(error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                        }
                    })
                    .dispatch();

        } catch (IOException e) {
            Log.e("CLOUDINARY", Objects.requireNonNull(e.getMessage()));
        }
    }

    public void uploadVideo(File file, String urlMusic, String name, String singer, String status){
        MediaManager.get().upload(file.getAbsolutePath())
                .unsigned("post_upload")
                .option("resource_type", "video")
                .option("folder", "post/")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {

                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {

                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String urlVideoCloudinary = (String) resultData.get("secure_url");
                        Log.e("fileVideo", "onSuccess: " + urlVideoCloudinary);
                        List<String> list = new ArrayList<>();
                        list.add(currentUserId);
                        Image image = new Image(mList, null, urlVideoCloudinary, status, currentUserId,
                                Timestamp.now(), list, urlMusic, name, singer, user.getUserName(),
                                user.getAvatarUser());
                        Log.e("status", "onSuccess: " + status);
                        db.collection("post_photo")
                                .add(image)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        onStateListener.onSuccess("Đăng thành công");
                                        if(file.delete()) Log.e(TAG, "onSuccess: xoa cache thanh cong");
                                    } else {
                                        Exception exception = task.getException();
                                        if(exception != null){
                                            onStateListener.onFailure(task.getException().getMessage());
                                        }
                                    }
                                });
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        onStateListener.onFailure(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {

                    }
                }).dispatch();
    }

    public void createFolderConnectionExternalStorage(Bitmap photo, Context context) {
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
                    onStateListener.onSuccess("Lưu ảnh thành công");
                } catch (IOException e) {
                    onStateListener.onFailure(e.getMessage());
                }
            } catch (Exception e) {
                onStateListener.onFailure(e.getMessage());
            }
        });
    }

    public void listenForNewPosts(NewPostCallBack newPostCallBack) {
        if (currentUserId == null) return;

        if (postListenerRealTime != null) {
            postListenerRealTime.remove(); //chi de mot listener lang nghe
        }

        //toi uu listening
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

    public void downloadPhoto(Bitmap photo, Context context){
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
                    onStateListener.onSuccess("Lưu ảnh thành công");
                }
            } catch (IOException e) {
                onStateListener.onFailure(e.getMessage());
            }
        });
    }

    public void downloadVideo(File videoFile, Context context){
        downloadExecutor.execute(() -> {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, UUID.randomUUID() + ".mp4");
                contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Connection");
                Uri uri = context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
                if(uri != null){
                    try{
                        OutputStream outStream = context.getContentResolver().openOutputStream(uri);
                        downloadFile(videoFile, outStream);
                    }
                    catch (IOException e){
                        onStateListener.onFailure(e.getMessage());
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
                File video = new File(folderConnection, UUID.randomUUID() + ".mp4");
                try{
                    FileOutputStream outputStream = new FileOutputStream(video);
                    downloadFile(videoFile, outputStream);
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(Uri.fromFile(video));
                    context.sendBroadcast(scanIntent);
                }
                catch (IOException e){
                    onStateListener.onFailure(e.getMessage());
                }
            }
        });
    }

    public void shutdownExecutor(){
        downloadExecutor.shutdown();
    }

    public void downloadFile(File videoFile, OutputStream outStream){
        try {
            if (outStream != null) {
                Log.e("download", "downloadVideo: stream");
                FileInputStream fileInputStream = new FileInputStream(videoFile);
                byte[] buffer = new byte[1024];
                int len;
                while((len = fileInputStream.read(buffer)) > 0){
                    outStream.write(buffer, 0, len);
                }
                outStream.flush();
                outStream.close();
                fileInputStream.close();
                onStateListener.onSuccess("Lưu video thành công");
            }
        }
        catch (IOException e){
            onStateListener.onFailure(e.getMessage());
        }
    }

}
