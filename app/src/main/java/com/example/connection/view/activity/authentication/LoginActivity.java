package com.example.connection.view.activity.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.connection.R;
import com.example.connection.utils.SharePreUtil;
import com.example.connection.viewmodel.LoginViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private Button btn_login;
    private TextInputEditText edt_phone;
    private TextInputLayout txt_input_layout;
    private ProgressBar progressBar;
    private TextView tv_email;
    private final String TAG = getClass().getName();
    private LoginViewModel loginViewModel;
    private String sdt;
    private String codeSent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        initView();
        loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        btn_login.setOnClickListener(view -> {
            if (SharePreUtil.getInstance(getApplicationContext()).getNumberCurrentUser() > 20) {
                Toast.makeText(LoginActivity.this,
                                "Thật đáng tiếc, số lượng user đã đạt giới hạn",
                                Toast.LENGTH_LONG)
                        .show();
                return;
            }
            String phone = Objects.requireNonNull(edt_phone.getText()).toString().trim();
            if(phone.isEmpty()){
                Toast.makeText(LoginActivity.this, "Vui lòng nhập số điện thoại", Toast.LENGTH_LONG).show();
                return;
            }
            String areaCode = Objects.requireNonNull(txt_input_layout.getPrefixText()).toString();
            String sdt = areaCode + phone;
            loginViewModel.senOtp(sdt, LoginActivity.this);
        });
        loginViewModel.getCheckPhone().observe(this, checkPhone -> progressBar.setVisibility(View.VISIBLE));
        loginViewModel.getSignInSuccess().observe(this, success -> {
            if (success) {
                goToUsernameActivity();
            } else {
                Toast.makeText(LoginActivity.this, "Invalid", Toast.LENGTH_LONG).show();
            }
        });
        loginViewModel.getError().observe(this, s -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(LoginActivity.this, s, Toast.LENGTH_LONG).show();
        });
        loginViewModel.getValidateSms().observe(this, validate -> {
            if (validate) {
                progressBar.setVisibility(View.GONE);
                goToOTPActivity(sdt, codeSent);
            }
        });
        loginViewModel.getSdt().observe(this, s -> {
            if (s != null) {
                sdt = s;
                Log.e(TAG, "onCreate: " + sdt);
            }
        });
        loginViewModel.getCodeSent().observe(this, s -> {
            if (s != null) {
                codeSent = s;
                Log.e(TAG, "onCreate: " + codeSent);
            }
        });
        tv_email.setOnClickListener(view -> gotoSignInActivity());
    }

    private void initView() {
        btn_login = findViewById(R.id.btn_login);
        edt_phone = findViewById(R.id.edt_phone);
        progressBar = findViewById(R.id.progress_bar);
        tv_email = findViewById(R.id.tv_email);
        txt_input_layout = findViewById(R.id.txt_layout1);
    }

    private void goToUsernameActivity() {
        Intent intent = new Intent(LoginActivity.this, UsernameActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToOTPActivity(String mSdt, String s) {
        Log.e(TAG, "goToOTPActivity: " + mSdt + " " + s);
        Intent intent = new Intent(LoginActivity.this, OTPActivity.class);
        intent.putExtra("phone", mSdt);
        intent.putExtra("id", s);
        startActivity(intent);
        finish();
    }

    private void gotoSignInActivity() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
    }

}