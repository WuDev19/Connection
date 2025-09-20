package com.example.connection.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.ChatRoom;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SearchRecentChatAdapter extends RecyclerView.Adapter<SearchRecentChatAdapter.SearchRecentChatViewHolder> {

    private final Context context;
    private List<ChatRoom> chatRoomList;
    private final OnItemClick click;

    public interface OnItemClick{
        void onItemClick(ChatRoom chatRoom);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<ChatRoom> list){
        this.chatRoomList = list;
        notifyDataSetChanged();
    }

    public SearchRecentChatAdapter(Context context, List<ChatRoom> chatRoomList, OnItemClick click) {
        this.context = context;
        this.chatRoomList = chatRoomList;
        this.click = click;
    }

    @NonNull
    @Override
    public SearchRecentChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_recent_chat_item, parent, false);
        return new SearchRecentChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchRecentChatViewHolder holder, int position) {
        ChatRoom chatRoom = chatRoomList.get(position);
        if(chatRoom != null){
            if(chatRoom.getUserId().size() >= 3){
                Glide.with(context)
                        .load(chatRoom.getChatRoomAvt())
                        .error(R.drawable.icon_avatar)
                        .into(holder.circleImageView);
                holder.materialTextView.setText(chatRoom.getChatName());
            }
            else {
                String otherId = getOtherUserId(chatRoom.getUserId());
                Glide.with(context)
                        .load(chatRoom.getAvtSender().get(otherId))
                        .error(R.drawable.icon_avatar)
                        .into(holder.circleImageView);
                String alias = chatRoom.getAlias().get(otherId);
                if(alias != null){
                    if (alias.contains("_")) {
                        holder.materialTextView.setText(alias.split("_")[1]);
                    }
                    else holder.materialTextView.setText(alias);
                }

            }
            holder.linearLayoutSearch.setOnClickListener(view -> click.onItemClick(chatRoom));
        }
    }

    @Override
    public int getItemCount() {
        if(!chatRoomList.isEmpty()) return chatRoomList.size();
        return 0;
    }

    public static class SearchRecentChatViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout linearLayoutSearch;
        private final CircleImageView circleImageView;
        private final MaterialTextView materialTextView;

        public SearchRecentChatViewHolder(@NonNull View itemView) {
            super(itemView);
            linearLayoutSearch = itemView.findViewById(R.id.search_recent_chat);
            circleImageView = itemView.findViewById(R.id.recent_chat_avt);
            materialTextView = itemView.findViewById(R.id.tv_recent_chat_name);
        }
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

}
