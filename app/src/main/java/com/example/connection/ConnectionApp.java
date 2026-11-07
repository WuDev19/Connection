package com.example.connection;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class ConnectionApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", BuildConfig.CLOUDINARY_NAME);
        MediaManager.init(this, config);
    }
}
