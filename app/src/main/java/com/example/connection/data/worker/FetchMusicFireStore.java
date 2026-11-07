package com.example.connection.data.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.connection.data.local.MusicDao;
import com.example.connection.data.local.MusicDatabase;
import com.example.connection.data.local.MyMusic;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.concurrent.TimeUnit;

public class FetchMusicFireStore extends Worker {

    private static final String TAG = "FetchMusicFireStore";

    public FetchMusicFireStore(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.e(TAG, "doWorkRunning");
        MusicDatabase musicDatabase = MusicDatabase.getInstance(getApplicationContext());
        MusicDao musicDao = musicDatabase.musicDao();
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
        int num = sharedPreferences.getInt("num", 0);

        try {
            QuerySnapshot queryDocumentSnapshots = Tasks.await(
                    FirebaseFirestore.getInstance().collection("music").get(),
                    30, TimeUnit.SECONDS
            );

            if (!queryDocumentSnapshots.isEmpty() && queryDocumentSnapshots.size() != num) {
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    if (documentSnapshot.exists()) {
                        MyMusic myMusic = documentSnapshot.toObject(MyMusic.class);
                        if (myMusic != null) {
                            musicDao.insert(myMusic);
                            Log.e(TAG, "fetchMusic: insert " + myMusic.getMusicName());
                        }
                    }
                }
                sharedPreferences.edit().putInt("num", queryDocumentSnapshots.size()).apply();
                Log.e(TAG, "fetchMusic: Successfully finished");
            } else {
                Log.e(TAG, "fetchMusic: already fetched or empty");
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "fetchMusic error: " + e.getMessage(), e);
            return Result.retry();
        }
    }
}
