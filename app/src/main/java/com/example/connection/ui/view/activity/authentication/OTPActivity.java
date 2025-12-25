package com.example.connection.ui.view.activity.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.connection.R;
import com.example.connection.ui.viewmodel.OTPViewModel;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Objects;

public class OTPActivity extends AppCompatActivity {

    Button btn_confirm;
    TextInputEditText edt_otp;
    TextView tv_resend;
    String mSdt, idVerify;
    ProgressBar progressBar;
    String TAG = getClass().getName();
    OTPViewModel otpViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp);
        otpViewModel = new ViewModelProvider(this).get(OTPViewModel.class);
        initView();
        getDataIntent();

        btn_confirm.setOnClickListener(view -> {
            String otp = Objects.requireNonNull(edt_otp.getText()).toString().trim();
            otpViewModel.loginWithOTP(idVerify, otp, mSdt);
        });

        tv_resend.setOnClickListener(view -> otpViewModel.resendOTP(mSdt, OTPActivity.this));

        otpViewModel.getCheckOTP().observe(this, checkOTP -> {
            if(checkOTP){
                progressBar.setVisibility(View.VISIBLE);
            }
            else {
                Toast.makeText(OTPActivity.this, "Vui lòng nhập mã OTP", Toast.LENGTH_LONG).show();
            }
        });

        otpViewModel.getSuccess().observe(this, success -> {
            if(success){
                goToUsernameActivity();
            }
            else {
                Toast.makeText(OTPActivity.this, "Invalid", Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        });

        otpViewModel.getVerify().observe(this, s -> {
            if (s != null) {
                idVerify = s;
                progressBar.setVisibility(View.GONE);
            }
        });

        otpViewModel.getValidate().observe(this, validate -> {
            if(validate){
                progressBar.setVisibility(View.GONE);
            }
            else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(OTPActivity.this, "Send OTP Failed", Toast.LENGTH_LONG).show();
            }
        });

        otpViewModel.getCodeSent().observe(this, codeSent -> {
            if(codeSent){
                Toast.makeText(OTPActivity.this, "Vui lòng kiểm tra tin nhắn", Toast.LENGTH_LONG).show();
            }
        });

    }

    private void initView() {
        btn_confirm = findViewById(R.id.btn_confirm);
        edt_otp = findViewById(R.id.edt_otp);
        tv_resend = findViewById(R.id.tv_resend);
        progressBar = findViewById(R.id.progress_bar2);
    }

    private void getDataIntent() {
        Intent intent = getIntent();
        mSdt = intent.getStringExtra("phone");
        idVerify = intent.getStringExtra("id");
    }

    private void goToUsernameActivity() {
        Intent intent = new Intent(OTPActivity.this, UsernameActivity.class);
        startActivity(intent);
        finish();
    }

}