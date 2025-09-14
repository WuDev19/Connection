package com.example.connection.utils;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SendBroadToFragTakeUtil {

    public static void sendBroadToFragmentTakePhoto(Context context){
        Intent intent = new Intent("click_when_take_playing");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void broadcastWhenBackNotification(Context context){
        Intent intent = new Intent("back_press_when_playing");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

}
