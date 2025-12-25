package com.example.connection.ui.view.activity.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.connection.R;
import com.example.connection.ui.view.activity.MainActivity;
import com.example.connection.ui.viewmodel.UsernameViewModel;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Objects;

public class UsernameActivity extends AppCompatActivity {

    private TextInputEditText editText;
    private final String TAG = getClass().getName();
    private String usernameCurrent;
    private UsernameViewModel usernameViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_username);
        editText = findViewById(R.id.edt_login_username);
        Button button = findViewById(R.id.btn_commit);

        usernameViewModel = new ViewModelProvider(this).get(UsernameViewModel.class);

        usernameViewModel.getCurrentUsername();

        usernameViewModel.getUsername().observe(this, s -> {
            if (s != null) {
                Log.e(TAG, "onChanged: username");
                usernameCurrent = s;
                editText.setText(usernameCurrent);
            }
        });

        button.setOnClickListener(view -> {
            String username = Objects.requireNonNull(editText.getText()).toString().trim();
            usernameViewModel.setUsername(username, usernameCurrent);
        });

        usernameViewModel.getSuccessLiveData().observe(this, success -> {
            if (success.equals("setUserName")) {
                Intent intent = new Intent(UsernameActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        usernameViewModel.getErrorLiveData().observe(this, error ->
                Toast.makeText(UsernameActivity.this, error, Toast.LENGTH_SHORT).show());
    }

}