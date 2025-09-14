package com.example.connection.listener;

public interface OnStateListener {
    void onSuccess(String success);
    void onFailure(String error);
    void onLoading(boolean isLoading);
}
