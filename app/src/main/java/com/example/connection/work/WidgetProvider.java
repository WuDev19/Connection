package com.example.connection.work;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.connection.utils.SharePreUtil;

import java.util.concurrent.TimeUnit;

public class WidgetProvider extends AppWidgetProvider {

    private final String TAG = getClass().getName();

    @SuppressLint("ApplySharedPref")
    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        SharePreUtil.getInstance(context.getApplicationContext()).setWidget(true);
        Log.e(TAG, "onEnabled: ");
        scheduleJob(context);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        SharePreUtil.getInstance(context.getApplicationContext()).setWidget(false);
        Log.e(TAG, "onDisabled: ");
        WorkManager.getInstance(context).cancelUniqueWork("getPost");
    }

    private void scheduleJob(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(MyWork.class, 15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("getPost", ExistingPeriodicWorkPolicy.KEEP, workRequest);
    }
}
