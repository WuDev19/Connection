package com.example.connection.view.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;
import com.example.connection.R;
import com.example.connection.view.activity.chat.RecentChatActivity;
import com.example.connection.view.fragment.BottomSheetSearch;
import com.example.connection.view.fragment.FragmentHome;
import com.example.connection.view.fragment.FragmentNotification;
import com.example.connection.work.WidgetProvider;
import com.example.connection.utils.MediaPlayerUtil;
import com.example.connection.utils.SendBroadToFragTakeUtil;
import com.example.connection.viewmodel.MainViewModel;
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        dialogPolicy();

        initView();

        checkPermission();

        mainViewModel.checkActive();

        img_chat.setOnClickListener(view -> {
            MediaPlayerUtil.getInstance().pauseMusic();
            Intent intent = new Intent(MainActivity.this, RecentChatActivity.class);
            Log.e(TAG, "chat");
            startActivity(intent);
        });

        img_personal.setOnClickListener(view -> {
            SendBroadToFragTakeUtil.sendBroadToFragmentTakePhoto(MainActivity.this);
            MediaPlayerUtil.getInstance().pauseMusic();
            Intent intent = new Intent(this, PersonalActivity.class);
            startActivity(intent);
        });

        tv_search.setOnClickListener(view -> {
            MediaPlayerUtil.getInstance().pauseMusic();
            BottomSheetSearch bottomSheetSearch = new BottomSheetSearch();
            Log.e(TAG, "search");
            bottomSheetSearch.show(getSupportFragmentManager(), bottomSheetSearch.getTag());
        });

        fab.setOnClickListener(view -> {
            SendBroadToFragTakeUtil.sendBroadToFragmentTakePhoto(this);
            Bundle bundle = new Bundle();
            bundle.putStringArrayList("confirm_friend", (ArrayList<String>) friendConfirm);
            bundle.putInt("number", numberOfNotification);
            fragmentNotification.setArguments(bundle);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, fragmentNotification, "FragmentNotification")
                    .addToBackStack("FragmentNotification")
                    .commit();
            fab.hide();
            tv_notification.setVisibility(View.GONE);
        });

        fab.setOnTouchListener(new View.OnTouchListener() {
            float initialTouchX, initialTouchY, initialViewX, initialViewY;
            final float density;
            final int screenWidth, screenHeight;

            {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
                density = metrics.density;
                screenWidth = metrics.widthPixels;
                screenHeight = metrics.heightPixels - 160;
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

        mainViewModel.scheduleJob(this);

        mainViewModel.getWidgetLiveData().observe(this, widgetModel -> {
            if(widgetModel != null){
                Log.e(TAG, "onCreate: " + widgetModel.getImage() + " " + widgetModel.getUser() + " " + widgetModel.getList());
                ComponentName componentName = new ComponentName(getApplicationContext(), WidgetProvider.class);
                AppWidgetManager manager = AppWidgetManager.getInstance(getApplicationContext());
                int[] widgetIds = manager.getAppWidgetIds(componentName);
                for (int widgetId : widgetIds) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1, intent, PendingIntent.FLAG_IMMUTABLE);
                    RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.layout_widget);
                    views.setOnClickPendingIntent(R.id.img_widget, pendingIntent);
                    if (widgetModel.getList().isEmpty()) {
                        views.setViewVisibility(R.id.tv_widget, View.GONE);
                    } else if (widgetModel.getList().size() > 9) {
                        views.setTextViewText(R.id.tv_widget, "9+");
                    } else
                        views.setTextViewText(R.id.tv_widget, String.valueOf(widgetModel.getList().size()));
                    if (widgetModel.getImage().getStatus().isEmpty())
                        views.setViewVisibility(R.id.tv_username_widget, View.GONE);
                    else
                        views.setTextViewText(R.id.tv_username_widget, widgetModel.getImage().getStatus());
                    AppWidgetTarget post = new AppWidgetTarget(getApplicationContext(), R.id.img_widget, views, widgetId);
                    AppWidgetTarget avt = new AppWidgetTarget(getApplicationContext(), R.id.img_avatar_widget, views, widgetId);
                    Glide.with(getApplicationContext())
                            .asBitmap()
                            .load(widgetModel.getImage().getImageUrl())
                            .override(300, 300)
                            .into(post);
                    Glide.with(getApplicationContext())
                            .asBitmap()
                            .load(widgetModel.getUser().getAvatarUser())
                            .override(300, 300)
                            .into(avt);
                    manager.partiallyUpdateAppWidget(widgetId, views);
                }
            }
        });

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

    private void initView() {
        tv_search = findViewById(R.id.tv_search);
        img_chat = findViewById(R.id.img_chat);
        img_personal = findViewById(R.id.img_personal);
        tv_notification = findViewById(R.id.tv_notification);
        tv_message = findViewById(R.id.tv_number_message);
        fab = findViewById(R.id.floating_action_button);
        frameLayoutNotification = findViewById(R.id.frame_layout_notification);
        FragmentHome fragmentHome = new FragmentHome();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, fragmentHome, "FragmentHome")
                .commit();
        fragmentNotification = new FragmentNotification();
    }

    public void checkPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.CAMERA};
            requestPermissions(permissions, 10);
            Log.e(TAG, "checkPermission: ");
        }
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
        mainViewModel.getRealtimeUnseenMessage();
        mainViewModel.getCountMessageLiveData().observe(this, countMessage -> {
            if(countMessage == null) return;
            int number = 0;
            for (Integer integer : countMessage.values()) {
                number += integer;
            }
            if (number != 0) {
                tv_message.setText(String.valueOf(number));
            } else tv_message.setText("");
        });
        mainViewModel.getNotificationListenerRealtime();
        mainViewModel.getListFriendConfirmLiveData().observe(this, list -> {
            if(list != null){
                friendConfirm = list;
                friendConfirm.remove(currentUserId);
                if (friendConfirm.isEmpty()) tv_notification.setText("");
                else {
                    tv_notification.setText(String.valueOf(friendConfirm.size()));
                    numberOfNotification = friendConfirm.size();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: ");
        mainViewModel.unregisterListeningRealtime();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mainViewModel.setActive();
        MediaPlayerUtil.getInstance().releaseMediaPlayer();
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