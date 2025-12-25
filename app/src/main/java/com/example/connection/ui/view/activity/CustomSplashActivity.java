package com.example.connection.ui.view.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.connection.R;
import com.example.connection.constants.SharePreUtil;
import com.example.connection.ui.view.activity.authentication.LoginActivity;
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

    private void goToNextActivity() {
        splashViewModel.checkUser().observe(this, firebaseUser -> {
            Intent intent;
            if (firebaseUser != null) {
                intent = new Intent(CustomSplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(CustomSplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        });
    }

}