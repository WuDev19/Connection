package com.example.connection.data.api;

import com.example.connection.model.notification.NotificationRequest;
import com.example.connection.model.notification.NotificationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface NotificationApiService {

    @POST("api/v1/notifications/send")
    Call<NotificationResponse> sendNotification(
            @Header("Authorization") String bearerToken,
            @Body NotificationRequest request
    );
}
