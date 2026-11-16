package com.example.connection.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.connection.R;
import com.example.connection.ui.view.activity.MainActivity;
import com.example.connection.ui.view.activity.chat.ChatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class ConnectionFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "ConnectionFCM";
    private static final String CHANNEL_ID = "connection_notification_channel";
    private static final String CHANNEL_NAME = "Connection Notifications";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed");
        updateTokenToFirestore(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> event = remoteMessage.getData();
        if ("WIDGET_REFRESH".equals(event.get("type"))) {
            if (com.example.connection.data.worker.WidgetContent.isRefreshFor(event, FirebaseAuth.getInstance().getUid())) {
                com.example.connection.data.worker.WidgetUpdater.requestRefresh(getApplicationContext());
            }
            return;
        }

        String title = null;
        String body = null;

        // Đọc title và body từ Notification payload (nếu có)
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        // Đọc data payload (nếu notification payload null thì lấy từ data)
        Map<String, String> data = remoteMessage.getData();
        if (title == null && data.containsKey("title")) {
            title = data.get("title");
        }
        if (body == null && data.containsKey("body")) {
            body = data.get("body");
        }

        if (title == null) title = getString(R.string.app_name);
        if (body == null) body = "Bạn có thông báo mới";

        sendNotification(title, body, data);
    }

    private void sendNotification(String title, String messageBody, Map<String, String> data) {
        if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        if (!androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()) return;
        Intent intent = new Intent(this, MainActivity.class);
        String chatRoomId = data == null ? null : data.get("chatRoomId");
        if (chatRoomId == null && data != null) chatRoomId = data.get("chat_room_id");
        String type = data == null ? null : data.get("type");
        if (chatRoomId != null && !chatRoomId.isEmpty()) intent.putExtra("chat_room_id", chatRoomId);
        if (type != null) intent.putExtra("type", type);
        String notificationKey = chatRoomId != null ? "chat:" + chatRoomId
                : (type == null ? "notification" : type) + ":" + (data == null ? "" : data.get("senderId"));
        intent.setData(new Uri.Builder().scheme("connection").authority("notification")
                .appendPath(notificationKey).build());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Thông báo tin nhắn và tương tác của Connection");
                channel.enableLights(true);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }

            notificationManager.notify(notificationKey, 0, notificationBuilder.build());
        }
    }

    public static void updateTokenToFirestore(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null && token != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã lưu FCM token lên Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi lưu FCM token: " + e.getMessage()));
        }
    }
}
