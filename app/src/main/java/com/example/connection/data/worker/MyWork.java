package com.example.connection.data.worker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.ui.view.activity.MainActivity;
import com.example.connection.model.Image;
import com.example.connection.model.User;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyWork extends Worker {

    private final String TAG = getClass().getName();

    public MyWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) {
            return Result.success();
        }

        try {
            QuerySnapshot queryDocumentSnapshots = Tasks.await(
                    FirebaseFirestore.getInstance().collection("post_photo")
                            .whereArrayContains("friendId", myId)
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .get(),
                    30, TimeUnit.SECONDS
            );

            List<Image> list = new ArrayList<>();
            Image imageFirst = null;
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                if (doc.exists()) {
                    Image image = doc.toObject(Image.class);
                    if (image != null) {
                        if (imageFirst == null) {
                            imageFirst = image;
                        }
                        List<String> seen = image.getSeen();
                        if (seen != null && !seen.contains(myId)) {
                            list.add(image);
                        }
                    }
                }
            }

            if (imageFirst == null) {
                Log.e(TAG, "Ko có dữ liệu");
                return Result.success();
            }

            DocumentSnapshot userDoc = Tasks.await(
                    FirebaseFirestore.getInstance().collection("users")
                            .document(imageFirst.getPosterId())
                            .get(),
                    30, TimeUnit.SECONDS
            );

            if (!userDoc.exists()) {
                return Result.success();
            }

            User user = userDoc.toObject(User.class);
            if (user == null) {
                return Result.success();
            }

            ComponentName componentName = new ComponentName(context, WidgetProvider.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] widgetIds = manager.getAppWidgetIds(componentName);

            Bitmap postBitmap = null;
            if (imageFirst.getImageUrl() != null && !imageFirst.getImageUrl().isEmpty()) {
                try {
                    postBitmap = Glide.with(context)
                            .asBitmap()
                            .load(imageFirst.getImageUrl())
                            .override(300, 300)
                            .submit()
                            .get(15, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load post image for widget", e);
                }
            }

            Bitmap avtBitmap = null;
            if (user.getAvatarUser() != null && !user.getAvatarUser().isEmpty()) {
                try {
                    avtBitmap = Glide.with(context)
                            .asBitmap()
                            .load(user.getAvatarUser())
                            .override(300, 300)
                            .submit()
                            .get(15, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load avatar image for widget", e);
                }
            }

            for (int widgetId : widgetIds) {
                Intent intent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE);
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget);
                views.setOnClickPendingIntent(R.id.img_widget, pendingIntent);

                if (list.isEmpty()) {
                    views.setViewVisibility(R.id.tv_widget, View.GONE);
                } else if (list.size() > 9) {
                    views.setViewVisibility(R.id.tv_widget, View.VISIBLE);
                    views.setTextViewText(R.id.tv_widget, "9+");
                } else {
                    views.setViewVisibility(R.id.tv_widget, View.VISIBLE);
                    views.setTextViewText(R.id.tv_widget, String.valueOf(list.size()));
                }

                if (imageFirst.getStatus() == null || imageFirst.getStatus().isEmpty()) {
                    views.setViewVisibility(R.id.tv_username_widget, View.GONE);
                } else {
                    views.setViewVisibility(R.id.tv_username_widget, View.VISIBLE);
                    views.setTextViewText(R.id.tv_username_widget, imageFirst.getStatus());
                }

                if (postBitmap != null) {
                    views.setImageViewBitmap(R.id.img_widget, postBitmap);
                }
                if (avtBitmap != null) {
                    views.setImageViewBitmap(R.id.img_avatar_widget, avtBitmap);
                }

                manager.partiallyUpdateAppWidget(widgetId, views);
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "MyWork error: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}
