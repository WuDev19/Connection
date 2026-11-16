package com.example.connection.data.worker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.example.connection.R;
import com.example.connection.model.Image;
import com.example.connection.ui.view.activity.MainActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/** The only renderer for all widget instances, regardless of the update trigger. */
public final class WidgetUpdater {
    private static final String IMMEDIATE_WORK = "widget-refresh";
    private static final String PERIODIC_WORK = "getPost";
    private static final Object PUBLISH_LOCK = new Object();
    private static final AtomicLong RENDER_SEQUENCE = new AtomicLong();

    public static int[] widgetIds(Context context) {
        return AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
    }

    public static boolean hasWidgets(Context context) { return widgetIds(context).length > 0; }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("widget_session", Context.MODE_PRIVATE);
    }

    public static void accountChanged(Context context) {
        synchronized (PUBLISH_LOCK) {
            String uid = FirebaseAuth.getInstance().getUid();
            SharedPreferences prefs = preferences(context);
            if (Objects.equals(prefs.getString("uid", null), uid)) return;
            prefs.edit().putString("uid", uid).putLong("epoch", prefs.getLong("epoch", 0) + 1).apply();
            RENDER_SEQUENCE.incrementAndGet();
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK);
            for (int id : widgetIds(context)) initialize(context, id);
        }
    }

    public static void initialize(Context context, int id) {
        synchronized (PUBLISH_LOCK) {
            RemoteViews views = baseViews(context, id);
            views.setTextViewText(R.id.widget_message, context.getString(FirebaseAuth.getInstance().getUid() == null
                    ? R.string.widget_sign_in : R.string.widget_loading));
            views.setTextViewText(R.id.widget_state_hint, context.getString(FirebaseAuth.getInstance().getUid() == null
                    ? R.string.widget_sign_in_hint : R.string.widget_loading_hint));
            AppWidgetManager.getInstance(context).updateAppWidget(id, views);
            preferences(context).edit().putLong("initialized_" + id, preferences(context).getLong("epoch", 0)).apply();
        }
    }

    public static void initializeIfNeeded(Context context, int id) {
        synchronized (PUBLISH_LOCK) {
            if (preferences(context).getLong("initialized_" + id, -1) != preferences(context).getLong("epoch", 0)) {
                initialize(context, id);
            }
        }
    }

    public static void forget(Context context, int id) {
        preferences(context).edit().remove("initialized_" + id).apply();
    }

    public static void requestRefresh(Context context) {
        if (!hasWidgets(context)) return;
        accountChanged(context);
        if (FirebaseAuth.getInstance().getUid() == null) {
            for (int id : widgetIds(context)) initialize(context, id);
            return;
        }
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(MyWork.class)
                .setConstraints(networkConstraint()).build();
        // A late event must run after an already-started fetch, rather than being lost by KEEP.
        WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, work);
    }

    public static void schedulePeriodic(Context context) {
        if (!hasWidgets(context)) return;
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(MyWork.class, 15, TimeUnit.MINUTES)
                .setConstraints(networkConstraint()).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, work);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK);
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK);
    }

    private static Constraints networkConstraint() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    /** Returns false on transient network/image failures, preserving the launcher's last good image. */
    public static boolean refresh(Context context, BooleanSupplier stopped) throws Exception {
        accountChanged(context);
        long sequence = RENDER_SEQUENCE.incrementAndGet();
        String uid = FirebaseAuth.getInstance().getUid();
        long epoch = preferences(context).getLong("epoch", 0);
        if (!hasWidgets(context) || stopped.getAsBoolean()) return true;
        if (uid == null) {
            for (int id : widgetIds(context)) initialize(context, id);
            return true;
        }
        QuerySnapshot snapshot = Tasks.await(FirebaseFirestore.getInstance().collection("post_photo")
                .whereArrayContains("friendId", uid).orderBy("timestamp", Query.Direction.DESCENDING)
                .get(Source.SERVER), 30, TimeUnit.SECONDS);
        List<Image> images = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot) {
            Image image = doc.toObject(Image.class);
            if (image != null) {
                image.setPostId(doc.getId());
                images.add(image);
            }
        }
        WidgetContent content = WidgetContent.from(images, uid);
        FutureTarget<Bitmap> photoTarget = null;
        FutureTarget<Bitmap> avatarTarget = null;
        try {
            Bitmap photo = null;
            Bitmap avatar = null;
            String mediaUrl = content.mediaUrl();
            if (mediaUrl != null) {
                photoTarget = Glide.with(context).asBitmap().load(mediaUrl).override(480, 480).fitCenter().submit();
                photo = photoTarget.get(15, TimeUnit.SECONDS);
            }
            if (stopped.getAsBoolean()) return true;
            if (content.latest != null && content.latest.getAvatarPoster() != null && !content.latest.getAvatarPoster().isEmpty()) {
                avatarTarget = Glide.with(context).asBitmap().load(content.latest.getAvatarPoster())
                        .override(64, 64).circleCrop().submit();
                try { avatar = avatarTarget.get(8, TimeUnit.SECONDS); }
                catch (Exception ignored) { /* Show the default avatar if this optional image is unavailable. */ }
            }
            synchronized (PUBLISH_LOCK) {
                if (stopped.getAsBoolean() || !Objects.equals(uid, FirebaseAuth.getInstance().getUid())
                        || epoch != preferences(context).getLong("epoch", 0) || sequence != RENDER_SEQUENCE.get()) return true;
                for (int id : widgetIds(context)) {
                    RemoteViews views = baseViews(context, id);
                    if (content.latest == null) {
                        views.setTextViewText(R.id.widget_message, context.getString(R.string.widget_empty));
                        views.setTextViewText(R.id.widget_state_hint, context.getString(R.string.widget_empty_hint));
                    } else {
                        views.setViewVisibility(R.id.widget_placeholder, photo == null ? View.VISIBLE : View.GONE);
                        views.setViewVisibility(R.id.img_widget, photo == null ? View.GONE : View.VISIBLE);
                        if (photo == null) {
                            views.setTextViewText(R.id.widget_message, context.getString(R.string.widget_video));
                            views.setTextViewText(R.id.widget_state_hint, context.getString(R.string.widget_video_hint));
                        } else views.setImageViewBitmap(R.id.img_widget, photo);
                        views.setViewVisibility(R.id.widget_author, View.VISIBLE);
                        if (avatar != null) views.setImageViewBitmap(R.id.img_avatar_widget, avatar);
                        String author = content.latest.getUserNamePoster();
                        views.setTextViewText(R.id.tv_username_widget, author == null || author.trim().isEmpty()
                                ? context.getString(R.string.widget_author_fallback) : author.trim());
                        String caption = content.latest.getStatus();
                        caption = caption == null ? "" : caption.trim();
                        views.setTextViewText(R.id.widget_caption, caption);
                        views.setViewVisibility(R.id.widget_caption, caption.isEmpty() ? View.GONE : View.VISIBLE);
                        views.setTextViewText(R.id.tv_widget, context.getString(R.string.widget_new_posts, content.badge()));
                        views.setViewVisibility(R.id.tv_widget, content.unread > 0 ? View.VISIBLE : View.GONE);
                    }
                    AppWidgetManager.getInstance(context).updateAppWidget(id, views);
                }
            }
            return true;
        } finally {
            if (photoTarget != null) Glide.with(context).clear(photoTarget);
            if (avatarTarget != null) Glide.with(context).clear(avatarTarget);
        }
    }

    private static RemoteViews baseViews(Context context, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget);
        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        views.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(context, 101, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        Intent refresh = new Intent(context, WidgetProvider.class).setAction(WidgetProvider.ACTION_REFRESH)
                .setData(Uri.parse("connection://widget/refresh/" + id))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        views.setOnClickPendingIntent(R.id.widget_refresh, PendingIntent.getBroadcast(context, id, refresh,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        views.setImageViewResource(R.id.img_widget, R.drawable.widget_background);
        views.setImageViewResource(R.id.img_avatar_widget, R.drawable.widget_avatar_placeholder);
        views.setViewVisibility(R.id.img_widget, View.GONE);
        views.setViewVisibility(R.id.widget_placeholder, View.VISIBLE);
        views.setViewVisibility(R.id.widget_caption, View.GONE);
        views.setViewVisibility(R.id.widget_author, View.GONE);
        views.setViewVisibility(R.id.tv_widget, View.GONE);
        return views;
    }

    private WidgetUpdater() {}
}
