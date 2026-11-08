package com.example.connection.data.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // Mặc định: 10.0.2.2 cho Android Emulator trỏ về localhost máy tính.
    // Khi test trên máy thật qua cùng mạng Wi-Fi, gọi ApiClient.setBaseUrl("http://<IP_LAN_MAY_TINH>:8080/")
    private static String BASE_URL = "http://10.0.2.2:8080/";
    private static Retrofit retrofit = null;

    public static synchronized void setBaseUrl(String newBaseUrl) {
        if (!newBaseUrl.endsWith("/")) {
            newBaseUrl += "/";
        }
        BASE_URL = newBaseUrl;
        retrofit = null; // Reset để khởi tạo lại với URL mới
    }

    public static synchronized String getBaseUrl() {
        return BASE_URL;
    }

    public static synchronized Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static NotificationApiService getNotificationApiService() {
        return getClient().create(NotificationApiService.class);
    }
}
