package com.example.connection.data.worker;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.connection.model.Image;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Realtime while the app is visible; WorkManager/FCM handle the background. */
public final class WidgetSyncCoordinator implements Application.ActivityLifecycleCallbacks {
    private static WidgetSyncCoordinator instance;
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int startedActivities;
    private ListenerRegistration posts;
    private String listeningUid;
    private WidgetContent previous;
    private final Runnable refresh;
    private final Runnable stop;

    private WidgetSyncCoordinator(Application application) {
        context = application.getApplicationContext();
        refresh = () -> WidgetUpdater.requestRefresh(context);
        stop = () -> { if (startedActivities == 0) stopListening(); };
        application.registerActivityLifecycleCallbacks(this);
        FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            WidgetUpdater.accountChanged(context);
            reconcile();
            WidgetUpdater.requestRefresh(context);
        });
    }

    public static synchronized WidgetSyncCoordinator get(Context context) {
        if (instance == null) instance = new WidgetSyncCoordinator((Application) context.getApplicationContext());
        return instance;
    }

    public void reconcile() {
        String uid = FirebaseAuth.getInstance().getUid();
        boolean installed = WidgetUpdater.hasWidgets(context);
        if (!installed || uid == null || startedActivities == 0) {
            stopListening();
            return;
        }
        WidgetUpdater.schedulePeriodic(context);
        if (posts != null && Objects.equals(uid, listeningUid)) return;
        stopListening();
        listeningUid = uid;
        posts = FirebaseFirestore.getInstance().collection("post_photo")
                .whereArrayContains("friendId", uid).orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (!Objects.equals(uid, listeningUid) || !Objects.equals(uid, FirebaseAuth.getInstance().getUid())) return;
                    if (error != null) { Log.w("WidgetSync", "Realtime widget update unavailable", error); return; }
                    if (snapshot == null || snapshot.getMetadata().hasPendingWrites()) return;
                    List<Image> images = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot) {
                        Image image = doc.toObject(Image.class);
                        if (image != null) { image.setPostId(doc.getId()); images.add(image); }
                    }
                    WidgetContent content = WidgetContent.from(images, uid);
                    if (!content.equals(previous)) {
                        previous = content;
                        handler.removeCallbacks(refresh);
                        handler.postDelayed(refresh, 700);
                    }
                });
    }

    private void stopListening() {
        if (posts != null) posts.remove();
        posts = null;
        listeningUid = null;
        previous = null;
    }

    @Override public void onActivityStarted(Activity activity) {
        startedActivities++;
        handler.removeCallbacks(stop);
        reconcile();
    }

    @Override public void onActivityStopped(Activity activity) {
        startedActivities = Math.max(0, startedActivities - 1);
        if (startedActivities == 0) handler.postDelayed(stop, 700);
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
