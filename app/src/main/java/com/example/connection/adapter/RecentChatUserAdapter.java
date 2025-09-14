package com.example.connection.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.ChatRoom;
import com.example.connection.viewmodel.RecentChatViewModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
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

    //them phan chinh sua nhom
    //update user thi update nhung cai lien quan
    @SuppressLint("SetTextI18n")
    @Override
    protected void onBindViewHolder(@NonNull RecentChatUserViewHolder holder, int position, @NonNull ChatRoom model) {
        Log.e("Checkchat", "onBindViewHolder: " + position);
        if (holder.getBindingAdapterPosition() != position) return;
        if (model.getUserId().size() <= 2) {
            recentChatViewModel.getInfo(model).observe(lifecycleOwner, otherUser -> {
                if (otherUser == null) return;
                if (otherUser.isActive()) {
                    holder.img_active.setVisibility(View.VISIBLE);
                } else {
                    Log.e(TAG, "Hide");
                    holder.img_active.setVisibility(View.GONE);
                }
                holder.tv_username.setText(otherUser.getUserName());
                Glide.with(holder.itemView.getContext()).load(otherUser.getAvatarUser()).error(R.drawable.icon_avatar).into(holder.img_avatar);
                if (model.getLastMessage() != null) {
                    if (!model.getLastMessageSenderId().equals(otherUser.getId())) {
                        if (model.getLastMessage().contains("_")) {
                            String[] mess = model.getLastMessage().split("_");
                            holder.tv_message.setText("Bạn: " + mess[0]);
                        } else {
                            if (model.getLastMessage().contains("https://")) {
                                holder.tv_message.setText("Bạn: " + "Đã gửi một ảnh");
                            } else {
                                holder.tv_message.setText("Bạn: " + model.getLastMessage());
                                Log.e("checkk", "onBindViewHolder: khac id");
                            }

                        }
                    } else {
                        if (model.getLastMessage().contains("_")) {
                            String[] mess = model.getLastMessage().split("_");
                            holder.tv_message.setText(mess[0]);
                        } else {
                            if (model.getLastMessage().contains("https://")) {
                                holder.tv_message.setText("Đã gửi một ảnh");
                            } else {
                                holder.tv_message.setText(model.getLastMessage());
                            }
                        }
                    }
                }
            });
        } else {
            Glide.with(context).load(model.getChatRoomAvt()).error(R.drawable.icon_avatar).into(holder.img_avatar);
            holder.tv_username.setText(model.getChatName());
            if(model.getLastMessageSenderId() != null){
                if(model.getLastMessageSenderId().equals(FirebaseAuth.getInstance().getUid())){
                    holder.tv_message.setText(model.getLastMessage().split(": ")[1]);
                }
                else {
                    holder.tv_message.setText(model.getLastMessage());
                }
            }
        }
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatDay = new SimpleDateFormat("E, dd HH:mm", new Locale("vi", "VN"));
        if (model.getLastMessageTimestamp() != null) {
            String fullTime = formatDay.format(model.getLastMessageTimestamp().toDate());
            holder.tv_time.setText(getTimeAgo(fullTime, model.getLastMessageTimestamp()));
        }
        recentChatViewModel.getIsNotSeen(model).observe(lifecycleOwner, isNotSeen -> {
            if (isNotSeen) {
                Log.e("kiemtraseen", "onBindViewHolder: seen");
                holder.tv_message.setTypeface(null, Typeface.BOLD);
            } else holder.tv_message.setTypeface(null, Typeface.NORMAL);
        });
        holder.relativeLayout.setOnClickListener(view -> set.clickItem(model, holder.tv_message));
        holder.relativeLayout.setOnLongClickListener(view -> {
            Log.e(TAG, "onChanged: ");
            PopupMenu popupMenu = new PopupMenu(context, holder.relativeLayout);
            popupMenu.inflate(R.menu.menu_chat_options);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                popupMenu.setForceShowIcon(true);
            }
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.menu_delete) {
                    recentChatViewModel.getDelete(model);
                }
                return false;
            });
            popupMenu.show();
            return true;
        });
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
