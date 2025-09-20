package com.example.connection.view.activity.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.connection.R;
import com.example.connection.utils.SharePreUtil;
import com.example.connection.viewmodel.SignUpViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText edt_email, edt_password, edt_password_confirm, edt_create_username;
    private Button bt_sign_up;
    private ProgressBar progressBar;
    private SignUpViewModel signUpViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        initView();

        signUpViewModel = new ViewModelProvider(this).get(SignUpViewModel.class);

        signUpViewModel.getSignUpSuccess().observe(this, success -> {
            if (success) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SignUpActivity.this, "Đăng ký thành công", Toast.LENGTH_LONG).show();
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SignUpActivity.this, "Tài khoản có vẻ đã tồn tại", Toast.LENGTH_LONG).show();
            }
        });

        signUpViewModel.getEmailAndPassword().observe(this, s -> {
            String[] emailPassword = s.split("_");
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            intent.putExtra("email", emailPassword[0]);
            intent.putExtra("password", emailPassword[1]);
            startActivity(intent);
            finish();
        });

        signUpViewModel.getConfirmInfoLiveData().observe(this, confirm -> {
            switch (confirm) {
                case "confirm":
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case "distinct mk":
                    edt_password_confirm.setError("Mật khẩu xác nhận có vẻ chưa chính xác");
                    break;
                case "lack info":
                    Toast.makeText(SignUpActivity.this,
                                    "Hãy điền đầy đủ thông tin",
                                    Toast.LENGTH_LONG
                            )
                            .show();
                    break;
            }
        });

        bt_sign_up.setOnClickListener(view -> {
            if (SharePreUtil.getInstance(getApplicationContext()).getNumberCurrentUser() > 20) {
                Toast.makeText(SignUpActivity.this,
                                "Thật đáng tiếc, số lượng user đã đạt giới hạn",
                                Toast.LENGTH_LONG)
                        .show();
                return;
            }
            registerUser();
        });

    }

    private void initView() {
        edt_email = findViewById(R.id.edt_email);
        edt_password = findViewById(R.id.edt_password);
        edt_password_confirm = findViewById(R.id.edt_password_confirm);
        bt_sign_up = findViewById(R.id.bt_sign_up);
        progressBar = findViewById(R.id.progress_bar3);
        edt_create_username = findViewById(R.id.edt_create_username);
    }

    private void registerUser() {
        String email = Objects.requireNonNull(edt_email.getText()).toString().trim();
        String password = Objects.requireNonNull(edt_password.getText()).toString().trim();
        String passwordConfirm = Objects.requireNonNull(edt_password_confirm.getText()).toString().trim();
        String username = Objects.requireNonNull(edt_create_username.getText()).toString().trim();
        signUpViewModel.signUp(email, password, passwordConfirm, username);
    }

}