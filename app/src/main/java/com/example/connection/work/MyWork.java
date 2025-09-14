package com.example.connection.work;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.example.connection.R;
import com.example.connection.view.activity.MainActivity;
import com.example.connection.model.Image;
import com.example.connection.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class MyWork extends Worker {

    private final String TAG = getClass().getName();
    private Image imageFirst;

    public MyWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        scheduleJob(context);
        return Result.success();
    }

    private void scheduleJob(Context context) {
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return;
        List<Image> list = new ArrayList<>();
        FirebaseFirestore.getInstance().collection("post_photo")
                .whereArrayContains("friendId", myId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        tasks.add(documentSnapshot.getReference().get());
                    }
                    Tasks.whenAllComplete(tasks).addOnCompleteListener(task -> {
                        for (int i = 0; i < tasks.size(); i++) {
                            if (tasks.get(i).isSuccessful()) {
                                Image image = tasks.get(i).getResult().toObject(Image.class);
                                if (image != null) {
                                    if (i == 0) imageFirst = image;
                                    List<String> seen = image.getSeen();
                                    if (!seen.contains(myId)) {
                                        list.add(image);
                                    }
                                }
                            }
                        }
                        if (imageFirst == null) {
                            Log.e(TAG, "Ko có dữ liệu");
                            return;
                        }
                        FirebaseFirestore.getInstance().collection("users").document(imageFirst.getPosterId())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        User user = documentSnapshot.toObject(User.class);
                                        if (user != null) {
                                            ComponentName componentName = new ComponentName(context, WidgetProvider.class);
                                            AppWidgetManager manager = AppWidgetManager.getInstance(context);
                                            int[] widgetIds = manager.getAppWidgetIds(componentName);
                                            for (int widgetId : widgetIds) {
                                                Intent intent = new Intent(context, MainActivity.class);
                                                PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE);
                                                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget);
                                                views.setOnClickPendingIntent(R.id.img_widget, pendingIntent);
                                                if (list.isEmpty()) {
                                                    views.setViewVisibility(R.id.tv_widget, View.GONE);
                                                } else if (list.size() > 9) {
                                                    views.setTextViewText(R.id.tv_widget, "9+");
                                                } else
                                                    views.setTextViewText(R.id.tv_widget, String.valueOf(list.size()));
                                                if (imageFirst.getStatus().isEmpty())
                                                    views.setViewVisibility(R.id.tv_username_widget, View.GONE);
                                                else
                                                    views.setTextViewText(R.id.tv_username_widget, imageFirst.getStatus());
                                                AppWidgetTarget post = new AppWidgetTarget(context, R.id.img_widget, views, widgetId);
                                                AppWidgetTarget avt = new AppWidgetTarget(context, R.id.img_avatar_widget, views, widgetId);
                                                Glide.with(context)
                                                        .asBitmap()
                                                        .load(imageFirst.getImageUrl())
                                                        .override(300, 300)
                                                        .into(post);
                                                Glide.with(context)
                                                        .asBitmap()
                                                        .load(user.getAvatarUser())
                                                        .override(300, 300)
                                                        .into(avt);
                                                manager.partiallyUpdateAppWidget(widgetId, views);
                                            }
                                        }
                                    }
                                });
                    });
                });
    }
}
