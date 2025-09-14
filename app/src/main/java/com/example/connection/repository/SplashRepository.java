package com.example.connection.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.connection.work.FetchMusicFireStore;
import com.example.connection.utils.SharePreUtil;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.concurrent.TimeUnit;

public class SplashRepository {

    private final Application application;

    public SplashRepository(Application application) {
        this.application = application;
    }

//    public LiveData<Integer> getNumberOfMessage() {
//        Log.e("MainActivity", "getNumberOfMessage: truoc");
//        String myId = FirebaseAuth.getInstance().getUid();
//        if (myId != null) {
//            Log.e("MainActivity", "getNumberOfMessage: ");
//            FirebaseFirestore.getInstance().collection("chatroom")
//                    .whereArrayContains("userId", myId)
//                    .get()
//                    .addOnSuccessListener(queryDocumentSnapshots -> {
//                        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
//                        for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
//                            Task<QuerySnapshot> task = documentSnapshot.getReference().collection("chats")
//                                    .whereNotEqualTo("senderId", myId)
//                                    .whereEqualTo("seen", false)
//                                    .get();
//                            tasks.add(task);
//                        }
//                        Tasks.whenAllComplete(tasks).addOnCompleteListener(task -> {
//                            int number = 0;
//                            for (Task<QuerySnapshot> task1 : tasks) {
//                                if (task1.isSuccessful()) {
//                                    number += task1.getResult().size();
//                                }
//                            }
//                            mutableLiveData.setValue(number);
//                        });
//                    });
//        }
//        return mutableLiveData;
//    }

    public void fetchMusicFireStore() {
        if (SharePreUtil.getInstance(application.getApplicationContext()).firstUseApp()) {
            OneTimeWorkRequest fetchMusicSchedule = new OneTimeWorkRequest.Builder(FetchMusicFireStore.class)
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .build();
            WorkManager.getInstance(application).enqueueUniqueWork("fetchMusic", ExistingWorkPolicy.KEEP, fetchMusicSchedule);
        }
    }

    public LiveData<FirebaseUser> checkUser() {
        MutableLiveData<FirebaseUser> userMutableLiveData = new MutableLiveData<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userMutableLiveData.setValue(user);
        return userMutableLiveData;
    }

    public Task<QuerySnapshot> getNumberCurrentUser() {
        return FirebaseFirestore.getInstance()
                .collection("users")
                .get();
    }

}
