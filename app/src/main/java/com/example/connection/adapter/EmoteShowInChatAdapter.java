package com.example.connection.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.EmoteModel;

import java.util.List;


import de.hdodenhof.circleimageview.CircleImageView;

public class EmoteShowInChatAdapter extends RecyclerView.Adapter<EmoteShowInChatAdapter.EmoteShowInChatViewHolder> {

    private final Context context;
    private final List<EmoteModel> emoteModels;
    private final OnItemClick onItemClick;

    public EmoteShowInChatAdapter(Context context, List<EmoteModel> emoteModels, OnItemClick onItemClick) {
        this.context = context;
        this.emoteModels = emoteModels;
        this.onItemClick = onItemClick;
    }

    public interface OnItemClick{
        void onClick(String idUser);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<EmoteModel> list){
        this.emoteModels.clear();
        emoteModels.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmoteShowInChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_layout_emote_show_item, parent, false);
        return new EmoteShowInChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmoteShowInChatViewHolder holder, int position) {
        EmoteModel emoteModel = emoteModels.get(position);
        if(emoteModel != null){
            Glide.with(context)
                    .load(emoteModel.getUrlAvt())
                    .error(R.drawable.icon_avatar)
                    .into(holder.avt);
            holder.username.setText(emoteModel.getUsername());
            holder.emote.setText(emoteModel.getEmote());
            holder.relative_emote.setOnClickListener(view -> onItemClick.onClick(emoteModel.getIdUser()));
        }
    }

    @Override
    public int getItemCount() {
        if(!emoteModels.isEmpty()) return emoteModels.size();
        return 0;
    }

    public static class EmoteShowInChatViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView avt;
        private final TextView username;
        private final TextView emote;
        private final RelativeLayout relative_emote;

        public EmoteShowInChatViewHolder(@NonNull View itemView) {
            super(itemView);
            avt = itemView.findViewById(R.id.emote_avt);
            username = itemView.findViewById(R.id.emote_username);
            emote = itemView.findViewById(R.id.emote_emote);
            relative_emote = itemView.findViewById(R.id.relative_emote);
        }
    }

}
