package com.example.connection.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connection.R;
import com.example.connection.viewmodel.NotificationViewModel;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private List<String> mList;
    private final onItemClick click;
    private final NotificationViewModel notificationViewModel;
    private final LifecycleOwner lifecycleOwner;

    public interface onItemClick{
        void onAccept(String otherUserId, int position);
        void onCancel(String otherUserId, int position);
    }

    public NotificationAdapter(Context context, List<String> mList, onItemClick click,
                               NotificationViewModel notificationViewModel, LifecycleOwner lifecycleOwner) {
        this.context = context;
        this.mList = mList;
        this.click = click;
        this.notificationViewModel = notificationViewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<String> list){
        this.mList = list;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        String id = mList.get(position);
        if(id == null || id.equals(notificationViewModel.getCurrentUserId())) return;
        notificationViewModel.getUser(id).observe(lifecycleOwner, user -> {
            if(user != null){
                holder.tv_add_friend.setText("Bạn có lời mời kết bạn từ " + user.getUserName());
                holder.btn_accept.setOnClickListener(view -> click.onAccept(id, position));
                holder.btn_cancel.setOnClickListener(view -> click.onCancel(id, position));
            }
        });
    }

    @Override
    public int getItemCount() {
        if(mList != null) return mList.size();
        return 0;
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {

        TextView tv_add_friend;
        ImageButton btn_accept, btn_cancel;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_add_friend = itemView.findViewById(R.id.tv_add_friend);
            btn_accept = itemView.findViewById(R.id.btn_accept);
            btn_cancel = itemView.findViewById(R.id.btn_cancel);
        }
    }
}
