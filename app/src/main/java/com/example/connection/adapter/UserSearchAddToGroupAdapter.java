package com.example.connection.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.User;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserSearchAddToGroupAdapter extends RecyclerView.Adapter<UserSearchAddToGroupAdapter.UserSearchAddToGroupViewHolder> {

    private final Context context;
    private List<User> userList;
    private final List<User> avtUserInGroup = new ArrayList<>();
    private final List<String> userInGroup = new ArrayList<>();

    public UserSearchAddToGroupAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<User> list) {
        this.userList = list;
        notifyDataSetChanged();
    }

    public void setUser(User user){
        avtUserInGroup.add(user);
        userInGroup.add(user.getId());
    }

    @NonNull
    @Override
    public UserSearchAddToGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_group_item, parent, false);
        return new UserSearchAddToGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserSearchAddToGroupViewHolder holder, int position) {
        User user = userList.get(position);
        if (user == null || Objects.equals(user.getId(), FirebaseAuth.getInstance().getUid())) {
            holder.textView.setVisibility(View.GONE);
            holder.circleImageView.setVisibility(View.GONE);
            holder.checkBox.setVisibility(View.GONE);
            return;
        }
        holder.textView.setVisibility(View.VISIBLE);
        holder.circleImageView.setVisibility(View.VISIBLE);
        holder.checkBox.setVisibility(View.VISIBLE);
        holder.checkBox.setOnCheckedChangeListener((compoundButton, check) -> {
            if (check) {
                avtUserInGroup.add(user);
                userInGroup.add(user.getId());
            }
            else {
                avtUserInGroup.remove(user);
                userInGroup.remove(user.getId());
            }
        });
        Glide.with(context).load(user.getAvatarUser()).error(R.drawable.icon_avatar).into(holder.circleImageView);
        holder.textView.setText(user.getUserName());
    }

    @Override
    public int getItemCount() {
        if (!userList.isEmpty()) return userList.size();
        return 0;
    }

    public List<User> getAvtUserInGroup() {
        return avtUserInGroup;
    }

    public List<String> getUserInGroup(){
        return userInGroup;
    }

    public static class UserSearchAddToGroupViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView circleImageView;
        private final TextView textView;
        private final CheckBox checkBox;

        public UserSearchAddToGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            circleImageView = itemView.findViewById(R.id.avt_user_group);
            textView = itemView.findViewById(R.id.tv_username_group);
            checkBox = itemView.findViewById(R.id.check_box1);
        }
    }
}
