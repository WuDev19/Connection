package com.example.connection.view.activity.chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connection.R;
import com.example.connection.adapter.RecentChatUserAdapter;
import com.example.connection.adapter.SearchRecentChatAdapter;
import com.example.connection.model.ChatRoom;
import com.example.connection.view.fragment.FragmentCreateGroupChat;
import com.example.connection.viewmodel.RecentChatViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecentChatActivity extends AppCompatActivity {

    private ImageView img_back;
    private TextInputLayout txt_layout_search;
    private TextInputEditText edt_chat_search;
    private TextView tv_create_group;
    private RecyclerView recyclerViewRecentChat, recycler_view_search_chat;
    private RecentChatUserAdapter adapter;
    private final String TAG = getClass().getName();
    private RecentChatViewModel recentChatViewModel;
    private LiveData<List<ChatRoom>> currentSearchLiveData = null;
    private SearchRecentChatAdapter searchRecentChatAdapter;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recent_chat);

        initView();

        recentChatViewModel = new ViewModelProvider(this).get(RecentChatViewModel.class);

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
    }

}