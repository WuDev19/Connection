package com.example.connection.ui.state;

public interface OnStateListener {
    void onSuccess(String success);
    void onFailure(String error);
    void onLoading(boolean isLoading);
}
