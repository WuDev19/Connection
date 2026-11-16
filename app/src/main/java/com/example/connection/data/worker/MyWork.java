package com.example.connection.data.worker;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MyWork extends Worker {
    public MyWork(@NonNull Context context, @NonNull WorkerParameters parameters) { super(context, parameters); }

    @NonNull @Override public Result doWork() {
        try {
            return WidgetUpdater.refresh(getApplicationContext(), this::isStopped) ? Result.success() : retryOrFail();
        } catch (Exception error) {
            if (isStopped()) return Result.success();
            Log.w("WidgetWorker", "Keeping the last widget image until a refresh succeeds", error);
            return retryOrFail();
        }
    }

    private Result retryOrFail() { return getRunAttemptCount() < 4 ? Result.retry() : Result.failure(); }
}
