package com.example.connection.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.ChatRoom;
import com.example.connection.ui.viewmodel.RecentChatViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentChatUserAdapter extends FirestoreRecyclerAdapter<ChatRoom, RecentChatUserAdapter.RecentChatUserViewHolder> {

    private final Context context;
    private final setOnItemClick set;
    private final String TAG = getClass().getName();
    private final RecentChatViewModel recentChatViewModel;
    private final LifecycleOwner lifecycleOwner;

    public RecentChatUserAdapter(@NonNull FirestoreRecyclerOptions<ChatRoom> options, Context context, setOnItemClick set, RecentChatViewModel recentChatViewModel, LifecycleOwner lifecycleOwner) {
        super(options);
        this.context = context;
        this.set = set;
        this.recentChatViewModel = recentChatViewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    public interface setOnItemClick {
        void clickItem(ChatRoom chatRoom, TextView tv_message);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onBindViewHolder(@NonNull RecentChatUserViewHolder holder, int position, @NonNull ChatRoom model) {
        if (holder.getBindingAdapterPosition() != position) return;
        if (model.getIsRandom() == 1) {
            Glide.with(context)
                    .load(model.getChatRoomAvt())
                    .error(R.drawable.anonymous_avt)
                    .into(holder.img_avatar);
            holder.tv_username.setText(model.getChatName() != null ? model.getChatName() : "");
            if (model.getLastMessageSenderId() != null) {
                if (model.getLastMessageSenderId().equals(FirebaseAuth.getInstance().getUid())) {
                    holder.tv_message.setText("Bạn: " + (model.getLastMessage() != null ? model.getLastMessage() : ""));
                } else {
                    holder.tv_message.setText(model.getLastMessage() != null ? model.getLastMessage() : "");
                }
            } else {
                holder.tv_message.setText(model.getLastMessage() != null ? model.getLastMessage() : "");
            }
        } else {
            List<String> userIds = model.getUserId();
            if (userIds != null && userIds.size() <= 2) {
                final String currentChatRoomId = model.getChatRoomId();
                holder.itemView.setTag(currentChatRoomId);
                holder.img_active.setVisibility(View.GONE);
                holder.tv_username.setText("");
                holder.tv_message.setText("");
                holder.img_avatar.setImageResource(R.drawable.icon_avatar);

                recentChatViewModel.getUserInfo(model, otherUser -> {
                    if (!currentChatRoomId.equals(holder.itemView.getTag())) {
                        return;
                    }
                    if (otherUser == null) return;
                    if (otherUser.isActive()) {
                        holder.img_active.setVisibility(View.VISIBLE);
                    } else {
                        holder.img_active.setVisibility(View.GONE);
                    }
                    String alias = null;
                    if (model.getAlias() != null && otherUser.getId() != null) {
                        alias = model.getAlias().get(otherUser.getId());
                    }
                    if (alias != null && !alias.isEmpty()) {
                        if (alias.contains("_")) {
                            holder.tv_username.setText(alias.split("_")[1]);
                        } else {
                            holder.tv_username.setText(alias);
                        }
                    } else {
                        holder.tv_username.setText(otherUser.getUserName() != null ? otherUser.getUserName() : "");
                    }
                    Glide.with(holder.itemView.getContext())
                            .load(otherUser.getAvatarUser())
                            .error(R.drawable.icon_avatar)
                            .into(holder.img_avatar);
                    if (model.getLastMessage() != null) {
                        String senderId = model.getLastMessageSenderId();
                        boolean isMe = senderId != null && !senderId.equals(otherUser.getId());
                        String prefix = isMe ? "Bạn: " : "";
                        String lastMsg = model.getLastMessage();
                        if (lastMsg.contains("_")) {
                            String[] mess = lastMsg.split("_");
                            holder.tv_message.setText(prefix + mess[0]);
                        } else if (lastMsg.contains("https://")) {
                            holder.tv_message.setText(prefix + "Đã gửi một ảnh");
                        } else {
                            holder.tv_message.setText(prefix + lastMsg);
                        }
                    } else {
                        holder.tv_message.setText("");
                    }
                });
            } else {
                Glide.with(context).load(model.getChatRoomAvt()).error(R.drawable.icon_avatar).into(holder.img_avatar);
                holder.tv_username.setText(model.getChatName() != null ? model.getChatName() : "");
                if (model.getLastMessageSenderId() != null && model.getLastMessage() != null) {
                    if (model.getLastMessageSenderId().equals(FirebaseAuth.getInstance().getUid())) {
                        String msg = model.getLastMessage();
                        if (msg.contains(": ")) {
                            String[] parts = msg.split(": ");
                            holder.tv_message.setText("Bạn: " + (parts.length > 1 ? parts[1] : parts[0]));
                        } else {
                            holder.tv_message.setText("Bạn: " + msg);
                        }
                    } else {
                        holder.tv_message.setText(model.getLastMessage());
                    }
                } else {
                    holder.tv_message.setText(model.getLastMessage() != null ? model.getLastMessage() : "");
                }
            }
        }
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDay = new SimpleDateFormat("E, dd HH:mm", new Locale("vi", "VN"));
        if (model.getLastMessageTimestamp() != null) {
            String fullTime = formatDay.format(model.getLastMessageTimestamp().toDate());
            holder.tv_time.setText(getTimeAgo(fullTime, model.getLastMessageTimestamp()));
        }

        String currentUid = FirebaseAuth.getInstance().getUid();
        boolean isUnread = false;
        if (currentUid != null && model.getUnreadCounts() != null && model.getUnreadCounts().containsKey(currentUid)) {
            Long unreadCount = model.getUnreadCounts().get(currentUid);
            isUnread = (unreadCount != null && unreadCount > 0);
        }
        if (isUnread) {
            holder.tv_message.setTypeface(null, Typeface.BOLD);
        } else {
            holder.tv_message.setTypeface(null, Typeface.NORMAL);
        }
        holder.relativeLayout.setOnClickListener(view -> set.clickItem(model, holder.tv_message));
    }

    @NonNull
    @Override
    public RecentChatUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_recent_chat, parent, false);
        return new RecentChatUserViewHolder(view);
    }

    public static class RecentChatUserViewHolder extends RecyclerView.ViewHolder {

        ImageView img_avatar, img_active;
        TextView tv_username, tv_message, tv_time;
        RelativeLayout relativeLayout;

        public RecentChatUserViewHolder(@NonNull View itemView) {
            super(itemView);
            img_avatar = itemView.findViewById(R.id.img_avatar_recent_chat);
            tv_username = itemView.findViewById(R.id.tv_username_recent_chat);
            tv_message = itemView.findViewById(R.id.tv_recent_message);
            tv_time = itemView.findViewById(R.id.tv_time);
            relativeLayout = itemView.findViewById(R.id.relative_recent_chat);
            img_active = itemView.findViewById(R.id.img_active);
        }
    }

    public String getTimeAgo(String time, Timestamp timestamp) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("E, dd HH:mm", new Locale("vi", "VN"));
        String currentTime = format.format(new Date().getTime());
        currentTime = currentTime.replace("Th ", "T");
        if (currentTime.contains(".")) {
            currentTime = currentTime.replace(".", "");
        }
        time = time.replace("Th ", "T");
        if (time.contains(".")) {
            time = time.replace(".", "");
        }
        Log.e(TAG, "getTimeAgo: " + time);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat fullTime = new SimpleDateFormat("E, yyyy-MM-dd", new Locale("vi", "VN"));
        String full = fullTime.format(timestamp.toDate());
        String date = time.substring(0, 2);
        Log.e(TAG, "getTimeAgo: " + date);
        String hourAndMinute = time.substring(7, 12);
        int day = Integer.parseInt(time.substring(4, 6));
        int dayCurrent = Integer.parseInt(currentTime.substring(4, 6));
        int signalDay = dayCurrent - day;
        if (signalDay > 7) {
            return full;
        } else {
            if (signalDay >= 1) {
                return date;
            }
        }
        return hourAndMinute;
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
    }

}
