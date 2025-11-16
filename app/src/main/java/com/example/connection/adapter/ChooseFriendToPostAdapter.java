package com.example.connection.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.SelectionFriendModel;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChooseFriendToPostAdapter extends RecyclerView.Adapter<ChooseFriendToPostAdapter.ChooseFriendToPostViewHolder> {

    private final Context context;
    private final List<SelectionFriendModel> listFriend;
    private final OnItemClickListener onItemClickListener;

    public ChooseFriendToPostAdapter(Context context, List<SelectionFriendModel> listFriend, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.listFriend = listFriend;
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onClick(SelectionFriendModel model);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<SelectionFriendModel> list) {
        listFriend.clear();
        listFriend.addAll(list);
        listFriend.replaceAll(model -> {
            model.setSelected(false);
            return model;
        });
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChooseFriendToPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_choose_friend_to_post_item, parent, false);
        return new ChooseFriendToPostViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ChooseFriendToPostViewHolder holder, int position) {
        SelectionFriendModel model = listFriend.get(position);
        if (model != null) {

            if(model.getAvt() != null){
                Glide.with(context)
                        .load(model.getAvt())
                        .error(R.drawable.icon_avatar)
                        .into(holder.img_avt);
            }
            else {
                holder.img_avt.setImageResource(R.drawable.icon_avatar);
            }

            holder.img_avt.setOnClickListener(view -> {
                holder.tv_username.setText("");
                holder.tv_username.setVisibility(View.INVISIBLE);
                boolean newState = !model.isSelected();
                model.setSelected(newState);
                if (model.isSelected()) {
                    holder.img_avt.setScaleX(1.5f);
                    holder.img_avt.setScaleY(1.5f);
                }
                else {
                    holder.img_avt.setScaleX(1f);
                    holder.img_avt.setScaleY(1f);
                }
                onItemClickListener.onClick(model);
            });

            holder.img_avt.setOnLongClickListener(view -> {
                holder.tv_username.setVisibility(View.VISIBLE);
                holder.tv_username.setText(model.getUsername());
                return true;
            });

        }
    }


    @Override
    public int getItemCount() {
        return listFriend.size();
    }

    public static class ChooseFriendToPostViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView img_avt;
        private final TextView tv_username;

        public ChooseFriendToPostViewHolder(@NonNull View itemView) {
            super(itemView);
            img_avt = itemView.findViewById(R.id.img_friend_to_post);
            tv_username = itemView.findViewById(R.id.tv_user_to_post);
        }
    }

}
