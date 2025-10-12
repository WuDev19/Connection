package com.example.connection.view.activity.chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connection.R;
import com.example.connection.adapter.AnonymousRandomAdapter;
import com.example.connection.adapter.RecentChatUserAdapter;
import com.example.connection.adapter.SearchRecentChatAdapter;
import com.example.connection.model.ChatRoom;
import com.example.connection.view.fragment.FragmentCreateGroupChat;
import com.example.connection.viewmodel.RecentChatViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RecentChatActivity extends AppCompatActivity {

    private ImageView img_back;
    private TextInputLayout txt_layout_search;
    private TextInputEditText edt_chat_search;
    private TextView tv_create_group, tv_random_chat;
    private RecyclerView recyclerViewRecentChat, recycler_view_search_chat;
    private RecentChatUserAdapter adapter;
    private final String TAG = getClass().getName();
    private RecentChatViewModel recentChatViewModel;
    private LiveData<List<ChatRoom>> currentSearchLiveData = null;
    private SearchRecentChatAdapter searchRecentChatAdapter;
    private FrameLayout frameLayout;
    private AlertDialog dialog;
    private Runnable runnableRandom;
    private final Handler handlerRandom = new Handler();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recent_chat);

        initView();

        recentChatViewModel = new ViewModelProvider(this).get(RecentChatViewModel.class);

        recentChatViewModel.getSuccess().observe(this, chatRoom -> {
            if(chatRoom != null){
                if(dialog.isShowing()){
                    dialog.dismiss();
                }
                Intent intent = new Intent(RecentChatActivity.this, ChatActivity.class);
                intent.putExtra("chat_room", chatRoom);
                startActivity(intent);
            }
        });

        searchRecentChatAdapter = new SearchRecentChatAdapter(this, new ArrayList<>(), chatRoom -> {
            Intent intent = new Intent(RecentChatActivity.this, ChatActivity.class);
            intent.putExtra("chat_room", chatRoom);
            startActivity(intent);
        });

        recycler_view_search_chat.setLayoutManager(new LinearLayoutManager(this));
        recycler_view_search_chat.setAdapter(searchRecentChatAdapter);

        img_back.setOnClickListener(view -> finish());

        tv_create_group.setOnClickListener(view -> {
            FragmentCreateGroupChat fragmentCreateGroupChat = new FragmentCreateGroupChat();
            fragmentCreateGroupChat.show(getSupportFragmentManager(), fragmentCreateGroupChat.getTag());
        });

        tv_random_chat.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(RecentChatActivity.this);
            @SuppressLint("InflateParams") View view1 = getLayoutInflater().inflate(R.layout.custom_random_chat_dialog, null);
            ImageButton img_add = view1.findViewById(R.id.img_add);
            ImageButton img_sub = view1.findViewById(R.id.img_sub);
            EditText edt_quantity = view1.findViewById(R.id.edt_quantity);
            RecyclerView recycler_view_random_chat = view1.findViewById(R.id.recycler_view_random_chat);
            Button btn_cancel = view1.findViewById(R.id.btn_cancel_quantity);
            Button btn_start = view1.findViewById(R.id.btn_start);
            Button btn_confirm = view1.findViewById(R.id.btn_confirm_random_chat);
            builder.setTitle("Chat ngẫu nhiên");
            builder.setMessage("Bạn sẽ được ghép nối với một người dùng bất kỳ và sẽ có tối đa 15 phút để nhắn tin, sau đó đoạn chat sẽ tự biến mất");
            builder.setView(view1);
            dialog = builder.create();
            Window window = dialog.getWindow();
            if(window != null){
                window.setBackgroundDrawableResource(R.drawable.custom_dialog);
                dialog.show();
            }
            btn_cancel.setOnClickListener(view23 -> dialog.dismiss());
            btn_start.setOnClickListener(view25 -> {
                btn_start.setEnabled(false);
                img_add.setEnabled(false);
                img_sub.setEnabled(false);
                edt_quantity.setEnabled(false);
                int number = Integer.parseInt(edt_quantity.getText().toString());
                if(number > 50){
                    Toast.makeText(RecentChatActivity.this, "Tối đa 50 người", Toast.LENGTH_LONG).show();
                    return;
                } else if (number <= 0) {
                    Toast.makeText(RecentChatActivity.this, "Tối thiểu 1 người", Toast.LENGTH_LONG).show();
                    return;
                }
                List<Integer> listAvt = Arrays.asList(R.drawable.anonymous_avt,
                        R.drawable.anonymous_avt,
                        R.drawable.anonymous_avt,
                        R.drawable.anonymous_avt,
                        R.drawable.anonymous_avt);
                AnonymousRandomAdapter anonymousRandomAdapter = new AnonymousRandomAdapter(RecentChatActivity.this, listAvt);
                recycler_view_random_chat.setAdapter(anonymousRandomAdapter);
                recycler_view_random_chat.setLayoutManager(new LinearLayoutManager(RecentChatActivity.this, LinearLayoutManager.HORIZONTAL, false));
                int totalSecond = 2000;
                long firstTouch = System.currentTimeMillis();
                final int[] position = {0};
                if(runnableRandom != null){
                    handlerRandom.removeCallbacks(runnableRandom);
                }
                runnableRandom = new Runnable() {
                    @Override
                    public void run() {
                        long now = System.currentTimeMillis();
                        long sub = now - firstTouch;
                        if(sub < totalSecond){
                            recycler_view_random_chat.smoothScrollToPosition(position[0]);
                            handlerRandom.postDelayed(this, 50);
                            position[0] = (position[0] + 1) % listAvt.size();
                        }
                        else {
                            btn_confirm.setVisibility(View.VISIBLE);
                            btn_start.setEnabled(true);
                            btn_start.setVisibility(View.GONE);
                        }
                    }
                };
                handlerRandom.post(runnableRandom);
            });
            btn_confirm.setOnClickListener(view24 ->
                    recentChatViewModel.getNumberUser(Integer.parseInt(edt_quantity.getText().toString()), getApplicationContext()));
            img_add.setOnClickListener(view2 -> {
                int count = Integer.parseInt(edt_quantity.getText().toString()) + 1;
                if(count > 50) return;
                edt_quantity.setText(String.valueOf(count));
            });
            img_sub.setOnClickListener(view22 -> {
                int count = Integer.parseInt(edt_quantity.getText().toString()) - 1;
                if(count <= 0) return;
                edt_quantity.setText(String.valueOf(count));
            });
        });

        txt_layout_search.setStartIconOnClickListener(view -> searchChat());

        edt_chat_search.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == EditorInfo.IME_ACTION_SEARCH ||
                    id == EditorInfo.IME_ACTION_DONE ||
                    (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchChat();
                return true;
            }
            return false;
        });

        edt_chat_search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                recycler_view_search_chat.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        setUpRecyclerView();

    }

    private void searchChat() {
        String chatName = Objects.requireNonNull(edt_chat_search.getText()).toString().trim();
        if (chatName.isEmpty()) {
            Toast.makeText(RecentChatActivity.this, "Vui lòng nhập ít nhất một ký tự", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentSearchLiveData != null) {
            currentSearchLiveData.removeObservers(RecentChatActivity.this);
        }
        currentSearchLiveData = recentChatViewModel.searchChatRoom(chatName);
        currentSearchLiveData.observe(RecentChatActivity.this, chatRooms -> {
            if (chatRooms != null) {
                if (chatRooms.isEmpty()) {
                    Toast.makeText(RecentChatActivity.this, "Không tìm thấy đoạn chat", Toast.LENGTH_SHORT).show();
                    recycler_view_search_chat.setVisibility(View.GONE);
                } else {
                    recycler_view_search_chat.setVisibility(View.VISIBLE);
                    searchRecentChatAdapter.setData(chatRooms);
                }
            }
        });
    }

    private void initView() {
        img_back = findViewById(R.id.img_back_recent_chat);
        recyclerViewRecentChat = findViewById(R.id.recycler_view_recent_chat);
        txt_layout_search = findViewById(R.id.txt_layout_search_chat);
        edt_chat_search = findViewById(R.id.edt_search_chatroom);
        recycler_view_search_chat = findViewById(R.id.recycler_view_search_chat);
        tv_create_group = findViewById(R.id.tv_create_group);
        RelativeLayout relativeLayout = findViewById(R.id.relative_recent_chat);
        frameLayout = findViewById(R.id.frame_recent_chat);
        tv_random_chat = findViewById(R.id.tv_random_chat);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        relativeLayout.setTranslationY(-screenHeight);
        relativeLayout.animate()
                .translationY(0f)
                .setDuration(800)
                .withEndAction(() -> frameLayout.setBackgroundColor(ContextCompat.getColor(RecentChatActivity.this, R.color.background)))
                .start();
    }

    private void setUpRecyclerView() {
        recentChatViewModel.getQuery().observe(this, query -> {
            if (query != null) {
                FirestoreRecyclerOptions<ChatRoom> options = new FirestoreRecyclerOptions.Builder<ChatRoom>()
                        .setQuery(query, ChatRoom.class)
                        .build();
                adapter = new RecentChatUserAdapter(options, RecentChatActivity.this, (chatRoom, tv_message) -> {
                    Intent intent = new Intent(RecentChatActivity.this, ChatActivity.class);
                    intent.putExtra("chat_room", chatRoom);
                    RecentChatActivity.this.startActivity(intent);
                    tv_message.setTypeface(null, Typeface.NORMAL);
                }, recentChatViewModel, this);
                recyclerViewRecentChat.setLayoutManager(new LinearLayoutManager(RecentChatActivity.this));
                recyclerViewRecentChat.setAdapter(adapter);
                adapter.startListening();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            Log.e(TAG, "onDestroy: ");
            adapter.stopListening();
        }
        if(runnableRandom != null){
            handlerRandom.removeCallbacks(runnableRandom);
        }
    }

}