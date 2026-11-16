package com.example.connection.ui.view.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.ui.view.activity.chat.RecentChatActivity;
import com.example.connection.ui.view.activity.chat.ChatActivity;
import com.example.connection.ui.view.fragment.BottomSheetSearch;
import com.example.connection.ui.view.fragment.FragmentHome;
import com.example.connection.ui.view.fragment.FragmentNotification;
import com.example.connection.constants.MediaPlayerUtil;
import com.example.connection.ui.viewmodel.MainViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FragmentNotification fragmentNotification;
    private ImageView img_chat, img_personal;
    private TextView tv_search;
    private TextView tv_notification, tv_message;
    private FrameLayout frameLayoutNotification;
    private FloatingActionButton fab;
    private final String TAG = getClass().getName();
    private final String currentUserId = FirebaseAuth.getInstance().getUid();
    private List<String> friendConfirm;
    private int numberOfNotification;
    private boolean isClick = false;
    private MainViewModel mainViewModel;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent loginIntent = new Intent(this, CustomSplashActivity.class);
            if (getIntent().getExtras() != null) loginIntent.putExtras(getIntent().getExtras());
            startActivity(loginIntent);
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        dialogPolicy();

        initView(savedInstanceState == null);

        checkPermission();
        checkNotificationPermission();
        syncFcmToken();

        mainViewModel.checkActive();

        img_chat.setOnClickListener(view -> {
            MediaPlayerUtil.getInstance().pauseMusic();
            Intent intent = new Intent(MainActivity.this, RecentChatActivity.class);
            Log.e(TAG, "chat");
            startActivity(intent);
        });

        img_personal.setOnClickListener(view -> {
            MediaPlayerUtil.getInstance().pauseMusic();
            Intent intent = new Intent(this, PersonalActivity.class);
            startActivity(intent);
        });

        tv_search.setOnClickListener(view -> {
            mainViewModel.setCameraMusicPlaying(false);
            MediaPlayerUtil.getInstance().pauseMusic();
            BottomSheetSearch bottomSheetSearch = new BottomSheetSearch();
            Log.e(TAG, "search");
            bottomSheetSearch.show(getSupportFragmentManager(), bottomSheetSearch.getTag());
        });

        fab.setOnClickListener(view -> openNotifications());

        fab.setOnTouchListener(new View.OnTouchListener() {
            float initialTouchX, initialTouchY, initialViewX, initialViewY;
            final float density;
            final int screenWidth, screenHeight;

            {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
                density = metrics.density;
                screenWidth = metrics.widthPixels;
                screenHeight = metrics.heightPixels;
                Log.e(TAG, "instance initializer: " + screenWidth + " " + screenHeight);
            }

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        initialTouchX = motionEvent.getRawX();
                        initialTouchY = motionEvent.getRawY();
                        initialViewX = frameLayoutNotification.getX();
                        initialViewY = frameLayoutNotification.getY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = motionEvent.getRawX() - initialTouchX;
                        float dy = motionEvent.getRawY() - initialTouchY;

                        float newX = initialViewX + dx;
                        float newY = initialViewY + dy;

                        newX = Math.max(0, Math.min(newX, screenWidth - frameLayoutNotification.getWidth()));

                        newY = Math.max(82, Math.min(newY, screenHeight - frameLayoutNotification.getHeight()));

                        frameLayoutNotification.setX(newX);
                        frameLayoutNotification.setY(newY);

                        return true;

                    case MotionEvent.ACTION_UP:
                        float clickX = motionEvent.getRawX() - initialTouchX;
                        float clickY = motionEvent.getRawY() - initialTouchY;
                        if (Math.abs(clickX) < 10 && Math.abs(clickY) < 10) {
                            view.performClick();
                        }
                        return true;
                }
                return false;
            }
        });

        mainViewModel.getCountMessageLiveData().observe(this, countMessage -> {
            if (countMessage == null) return;
            int number = 0;
            for (Integer integer : countMessage.values()) {
                number += integer;
            }
            if (number != 0) {
                tv_message.setText(String.valueOf(number));
            } else {
                tv_message.setText("");
            }
        });

        mainViewModel.getListFriendConfirmLiveData().observe(this, list -> {
            friendConfirm = new ArrayList<>();
            if (list != null) {
                for (String id : list) {
                    if (id != null && !id.equals(currentUserId) && !friendConfirm.contains(id)) friendConfirm.add(id);
                }
            }
            numberOfNotification = friendConfirm.size();
            tv_notification.setText(numberOfNotification == 0 ? "" : String.valueOf(numberOfNotification));
        });

        checkNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkNotificationIntent(intent);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mainViewModel != null) checkNotificationIntent(getIntent());
    }

    private void checkNotificationIntent(Intent intent) {
        if (intent == null || getSupportFragmentManager().isStateSaved()) return;
        String chatRoomId = intent.getStringExtra("chatRoomId");
        if (chatRoomId == null) chatRoomId = intent.getStringExtra("chat_room_id");
        String type = intent.getStringExtra("type");
        if (chatRoomId != null && !chatRoomId.trim().isEmpty()) {
            intent.removeExtra("chatRoomId");
            intent.removeExtra("chat_room_id");
            intent.removeExtra("type");
            MediaPlayerUtil.getInstance().pauseMusic();
            Intent chatIntent = new Intent(this, ChatActivity.class);
            chatIntent.putExtra("chat_room_id", chatRoomId);
            startActivity(chatIntent);
        } else if ("FRIEND_REQUEST".equals(type)) {
            intent.removeExtra("type");
            openNotifications();
        } else {
            intent.removeExtra("type");
        }
    }

    private void openNotifications() {
        androidx.fragment.app.FragmentManager manager = getSupportFragmentManager();
        if (!fab.isEnabled() || manager.isStateSaved() || manager.findFragmentByTag("FragmentNotification") != null) return;
        androidx.fragment.app.Fragment current = manager.findFragmentById(R.id.frame_layout);
        if (current == null) return;
        mainViewModel.setCameraMusicPlaying(false);
        MediaPlayerUtil.getInstance().pauseMusic();
        fragmentNotification = new FragmentNotification();
        Bundle arguments = new Bundle();
        arguments.putStringArrayList("confirm_friend", friendConfirm == null ? new ArrayList<>() : new ArrayList<>(friendConfirm));
        arguments.putInt("number", numberOfNotification);
        fragmentNotification.setArguments(arguments);
        // commitNow is unavailable with a back stack; disable the trigger until this transaction is applied.
        fab.setEnabled(false);
        manager.beginTransaction()
                .hide(current)
                .setMaxLifecycle(current, androidx.lifecycle.Lifecycle.State.STARTED)
                .add(R.id.frame_layout, fragmentNotification, "FragmentNotification")
                .addToBackStack("FragmentNotification")
                .commit();
        fab.hide();
        tv_notification.setVisibility(View.GONE);
    }

    @SuppressLint("ApplySharedPref")
    private void dialogPolicy() {
        mainViewModel.getFirstUse(this).observe(this, firstUse -> {
            if (firstUse) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                @SuppressLint("InflateParams") View viewDialog = getLayoutInflater().inflate(R.layout.layout_policy, null);
                CheckBox checkBox = viewDialog.findViewById(R.id.check_box);
                Button bt_out = viewDialog.findViewById(R.id.bt_out);
                Button bt_continue = viewDialog.findViewById(R.id.bt_continue);
                builder.setTitle("Connection");
                builder.setView(viewDialog);
                AlertDialog dialog = builder.create();
                dialog.setCancelable(false);
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawableResource(R.drawable.custom_dialog1);
                    dialog.show();
                } else {
                    Toast.makeText(MainActivity.this, "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
                }
                bt_out.setOnClickListener(view -> finish());
                bt_continue.setOnClickListener(view -> {
                    if (checkBox.isChecked()) {
                        dialog.dismiss();
                        mainViewModel.setFirstUse(MainActivity.this);
                    } else {
                        Toast.makeText(MainActivity.this, "Vui lòng tích vào ô để tiếp tục", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void initView(boolean createHome) {
        tv_search = findViewById(R.id.tv_search);
        img_chat = findViewById(R.id.img_chat);
        img_personal = findViewById(R.id.img_personal);
        tv_notification = findViewById(R.id.tv_notification);
        tv_message = findViewById(R.id.tv_number_message);
        fab = findViewById(R.id.floating_action_button);
        frameLayoutNotification = findViewById(R.id.frame_layout_notification);
        if (createHome) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, new FragmentHome(), "FragmentHome")
                    .commitNow();
        }
    }

    public void checkPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.CAMERA};
            requestPermissions(permissions, 10);
            Log.e(TAG, "checkPermission: ");
        }
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void syncFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(com.example.connection.service.ConnectionFirebaseMessagingService::updateTokenToFirestore)
                .addOnFailureListener(e -> Log.e(TAG, "Lỗi lấy FCM token: " + e.getMessage()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Đồng ý");
            } else {
                Toast.makeText(this, "Vui lòng cấp quyền để sử dụng camera", Toast.LENGTH_LONG).show();
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    // Hỏi lại sau 1-2 giây
                    new Handler(Looper.getMainLooper()).postDelayed(this::checkPermission, 1000);
                } else {
                    // Người dùng đã tích "Don't ask again"
                    showOpenSettingsDialog();
                }
            }
        }
    }

    private void showOpenSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Ứng dụng cần quyền camera để hoạt động")
                .setMessage("Bạn đã từ chối quyền camera. Vui lòng mở cài đặt để cấp quyền.")
                .setPositiveButton("Mở cài đặt", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStart: ");
        if (mainViewModel == null) return;
        mainViewModel.getRealtimeUnseenMessage();
        mainViewModel.getNotificationListenerRealtime();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: ");
        if (mainViewModel != null) mainViewModel.unregisterListeningRealtime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainViewModel != null) mainViewModel.setActive();
        if (isFinishing()) MediaPlayerUtil.getInstance().releaseMediaPlayer();
        Log.e(TAG, "onDestroy: ");
    }

    public boolean isClick() {
        return isClick;
    }

    public void setClick(boolean click) {
        isClick = click;
    }

    public void setHideFab() {
        fab.hide();
        frameLayoutNotification.setVisibility(View.GONE);
    }

    public void setShowFab() {
        fab.show();
        frameLayoutNotification.setVisibility(View.VISIBLE);
    }

}