package com.example.connection.view.activity.authentication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.connection.R;
import com.example.connection.view.activity.MainActivity;
import com.example.connection.viewmodel.SignInViewModel;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Objects;

public class SignInActivity extends AppCompatActivity {

    TextInputEditText edt_email1, edt_password1;
    TextView tv_sign_up, tv_forgot_password;
    Button bt_sign_in;
    ProgressBar progressBar;
    SignInViewModel signInViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);
        initView();
        signInViewModel = new ViewModelProvider(this).get(SignInViewModel.class);
        getDataIntent();
        tv_sign_up.setOnClickListener(view -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        bt_sign_in.setOnClickListener(view -> signIn());

        tv_forgot_password.setOnClickListener(view -> {
            View view1 = getLayoutInflater().inflate(R.layout.layout_forgot_password, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(SignInActivity.this);
            builder.setView(view1);
            TextInputEditText sendEmail = view1.findViewById(R.id.edt_email2);
            builder.setTitle("Connection");
            builder.setMessage("Vui lòng nhập email bạn đã đăng ký\nNếu nhập email khác sẽ không nhận được link xác nhận");
            builder.setNegativeButton("Không", (dialogInterface, i) -> dialogInterface.cancel());
            builder.setPositiveButton("Ok", (dialogInterface, i) -> {
                String email = Objects.requireNonNull(sendEmail.getText()).toString().trim();
                if (email.isEmpty()) dialogInterface.cancel();
                else {
                    signInViewModel.sendPasswordReset(email);
                }
            });
            AlertDialog dialog = builder.create();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.custom_dialog);
                dialog.show();
            } else {
                Toast.makeText(this, "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
            }
        });

        signInViewModel.getSuccessLiveData().observe(this, success -> {
            if (success.equals("Success")) {
                progressBar.setVisibility(View.GONE);
                Log.e("SignInRepository", "signIn: success");
                Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                startActivity(intent);
                finishAffinity();
            } else {
                Toast.makeText(SignInActivity.this, success, Toast.LENGTH_SHORT).show();
            }
        });

        signInViewModel.getErrorLiveData().observe(this, error -> {
            if (error.equals("Email hoặc mật khẩu không đúng") || error.equals("Không có kết nối mạng")) {
                progressBar.setVisibility(View.GONE);
            }
            Toast.makeText(SignInActivity.this, error, Toast.LENGTH_SHORT).show();
        });

        signInViewModel.getIsEmptyInfo().observe(this, isEmpty -> {
            if (isEmpty) {
                Toast.makeText(SignInActivity.this, "Vui lòng nhập email hoặc mật khẩu", Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initView() {
        edt_email1 = findViewById(R.id.edt_email1);
        edt_password1 = findViewById(R.id.edt_password1);
        tv_sign_up = findViewById(R.id.tv_sign_up);
        bt_sign_in = findViewById(R.id.bt_sign_in);
        progressBar = findViewById(R.id.progress_bar4);
        tv_forgot_password = findViewById(R.id.forgot_password);
    }

    @SuppressLint("ApplySharedPref")
    private void signIn() {
        String email = Objects.requireNonNull(edt_email1.getText()).toString().trim();
        String password = Objects.requireNonNull(edt_password1.getText()).toString().trim();
        signInViewModel.getSignInWithEmailAndPassword(email, password, this);
    }

    private void getDataIntent() {
        Intent intent = getIntent();
        if (intent == null) return;
        String email = intent.getStringExtra("email");
        String password = intent.getStringExtra("password");
        edt_email1.setText(email);
        edt_password1.setText(password);
    }

}