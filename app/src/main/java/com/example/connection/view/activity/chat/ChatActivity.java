package com.example.connection.view.activity.chat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import com.example.connection.view.fragment.FragmentChangeInfoChat;
import com.example.connection.viewmodel.ChatViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Query;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private CircleImageView img_avatar;
    private TextView tv_username, tv_active;
    private ImageView img_back, img_info;
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
    private ChatRoom chatRoom;
    private String otherId;
    private AlertDialog dialog;
    private boolean needRefresh = false;

    @SuppressLint({"ClickableViewAccessibility", "NotifyDataSetChanged"})
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

        img_info.setOnClickListener(view -> {
            if (chatRoom.getUserId().size() >= 3) {
                FragmentChangeInfoChat fragmentChangeInfoChat = new FragmentChangeInfoChat(() -> setUpRecyclerViewMessage(chatRoom));
                Bundle bundle = new Bundle();
                bundle.putParcelable("chat_room1", chatRoom);
                fragmentChangeInfoChat.setArguments(bundle);
                fragmentChangeInfoChat.show(getSupportFragmentManager(), fragmentChangeInfoChat.getTag());
            } else {
                PopupMenu popupMenu = new PopupMenu(ChatActivity.this, img_info);
                popupMenu.inflate(R.menu.menu_chat_options);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    popupMenu.setForceShowIcon(true);
                }
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.menu_delete) {
                        chatViewModel.getDelete();
                        return true;
                    } else if (menuItem.getItemId() == R.id.alias_chat) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        @SuppressLint("InflateParams") View view1 = getLayoutInflater().inflate(R.layout.custom_dialog_change_alias, null);
                        Button btn_cancel = view1.findViewById(R.id.btn_cancel_change_alias);
                        Button btn_remove = view1.findViewById(R.id.btn_remove_alias);
                        Button btn_confirm = view1.findViewById(R.id.btn_confirm_alias);
                        EditText edt_alias = view1.findViewById(R.id.edt_set_alias);
                        String alias = chatRoom.getAlias().get(otherId);
                        if (alias != null) {
                            if (alias.contains("_")) {
                                edt_alias.setText(alias.split("_")[1]);
                            } else {
                                edt_alias.setText(alias);
                            }
                        }
                        builder.setTitle("Chỉnh sửa biệt danh");
                        builder.setMessage("Biệt danh này chỉ được nhìn thấy trong đoạn chat");
                        builder.setView(view1);
                        dialog = builder.create();
                        Window window = dialog.getWindow();
                        if (window != null) {
                            window.setBackgroundDrawableResource(R.drawable.custom_dialog);
                            dialog.show();
                        } else {
                            Toast.makeText(this, "Có lỗi gì đó xảy ra, hãy thử lại", Toast.LENGTH_LONG).show();
                        }
                        btn_cancel.setOnClickListener(view2 -> dialog.dismiss());
                        btn_remove.setOnClickListener(view22 -> {
                            if (alias != null) {
                                if (!alias.contains("_")) return;
                                edt_alias.setText("");
                            }
                            chatViewModel.updateChatNameTwoPeople(null, otherId);
                            needRefresh = true;
                            dialog.dismiss();
                        });
                        btn_confirm.setOnClickListener(view22 -> {
                            String aliasNew = edt_alias.getText().toString().trim();
                            if (alias != null && !alias.contains("_") && alias.equals(aliasNew))
                                return;
                            if (!aliasNew.isEmpty()) {
                                chatViewModel.updateChatNameTwoPeople(aliasNew, otherId);
                                needRefresh = true;
                                dialog.dismiss();
                            }
                        });
                        return true;
                    }
                    return false;
                });
                popupMenu.show();
            }
        });

        chatViewModel.getSend().observe(this, send -> {
            if (send) {
                edt_send_message.setText("");
            }
        });

        chatViewModel.getChatRoomLiveData().observe(this, chatRoom -> {
            if (chatRoom == null) {
                chatViewModel.createNewChatRoom();
            } else {
                setUpRecyclerViewMessage(chatRoom);
                chatViewModel.listeningChatRoom();
            }
        });

        chatViewModel.getCreateNewChat().observe(this, chatRoom -> {
            if (chatRoom != null) {
                setUpRecyclerViewMessage(chatRoom);
                chatViewModel.listeningChatRoom();
            }
        });

        chatViewModel.getChatRoomListeningLiveData().observe(this, chatRoom1 -> {
            if (chatRoom1 == null) return;
            ChatActivity.this.updateChatRoomUi(chatRoom1);
            chatRoom = chatRoom1;
            if (needRefresh) {
                setUpRecyclerViewMessage(chatRoom1);
                needRefresh = false;
            }
        });

    }

    private void updateChatRoomUi(ChatRoom chatRoom1) {
        if (chatRoom == null || chatRoom1 == null) return;
        if (chatRoom1.getUserId().size() >= 3) {
            String oldName = chatRoom.getChatName();
            String newName = chatRoom1.getChatName();
            if (oldName != null && newName != null && !oldName.equals(newName)) {
                tv_username.setText(newName);
            }
            String oldAvt = chatRoom.getChatRoomAvt();
            String newAvt = chatRoom1.getChatRoomAvt();
            if (oldAvt != null && newAvt != null && !oldAvt.equals(newAvt)) {
                Glide.with(this)
                        .load(newAvt)
                        .error(R.drawable.icon_avatar)
                        .into(img_avatar);
            }
        } else {
            String alias = chatRoom1.getAlias().get(otherId);
            if (alias != null) {
                if (alias.contains("_")) {
                    tv_username.setText(alias.split("_")[1]);
                } else tv_username.setText(alias);
            }
        }
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
        img_info = findViewById(R.id.img_info);
        MaterialCardView materialCardView = findViewById(R.id.card_view_chat);
        manager = new LinearLayoutManager(ChatActivity.this);
        exoPlayer = new ExoPlayer.Builder(this).build();
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        materialCardView.setTranslationY(screenHeight);
        materialCardView.animate()
                .translationY(0f)
                .setDuration(800)
                .start();
    }

    private void getDataIntent() {
        Intent intent = getIntent();
        chatRoom = intent.getParcelableExtra("chat_room");
        if (chatRoom == null) {
            createChatRoom(intent);
        } else {
            showChatRoomExist(chatRoom);
        }
    }

    private void showChatRoomExist(ChatRoom chatRoom) {
        chatViewModel.setChatRoomId(chatRoom.getChatRoomId());
        if (chatRoom.getUserId().size() <= 2) {
            otherId = getOtherUserId(chatRoom.getUserId());
            if (otherId == null) return;
            if (otherId.contains("_delete")) {
                otherId = otherId.split("_")[0];
            }
            chatViewModel.getOtherUser(otherId).observe(this, otherUser -> {
                if (otherUser != null) {
                    String alias = chatRoom.getAlias().get(otherId);
                    if (alias != null) {
                        if (alias.contains("_")) {
                            tv_username.setText(alias.split("_")[1]);
                        } else tv_username.setText(alias);
                    }
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
        chatViewModel.getCreateOptions().removeObservers(this);
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
                    }, chatRoom1.getAvtSender(), chatRoom1.getAlias());
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
        if (chatViewModel.getChatRoom() != null) {
            chatViewModel.listeningChatRoom();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        chatViewModel.unregisterSeenMessageListening();
        chatViewModel.unregisterListeningChatRoom();
    }

}