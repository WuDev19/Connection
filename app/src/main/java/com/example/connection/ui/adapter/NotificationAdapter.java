package com.example.connection.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connection.R;
import com.example.connection.model.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private final Context context;
    private final onItemClick click;
    private List<String> items = new ArrayList<>();
    private Map<String, User> users = new HashMap<>();
    private Set<String> pending = new HashSet<>();

    public interface onItemClick {
        void onAccept(String id, int position);
        void onCancel(String id, int position);
    }

    public NotificationAdapter(Context context, List<String> ids, onItemClick click) {
        this.context = context;
        this.click = click;
        if (ids != null) items = new ArrayList<>(ids);
    }

    public void setData(List<String> ids) {
        items = ids == null ? new ArrayList<>() : new ArrayList<>(ids);
        notifyDataSetChanged();
    }

    public void setUsers(Map<String, User> data) {
        users = new HashMap<>(data);
        notifyDataSetChanged();
    }

    public void setPending(Set<String> ids) {
        pending = new HashSet<>(ids);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new NotificationViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_notification, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        String id = items.get(position);
        User user = users.get(id);
        holder.text.setText("Bạn có lời mời kết bạn từ " + (user == null ? "người dùng" : user.getUserName()));
        holder.accept.setEnabled(!pending.contains(id));
        holder.cancel.setEnabled(!pending.contains(id));
        holder.accept.setOnClickListener(v -> dispatch(holder, true));
        holder.cancel.setOnClickListener(v -> dispatch(holder, false));
    }

    private void dispatch(NotificationViewHolder holder, boolean accept) {
        int position = holder.getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;
        String id = items.get(position);
        if (pending.contains(id)) return;
        if (accept) click.onAccept(id, position);
        else click.onCancel(id, position);
    }

    @Override public int getItemCount() { return items.size(); }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        final TextView text;
        final ImageButton accept, cancel;
        NotificationViewHolder(View view) {
            super(view);
            text = view.findViewById(R.id.tv_add_friend);
            accept = view.findViewById(R.id.btn_accept);
            cancel = view.findViewById(R.id.btn_cancel);
        }
    }
}
