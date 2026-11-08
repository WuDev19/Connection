package com.example.connection.ui.view.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.connection.BuildConfig;
import com.example.connection.R;
import com.example.connection.constants.SecurityCheckUtil;
import com.example.connection.constants.SharePreUtil;
import com.example.connection.ui.view.activity.authentication.LoginActivity;
import com.example.connection.ui.view.activity.chat.ChatActivity;
import com.example.connection.ui.viewmodel.SplashViewModel;

@SuppressLint("CustomSplashScreen")
public class CustomSplashActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private SplashViewModel splashViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_custom_splash);

        if (!BuildConfig.DEBUG && (SecurityCheckUtil.isDeviceCompromised() || SecurityCheckUtil.isTampered(this, BuildConfig.EXPECTED_CERT_SHA256))) {
            Toast.makeText(this, "Phát hiện môi trường không an toàn. Ứng dụng sẽ đóng để bảo vệ dữ liệu.", Toast.LENGTH_LONG).show();
            finishAffinity();
            return;
        }

        progressBar = findViewById(R.id.progress_bar_splash);
        splashViewModel = new ViewModelProvider(this).get(SplashViewModel.class);
        splashViewModel.fetchMusicFireStore();
        splashViewModel.getNumberCurrentUser().observe(this, number -> SharePreUtil.getInstance(getApplicationContext()).writeNumberCurrentUser(number));
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            goToNextActivity();
            progressBar.setVisibility(View.GONE);
        }, 1000);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void goToNextActivity() {
        splashViewModel.checkUser().observe(this, firebaseUser -> {
            if (firebaseUser != null) {
                String chatRoomId = getIntent().getStringExtra("chatRoomId");
                if (chatRoomId == null) {
                    chatRoomId = getIntent().getStringExtra("chat_room_id");
                }
                if (chatRoomId != null && !chatRoomId.trim().isEmpty()) {
                    Intent mainIntent = new Intent(CustomSplashActivity.this, MainActivity.class);
                    Intent chatIntent = new Intent(CustomSplashActivity.this, ChatActivity.class);
                    chatIntent.putExtra("chat_room_id", chatRoomId);
                    chatIntent.putExtra("chatRoomId", chatRoomId);
                    startActivities(new Intent[]{mainIntent, chatIntent});
                } else {
                    Intent intent = new Intent(CustomSplashActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            } else {
                Intent intent = new Intent(CustomSplashActivity.this, LoginActivity.class);
                startActivity(intent);
            }
            finish();
        });
    }

}