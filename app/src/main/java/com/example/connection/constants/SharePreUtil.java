package com.example.connection.constants;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

public class SharePreUtil {
    private static SharePreUtil instance;
    private final SharedPreferences sharedPreferences;

    private SharePreUtil(Context context){
        sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE);
    }

    public static synchronized SharePreUtil getInstance(Context context){
        if(instance == null){
            instance = new SharePreUtil(context);
        }
        return instance;
    }

    public boolean firstUseApp(){
        return sharedPreferences.getBoolean("firstUse", true);
    }

    @SuppressLint("ApplySharedPref")
    public void setFirstUseApp(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("firstUse", false);
        editor.commit();
    }

    public boolean enableWidget(){
        return sharedPreferences.getBoolean("enable_widget", false);
    }

    @SuppressLint("ApplySharedPref")
    public void setWidget(boolean enable){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("enable_widget", enable);
        editor.commit();
    }

    @SuppressLint("ApplySharedPref")
    public void writeNumberCurrentUser(int number){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("number_of_user", number);
        editor.commit();
    }

    public int getNumberCurrentUser(){
        return sharedPreferences.getInt("number_of_user", 0);
    }
}
