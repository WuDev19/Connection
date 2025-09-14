package com.example.connection.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.User;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import de.hdodenhof.circleimageview.CircleImageView;

public class ShowFriendAdapter extends FirestoreRecyclerAdapter<User, ShowFriendAdapter.ShowFriendViewHolder> {

    private final Context context;
    private final onItemClickListener clickListener;

    public interface onItemClickListener{
        void deleteFriend(String friendId);
        void chatFriend(User friend);
    }

    public ShowFriendAdapter(@NonNull FirestoreRecyclerOptions<User> options, Context context, onItemClickListener clickListener) {
        super(options);
        this.context = context;
        this.clickListener = clickListener;
    }

    @Override
    protected void onBindViewHolder(@NonNull ShowFriendViewHolder holder, int position, @NonNull User model) {
        String currentId = FirebaseAuth.getInstance().getUid();
        if(!model.getId().equals(currentId)){
            Glide.with(context).load(model.getAvatarUser()).error(R.drawable.icon_avatar).into(holder.img_avatar_friend);
            holder.tv_username_friend.setText(model.getUserName());
            holder.btn_delete_friend.setOnClickListener(view -> clickListener.deleteFriend(model.getId()));
            holder.btn_chat_friend.setOnClickListener(view -> clickListener.chatFriend(model));
        }
        else {
            holder.btn_chat_friend.setVisibility(View.GONE);
            holder.btn_delete_friend.setVisibility(View.GONE);
            holder.tv_username_friend.setVisibility(View.GONE);
            holder.img_avatar_friend.setVisibility(View.GONE);
        }
    }

    @NonNull
    @Override
    public ShowFriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_show_friend, parent, false);
        return new ShowFriendViewHolder(view);
    }

    public static class ShowFriendViewHolder extends RecyclerView.ViewHolder {

        CircleImageView img_avatar_friend;
        TextView tv_username_friend;
        ImageButton btn_delete_friend, btn_chat_friend;

        public ShowFriendViewHolder(@NonNull View itemView) {
            super(itemView);
            img_avatar_friend = itemView.findViewById(R.id.img_avatar_friend);
            tv_username_friend = itemView.findViewById(R.id.tv_username_friend);
            btn_delete_friend = itemView.findViewById(R.id.btn_delete_friend);
            btn_chat_friend = itemView.findViewById(R.id.btn_chat_friend);
        }
    }
}
