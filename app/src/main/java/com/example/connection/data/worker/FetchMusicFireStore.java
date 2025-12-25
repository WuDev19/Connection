package com.example.connection.data.worker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.connection.data.local.UserDao;
import com.example.connection.data.local.MusicDatabase;
import com.example.connection.data.local.MyMusic;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FetchMusicFireStore extends Worker {

    private static final String TAG = "FetchMusicFireStore";
    private ExecutorService fetchMusicService;
    private UserDao userDao;

    public FetchMusicFireStore(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        fetchMusic();
        return Result.success();
    }

    @SuppressLint("ApplySharedPref")
    private void fetchMusic() {
        MusicDatabase musicDatabase = MusicDatabase.getInstance(getApplicationContext());
        userDao = musicDatabase.userDao();
        fetchMusicService = Executors.newSingleThreadExecutor();
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("passwordUser", Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
        int num = sharedPreferences.getInt("num", 0);
        Log.e(TAG, "doWorkRunning");
        FirebaseFirestore.getInstance().collection("music")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.e(TAG, "fetchMusic: " + queryDocumentSnapshots.size());
                    if (!queryDocumentSnapshots.isEmpty() && queryDocumentSnapshots.size() != num) {
                        List<Task<DocumentSnapshot>> taskList = new ArrayList<>();
                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots){
                            if(documentSnapshot.exists()){
                                taskList.add(documentSnapshot.getReference().get().addOnSuccessListener(documentSnapshot1 -> Log.e(TAG, "fetchMusic: add task")));
                            }
                        }
                        Tasks.whenAllSuccess(taskList).addOnSuccessListener(tasks -> {
                            fetchMusicService.execute(() -> {
                                for (Task<DocumentSnapshot> task : taskList) {
                                    if (task.isSuccessful()) {
                                        MyMusic myMusic = task.getResult().toObject(MyMusic.class);
                                        if (myMusic != null) {
                                            userDao.insert(myMusic);
                                            Log.e(TAG, "fetchMusic: insert");
                                        }
                                    }
                                }
                            });
                            Log.e(TAG, "fetchMusic: outside");
                            fetchMusicService.shutdown();
                        }).addOnFailureListener(e -> {
                            fetchMusicService.shutdown();
                            Log.e(TAG, "fetchMusic: task failure");
                        });
                        editor.putInt("num", queryDocumentSnapshots.size());
                        editor.commit();
                        Log.e(TAG, "Successfully");
                    } else {
                        Log.e(TAG, "fetchMusic: already fetched");
                        fetchMusicService.shutdown();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                    fetchMusicService.shutdown();
                });
    }
}
