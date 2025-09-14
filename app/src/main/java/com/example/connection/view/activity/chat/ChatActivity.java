package com.example.connection.view.activity.chat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.adapter.ChatMessageAdapter;
import com.example.connection.model.ChatMessage;
import com.example.connection.model.ChatRoom;
import com.example.connection.viewmodel.ChatViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private CircleImageView img_avatar;
    private TextView tv_username, tv_active;
    private ImageView img_back;
    private EditText edt_send_message;
    private ImageView img_send_message, img_show_image_click, img_disable_image;
    private PlayerView playerViewChat;
    private CardView cardView;
    private View scrimOverlay;
    private ChatMessageAdapter adapter;
    private RecyclerView recyclerView;
    private final String TAG = getClass().getName();
    private ChatViewModel chatViewModel;
    private LinearLayoutManager manager;
    private ExoPlayer exoPlayer;
    private String urlVideo;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        chatViewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        initView();

        getDataIntent();

        chatViewModel.getOrCreateChatRoom();

        img_back.setOnClickListener(view -> finish());

        img_send_message.setOnClickListener(view -> {
            String message = edt_send_message.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Nhập một tin nhắn bất kì", Toast.LENGTH_LONG).show();
                return;
            }
            chatViewModel.sendMessage(message);
        });

        img_disable_image.setOnClickListener(view -> {
            recyclerView.setOnTouchListener((view1, motionEvent) -> false);
            scrimOverlay.setVisibility(View.GONE);
            cardView.setVisibility(View.GONE);
            edt_send_message.setEnabled(true);
            img_back.setEnabled(true);
            img_send_message.setEnabled(true);
            playerViewChat.setVisibility(View.GONE);
            exoPlayer.clearMediaItems();
        });

        playerViewChat.setOnClickListener(view -> {
            if (urlVideo != null) {
                if (exoPlayer.isPlaying()) {
                    exoPlayer.pause();
                } else exoPlayer.play();
            }
        });

        chatViewModel.getSend().observe(this, send -> {
            if (send) {
                edt_send_message.setText("");
            }
        });

        chatViewModel.getChatRoomLiveData().observe(this, chatRoom -> {
            if (chatRoom == null) {
                Log.e(TAG, "onCreate: check ");
                chatViewModel.createNewChatRoom();
            } else setUpRecyclerViewMessage(chatRoom);
        });

        chatViewModel.getCreateNewChat().observe(this, chatRoom -> {
            if (chatRoom != null) {
                setUpRecyclerViewMessage(chatRoom);
            }
        });

    }

    private void initView() {
        img_back = findViewById(R.id.img_back_chat_room);
        tv_username = findViewById(R.id.tv_username_in_chat);
        tv_active = findViewById(R.id.tv_active);
        img_avatar = findViewById(R.id.avatar_in_chat);
        edt_send_message = findViewById(R.id.edt_text_message);
        img_send_message = findViewById(R.id.img_send_message);
        recyclerView = findViewById(R.id.recycler_view_message);
        img_show_image_click = findViewById(R.id.img_show_image_click_chat);
        img_disable_image = findViewById(R.id.img_disable_image);
        cardView = findViewById(R.id.card_image_chat);
        playerViewChat = findViewById(R.id.player_view_video_click_chat);
        scrimOverlay = findViewById(R.id.scrim_overlay);
        manager = new LinearLayoutManager(ChatActivity.this);
        exoPlayer = new ExoPlayer.Builder(this).build();
    }

    private void getDataIntent() {
        Intent intent = getIntent();
        ChatRoom chatRoom = intent.getParcelableExtra("chat_room");
        if (chatRoom == null) {
            createChatRoom(intent);
        } else {
            showChatRoomExist(chatRoom);
        }
    }

    private void showChatRoomExist(ChatRoom chatRoom) {
        chatViewModel.setChatRoomId(chatRoom.getChatRoomId());
        if (chatRoom.getUserId().size() <= 2) {
            String otherId = getOtherUserId(chatRoom.getUserId());
            if(otherId == null) return;
            if(otherId.contains("_delete")){
                otherId = otherId.split("_")[0];
            }
            chatViewModel.getOtherUser(otherId).observe(this, otherUser -> {
                if (otherUser != null) {
                    tv_username.setText(otherUser.getUserName());
                    Glide.with(ChatActivity.this)
                            .load(otherUser.getAvatarUser())
                            .error(R.drawable.icon_avatar)
                            .into(img_avatar);
                    if (otherUser.isActive()) tv_active.setVisibility(View.VISIBLE);
                }
            });
        } else {
            tv_username.setText(chatRoom.getChatName());
            Glide.with(ChatActivity.this)
                    .load(chatRoom.getChatRoomAvt())
                    .error(R.drawable.icon_avatar)
                    .into(img_avatar);
        }
    }

    private void createChatRoom(Intent intent) {
        String username = intent.getStringExtra("username_chat");
        String avatar = intent.getStringExtra("avatar_chat");
        String id = intent.getStringExtra("user_id");
        String check = intent.getStringExtra("check");
        boolean mActive = intent.getBooleanExtra("is_active", false);
        if (check != null) {
            if (check.equals("sdt")) {
                String sdt = intent.getStringExtra("phone_chat");
                chatViewModel.createOtherUser(id, avatar, username, null, sdt);
            } else {
                String email = intent.getStringExtra("email_chat");
                chatViewModel.createOtherUser(id, avatar, username, email, null);
            }
        }
        chatViewModel.getOtherUserLiveData().observe(this, user -> {
            if (user != null) {
                tv_username.setText(username);
                Glide.with(ChatActivity.this).load(avatar).error(R.drawable.icon_avatar).into(img_avatar);
                if (mActive) tv_active.setVisibility(View.VISIBLE);
            }
        });
    }

    private String getOtherUserId(List<String> list) {
        String myId = FirebaseAuth.getInstance().getUid();
        if (myId == null) return null;
        if (myId.equals(list.get(0))) {
            return list.get(1);
        } else {
            return list.get(0);
        }
    }

    private void setUpRecyclerViewMessage(ChatRoom chatRoom1) {
        chatViewModel.createOptions(chatRoom1);
        chatViewModel.getCreateOptions().observe(this, new Observer<Query>() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onChanged(Query query) {
                if (query != null) {
                    FirestoreRecyclerOptions<ChatMessage> options = new FirestoreRecyclerOptions.Builder<ChatMessage>()
                            .setQuery(query, ChatMessage.class)
                            .setLifecycleOwner(ChatActivity.this)
                            .build();
                    adapter = new ChatMessageAdapter(options, ChatActivity.this, urlPost -> {
                        recyclerView.setOnTouchListener((view, motionEvent) -> true);
                        scrimOverlay.setVisibility(View.VISIBLE);
                        cardView.setVisibility(View.VISIBLE);
                        edt_send_message.setEnabled(false);
                        img_back.setEnabled(false);
                        img_send_message.setEnabled(false);
                        if (urlPost.contains(".jpg")) {
                            img_show_image_click.setVisibility(View.VISIBLE);
                            playerViewChat.setVisibility(View.GONE);
                            Glide.with(ChatActivity.this).load(urlPost).error(R.drawable.icon_avatar).into(img_show_image_click);
                        } else if (urlPost.contains(".mp4")) {
                            urlVideo = urlPost;
                            playerViewChat.setVisibility(View.VISIBLE);
                            img_show_image_click.setVisibility(View.GONE);
                            playerViewChat.setPlayer(null);
                            playerViewChat.setPlayer(exoPlayer);
                            MediaItem mediaItem = MediaItem.fromUri(urlPost);
                            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
                            exoPlayer.setMediaItem(mediaItem);
                            exoPlayer.prepare();
                            exoPlayer.play();
                        }
                    }, chatRoom1.getAvtSender());
                    manager.setReverseLayout(true);
                    recyclerView.setLayoutManager(manager);
                    recyclerView.setAdapter(adapter);
                    adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                        @Override
                        public void onItemRangeInserted(int positionStart, int itemCount) {
                            super.onItemRangeInserted(positionStart, itemCount);
                            recyclerView.smoothScrollToPosition(0);
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        chatViewModel.setSeenMessage();
    }

    @Override
    protected void onStop() {
        super.onStop();
        chatViewModel.unregisterSeenMessageListening();
    }

}