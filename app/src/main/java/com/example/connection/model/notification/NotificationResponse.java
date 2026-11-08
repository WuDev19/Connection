package com.example.connection.model.notification;

import com.google.gson.annotations.SerializedName;

public class NotificationResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("success_count")
    private int successCount;

    @SerializedName("failure_count")
    private int failureCount;

    public NotificationResponse() {
    }

    public NotificationResponse(String message, int successCount, int failureCount) {
        this.message = message;
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }
}
