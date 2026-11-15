package com.example.connection.data.api;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.connection.model.User;
import com.example.connection.model.notification.NotificationRequest;
import com.example.connection.model.notification.NotificationResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    public interface OnNotificationSentListener {
        void onSuccess(int successCount);
        void onFailure(String error);
    }

    public static void sendNotificationToUser(String receiverUserId, String title, String body, Map<String, String> data) {
        sendNotificationToUser(receiverUserId, title, body, data, null);
    }

    public static void sendNotificationToUser(String receiverUserId, String title, String body, Map<String, String> data, OnNotificationSentListener listener) {
        if (receiverUserId == null || receiverUserId.isEmpty()) {
            if (listener != null) listener.onFailure("Người nhận không hợp lệ");
            return;
        }
        sendNotificationToUsers(Collections.singletonList(receiverUserId), title, body, data, listener);
    }

    public static void sendNotificationToUsers(List<String> receiverUserIds, String title, String body, Map<String, String> data) {
        sendNotificationToUsers(receiverUserIds, title, body, data, null);
    }

    public static void sendNotificationToUsers(List<String> receiverUserIds, String title, String body, Map<String, String> data, OnNotificationSentListener listener) {
        if (receiverUserIds == null || receiverUserIds.isEmpty()) {
            if (listener != null) listener.onFailure("Không có người nhận");
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getUid();
        List<String> targetUserIds = new ArrayList<>();
        for (String id : receiverUserIds) {
            if (id != null && !id.trim().isEmpty()) {
                String cleanId = id.endsWith("_delete") ? id.substring(0, id.length() - 7) : id;
                if (!cleanId.equals(currentUserId) && !targetUserIds.contains(cleanId)) {
                    targetUserIds.add(cleanId);
                }
            }
        }

        if (targetUserIds.isEmpty()) {
            if (listener != null) listener.onSuccess(0);
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<String> tokens = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger counter = new AtomicInteger(targetUserIds.size());

        for (String userId : targetUserIds) {
            db.collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            User user = task.getResult().toObject(User.class);
                            if (user != null && user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
                                tokens.add(user.getFcmToken());
                            }
                        }

                        if (counter.decrementAndGet() == 0) {
                            if (!tokens.isEmpty()) {
                                executeSend(tokens, title, body, data, listener);
                            } else {
                                Log.w(TAG, "Không tìm thấy FCM token nào cho người nhận.");
                                if (listener != null) listener.onFailure("Không có FCM token");
                            }
                        }
                    });
        }
    }

    private static void executeSend(List<String> tokens, String title, String body, Map<String, String> data, OnNotificationSentListener listener) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Người dùng chưa đăng nhập, không thể gửi notification.");
            if (listener != null) listener.onFailure("Chưa đăng nhập");
            return;
        }

        currentUser.getIdToken(false).addOnSuccessListener(getTokenResult -> {
            String idToken = getTokenResult.getToken();
            if (idToken == null || idToken.isEmpty()) {
                if (listener != null) listener.onFailure("Không lấy được ID token");
                return;
            }

            String bearerToken = "Bearer " + idToken;
            NotificationRequest request = new NotificationRequest(new ArrayList<>(new java.util.HashSet<>(tokens)), title, body, data);

            ApiClient.getNotificationApiService().sendNotification(bearerToken, request)
                    .enqueue(new Callback<NotificationResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<NotificationResponse> call, @NonNull Response<NotificationResponse> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().getSuccessCount() > 0) {
                                Log.d(TAG, "Bắn thông báo thành công: " + response.body().getSuccessCount() + " thiết bị");
                                if (listener != null) listener.onSuccess(response.body().getSuccessCount());
                            } else {
                                String errorMsg = "HTTP " + response.code() + ": " + response.message();
                                Log.e(TAG, "Backend trả về lỗi: " + errorMsg);
                                if (listener != null) listener.onFailure(errorMsg);
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<NotificationResponse> call, @NonNull Throwable t) {
                            Log.e(TAG, "Lỗi kết nối tới Backend Go: " + t.getMessage());
                            if (listener != null) listener.onFailure(t.getMessage());
                        }
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi lấy ID token: " + e.getMessage());
            if (listener != null) listener.onFailure(e.getMessage());
        });
    }
}
