package com.example.connection.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserSearchAdapter extends FirestoreRecyclerAdapter<User, UserSearchAdapter.UserSearchViewHolder> {

    private final Context context;
    private final onItemClick itemClick;
    private final TextView tv_exist;

    public UserSearchAdapter(@NonNull FirestoreRecyclerOptions<User> options, Context context, onItemClick itemClick, TextView tv_exist) {
        super(options);
        this.context = context;
        this.itemClick = itemClick;
        this.tv_exist = tv_exist;
    }

    public interface onItemClick{
        void onChatListener(User user);
        void onAddListener(String otherUserId);
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onBindViewHolder(@NonNull UserSearchViewHolder holder, int position, @NonNull User model) {
        Log.e("search", "onBindViewHolder: ");
        holder.tv_username.setText(model.getUserName());
        Glide.with(context).load(model.getAvatarUser()).error(R.drawable.icon_avatar).into(holder.img_avatar);
        holder.tv_chat.setOnClickListener(view -> itemClick.onChatListener(model));
        holder.tv_kb.setOnClickListener(view -> itemClick.onAddListener(model.getId()));
    }

    @NonNull
    @Override
    public UserSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_layout, parent, false);
        return new UserSearchViewHolder(view);
    }

    public static class UserSearchViewHolder extends RecyclerView.ViewHolder {

        CircleImageView img_avatar;
        TextView tv_username, tv_chat, tv_kb;

        public UserSearchViewHolder(@NonNull View itemView) {
            super(itemView);
            img_avatar = itemView.findViewById(R.id.img_avatar_user);
            tv_username = itemView.findViewById(R.id.tv_name_user);
            tv_chat = itemView.findViewById(R.id.tv_chat);
            tv_kb = itemView.findViewById(R.id.tv_kb);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onDataChanged() {
        super.onDataChanged();
        Log.e("search", "onDataChanged: ");
        if(getItemCount() < 1){
            tv_exist.setVisibility(View.VISIBLE);
            tv_exist.setText("Người dùng không tồn tại");
        }
    }
}
