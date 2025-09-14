package com.example.connection.application;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class Cloudinary extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", "dsmw3ean4");
        MediaManager.init(this, config);
    }
}
