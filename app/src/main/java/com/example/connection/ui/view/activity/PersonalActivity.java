package com.example.connection.ui.view.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.ui.view.activity.authentication.LoginActivity;
import com.example.connection.ui.view.fragment.FragmentChangeEmail;
import com.example.connection.ui.view.fragment.FragmentChangePassword;
import com.example.connection.ui.view.fragment.FragmentInfo;
import com.example.connection.ui.view.fragment.GuideWidget;
import com.example.connection.ui.viewmodel.PersonalViewModel;

import de.hdodenhof.circleimageview.CircleImageView;

public class PersonalActivity extends AppCompatActivity {

    private TextView tv_log_out, tv_email_change, tv_delete, tv_password_change, tv_username, tv_guide_widget, tv_contact;
    private Button bt_change_info;
    private CircleImageView img_avatar;
    private ImageView img_back;
    private PersonalViewModel personalViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_personal);

        personalViewModel = new ViewModelProvider(this).get(PersonalViewModel.class);

        initView();

        personalViewModel.showUserFromFirebase();

        personalViewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                tv_username.setText(user.getUserName());
                Glide.with(this)
                        .load(user.getAvatarUser())
                        .error(R.drawable.icon_avatar)
                        .into(img_avatar);
            } else {
                Toast.makeText(this, "Vui lòng kiểm tra lại kết nối", Toast.LENGTH_SHORT).show();
            }
        });

        personalViewModel.getSuccess().observe(this, success -> {
            Toast.makeText(this, success, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finishAffinity();
        });

        personalViewModel.getError().observe(this, error -> {
            if (error.equals("Dang nhap lai")) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(this)
                        .setTitle("Connection")
                        .setMessage("Vui lòng đăng nhập lại lần cuối trước khi xóa tài khoản")
                        .setPositiveButton("Ok", (dialogInterface12, i12) -> {
                            Intent intent = new Intent(this, LoginActivity.class);
                            startActivity(intent);
                        })
                        .setNegativeButton("Không", (dialogInterface1, i1) -> dialogInterface1.cancel());
                dialog.create().show();
            } else Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });

        onClick();

    }

    private void initView() {
        tv_log_out = findViewById(R.id.tv_log_out);
        tv_email_change = findViewById(R.id.tv_email_change);
        tv_password_change = findViewById(R.id.tv_password_change);
        tv_delete = findViewById(R.id.tv_delete);
        bt_change_info = findViewById(R.id.bt_change_info);
        img_avatar = findViewById(R.id.img_avatar);
        tv_username = findViewById(R.id.tv_username);
        tv_guide_widget = findViewById(R.id.tv_guide_widget);
        tv_contact = findViewById(R.id.tv_contact);
        img_back = findViewById(R.id.back_personal);
    }

    private void onClick() {

        tv_log_out.setOnClickListener(view -> {
            personalViewModel.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finishAffinity();
        });

        bt_change_info.setOnClickListener(view -> {
            FragmentInfo fragmentInfo = new FragmentInfo();
            fragmentInfo.show(getSupportFragmentManager(), fragmentInfo.getTag());
        });

        tv_email_change.setOnClickListener(view -> {
            FragmentChangeEmail fragmentChangeEmail = new FragmentChangeEmail();
            fragmentChangeEmail.show(getSupportFragmentManager(), fragmentChangeEmail.getTag());
        });

        tv_password_change.setOnClickListener(view -> {
            FragmentChangePassword fragmentChangePassword = new FragmentChangePassword();
            fragmentChangePassword.show(getSupportFragmentManager(), fragmentChangePassword.getTag());
        });

        tv_delete.setOnClickListener(view -> deleteUser());

        tv_guide_widget.setOnClickListener(view -> {
            GuideWidget guideWidget = new GuideWidget();
            guideWidget.show(getSupportFragmentManager(), guideWidget.getTag());
        });

        tv_contact.setOnClickListener(view -> {
            AlertDialog.Builder contactDialog = new AlertDialog.Builder(this);
            @SuppressLint("InflateParams") View view1 = getLayoutInflater().inflate(R.layout.custom_alert_contact, null);
            Button bt_cancel = view1.findViewById(R.id.bt_cancel);
            TextView alert_email = view1.findViewById(R.id.alert_email);
            TextView alert_facebook = view1.findViewById(R.id.alert_facebook);
            contactDialog.setTitle("Connection");
            contactDialog.setView(view1);
            AlertDialog dialog = contactDialog.create();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.custom_dialog);
                dialog.show();
            } else {
                Toast.makeText(this, "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
            }
            bt_cancel.setOnClickListener(view2 -> dialog.dismiss());
            alert_email.setOnClickListener(view22 -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:")); // Chỉ định là gửi email
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"nguyenvu19a19@gmail.com"});
                startActivity(intent);
            });
            alert_facebook.setOnClickListener(view23 -> {
                String facebookUrl = "https://www.facebook.com/vanvu.197";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl));
                startActivity(intent);
            });
        });

        img_back.setOnClickListener(view -> finish());

    }

    private void deleteUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Connection")
                .setMessage("Bạn chắc chắn muốn xóa tài khoản?")
                .setNegativeButton("Để suy nghĩ lại", (dialogInterface, i) -> dialogInterface.cancel())
                .setPositiveButton("Chắc chắn", (dialogInterface, i) -> personalViewModel.deleteUser());
        builder.create().show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        personalViewModel.updateEmail(this);
        personalViewModel.showUserFromFirebase();
    }

    @Override
    protected void onStop() {
        super.onStop();
        personalViewModel.unregisterUserListening();
    }

}