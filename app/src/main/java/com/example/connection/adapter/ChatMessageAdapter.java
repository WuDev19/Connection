package com.example.connection.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.ChatMessage;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatMessageAdapter extends FirestoreRecyclerAdapter<ChatMessage, ChatMessageAdapter.ChatMessageViewHolder> {

    private final Context context;
    private final String TAG = this.getClass().getName();
    private final OnItemClick onItemClick;
    private Map<String, String> avtSender;
    private Map<String, String> aliasSender;
    private final List<String> emoteList;
    private final String myId = FirebaseAuth.getInstance().getUid();
    private final List<Integer> positionIsShowingEmote = new ArrayList<>();

    public ChatMessageAdapter(@NonNull FirestoreRecyclerOptions<ChatMessage> options, Context context, OnItemClick onItemClick, Map<String, String> avtSender, Map<String, String> aliasSender, List<String> emoteList) {
        super(options);
        this.context = context;
        this.onItemClick = onItemClick;
        this.avtSender = avtSender;
        this.aliasSender = aliasSender;
        this.emoteList = emoteList;
    }

    public interface OnItemClick {
        void onImageClick(String urlImage);

        void onMessageLongClick(String idMess, String idUser, String emote);

        void onMessageClickToShowEmote(String idMess, Map<String, String> emoteMap);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(Map<String, String> avtSender, Map<String, String> aliasSender) {
        this.avtSender = avtSender;
        this.aliasSender = aliasSender;
        notifyDataSetChanged();
    }

    public List<Integer> getPositionIsShowingEmote() {
        return positionIsShowingEmote;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position, @NonNull ChatMessage model) {

        holder.recyclerViewEmoteLeft.setAdapter(null);
        holder.recyclerViewEmoteLeft.setVisibility(View.GONE);

        holder.recyclerViewEmoteRight.setAdapter(null);
        holder.recyclerViewEmoteRight.setVisibility(View.GONE);

        if (model.getSenderId().equals(myId)) {
            holder.avt_in_chat.setVisibility(View.GONE);
            holder.tv_alias.setVisibility(View.GONE);

            holder.tv_message_left.setVisibility(View.GONE);
            if (model.getEmote().isEmpty()) {
                holder.tv_emote_right.setVisibility(View.GONE);
            } else {
                holder.tv_emote_right.setVisibility(View.VISIBLE);
                int count = 1;
                StringBuilder emote = new StringBuilder();
                for (Map.Entry<String, String> entry : model.getEmote().entrySet()) {
                    if (count <= 2) {
                        emote.append(entry.getValue());
                        count++;
                    } else break;
                }
                if (count >= 3) {
                    if (model.getEmote().size() - 2 != 0) {
                        emote.insert(0, model.getEmote().size() - 2 + "+");
                    }
                }
                holder.tv_emote_right.setText(emote);
                holder.tv_emote_right.setOnClickListener(view -> onItemClick.onMessageClickToShowEmote(model.getIdMessage(), model.getEmote()));
            }

            if (model.getMessage().contains("_")) {
                String[] mess = model.getMessage().split("_");
                holder.tv_message_left.setVisibility(View.GONE);
                holder.cardview_left.setVisibility(View.GONE);
                holder.tv_emote_left.setVisibility(View.GONE);
                holder.tv_message_right.setVisibility(View.VISIBLE);
                holder.cardview_right.setVisibility(View.VISIBLE);
                holder.tv_message_right.setText(mess[0]);
                if (model.getMessage().contains(".jpg")) {
                    Glide.with(context)
                            .load(mess[1])
                            .error(R.drawable.icon_avatar)
                            .into(holder.img_picture_right);
                } else if (model.getMessage().contains(".mp4")) {
                    Glide.with(context)
                            .load(mess[1])
                            .frame(500000)
                            .error(R.drawable.icon_avatar)
                            .into(holder.img_picture_right);
                }
                String urlPost = mess[1];
                holder.img_picture_right.setOnClickListener(view -> {
                    onItemClick.onImageClick(urlPost);
                    Log.e(TAG, "onBindViewHolder: " + urlPost);
                });
            } else {
                if (model.getMessage().contains("https://")) {
                    holder.tv_message_right.setVisibility(View.GONE);
                    holder.tv_message_left.setVisibility(View.GONE);
                    holder.cardview_left.setVisibility(View.GONE);
                    holder.tv_emote_left.setVisibility(View.GONE);
                    holder.cardview_right.setVisibility(View.VISIBLE);
                    if (model.getMessage().contains(".jpg")) {
                        Glide.with(context)
                                .load(model.getMessage())
                                .error(R.drawable.icon_avatar)
                                .into(holder.img_picture_right);
                    } else if (model.getMessage().contains(".mp4")) {
                        Glide.with(context)
                                .load(model.getMessage())
                                .frame(500000)
                                .error(R.drawable.icon_avatar)
                                .into(holder.img_picture_right);
                    }
                    String urlPost = model.getMessage();
                    holder.img_picture_right.setOnClickListener(view -> {
                        onItemClick.onImageClick(urlPost);
                        Log.e(TAG, "onBindViewHolder: " + urlPost);
                    });
                } else {
                    holder.tv_message_left.setVisibility(View.GONE);
                    holder.cardview_left.setVisibility(View.GONE);
                    holder.tv_emote_left.setVisibility(View.GONE);
                    holder.cardview_right.setVisibility(View.GONE);
                    holder.tv_message_right.setVisibility(View.VISIBLE);
                    holder.tv_message_right.setText(model.getMessage());
                }
            }
            holder.tv_message_right.setOnLongClickListener(view -> {
                if (!positionIsShowingEmote.isEmpty() && position != positionIsShowingEmote.get(0))
                    notifyItemChanged(positionIsShowingEmote.get(0));
                positionIsShowingEmote.clear();
                positionIsShowingEmote.add(position);
                holder.recyclerViewEmoteRight.setVisibility(View.VISIBLE);
                holder.recyclerViewEmoteLeft.setVisibility(View.GONE);
                EmoteAdapter emoteAdapter = new EmoteAdapter(context, emoteList, emote -> onItemClick.onMessageLongClick(model.getIdMessage(), myId, emote));
                holder.recyclerViewEmoteRight.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                holder.recyclerViewEmoteRight.setAdapter(emoteAdapter);
                return true;
            });
        } else {
            holder.avt_in_chat.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(avtSender.get(model.getSenderId()))
                    .error(R.drawable.icon_avatar)
                    .into(holder.avt_in_chat);
            holder.tv_alias.setVisibility(View.VISIBLE);
            String alias = aliasSender.get(model.getSenderId());
            if (alias != null) {
                if (alias.contains("_")) {
                    holder.tv_alias.setText(alias.split("_")[1]);
                } else {
                    holder.tv_alias.setText(alias);
                }
            }

            holder.tv_emote_right.setVisibility(View.GONE);
            if (model.getEmote().isEmpty()) {
                holder.tv_emote_left.setVisibility(View.GONE);
            } else {
                holder.tv_emote_left.setVisibility(View.VISIBLE);
                int count = 1;
                StringBuilder emote = new StringBuilder();
                for (Map.Entry<String, String> entry : model.getEmote().entrySet()) {
                    if (count <= 2) {
                        emote.append(entry.getValue());
                        count++;
                    } else break;
                }
                if (count >= 3) {
                    if (model.getEmote().size() - 2 != 0) {
                        emote.insert(0, model.getEmote().size() - 2 + "+");
                    }
                }
                holder.tv_emote_left.setText(emote.toString());
                holder.tv_emote_left.setOnClickListener(view -> onItemClick.onMessageClickToShowEmote(model.getIdMessage(), model.getEmote()));
            }

            if (model.getMessage().contains("_")) {
                String[] mess = model.getMessage().split("_");
                holder.tv_message_left.setVisibility(View.VISIBLE);
                holder.cardview_left.setVisibility(View.VISIBLE);
                holder.tv_message_right.setVisibility(View.GONE);
                holder.cardview_right.setVisibility(View.GONE);
                holder.tv_emote_right.setVisibility(View.GONE);
                holder.tv_message_left.setText(mess[0]);
                if (model.getMessage().contains(".jpg")) {
                    Glide.with(context)
                            .load(mess[1])
                            .error(R.drawable.icon_avatar)
                            .into(holder.img_picture_left);
                } else if (model.getMessage().contains(".mp4")) {
                    Glide.with(context)
                            .load(mess[1])
                            .frame(500000)
                            .error(R.drawable.icon_avatar)
                            .into(holder.img_picture_left);
                }
                String urlPost = mess[1];
                holder.img_picture_left.setOnClickListener(view -> onItemClick.onImageClick(urlPost));
            } else {
                if (model.getMessage().contains("https://")) {
                    Log.e(TAG, "This is picture message");
                    holder.tv_message_right.setVisibility(View.GONE);
                    holder.tv_message_left.setVisibility(View.GONE);
                    holder.cardview_right.setVisibility(View.GONE);
                    holder.tv_emote_right.setVisibility(View.GONE);
                    holder.cardview_left.setVisibility(View.VISIBLE);
                    if (model.getMessage().contains(".jpg")) {
                        Glide.with(context)
                                .load(model.getMessage())
                                .error(R.drawable.icon_avatar)
                                .into(holder.img_picture_left);
                    } else if (model.getMessage().contains(".mp4")) {
                        Glide.with(context)
                                .load(model.getMessage())
                                .frame(500000)
                                .error(R.drawable.icon_avatar)
                                .into(holder.img_picture_left);
                    }
                    String urlPost = model.getMessage();
                    holder.img_picture_left.setOnClickListener(view -> onItemClick.onImageClick(urlPost));
                } else {
                    holder.tv_message_right.setVisibility(View.GONE);
                    holder.cardview_right.setVisibility(View.GONE);
                    holder.cardview_left.setVisibility(View.GONE);
                    holder.tv_emote_right.setVisibility(View.GONE);
                    holder.tv_message_left.setVisibility(View.VISIBLE);
                    holder.tv_message_left.setText(model.getMessage());
                }
            }
            holder.tv_message_left.setOnLongClickListener(view -> {
                if (!positionIsShowingEmote.isEmpty() && position != positionIsShowingEmote.get(0))
                    notifyItemChanged(positionIsShowingEmote.get(0));
                positionIsShowingEmote.clear();
                positionIsShowingEmote.add(position);
                holder.recyclerViewEmoteLeft.setVisibility(View.VISIBLE);
                holder.recyclerViewEmoteRight.setVisibility(View.GONE);
                EmoteAdapter emoteAdapter = new EmoteAdapter(context, emoteList, emote -> onItemClick.onMessageLongClick(model.getIdMessage(), myId, emote));
                holder.recyclerViewEmoteLeft.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                holder.recyclerViewEmoteLeft.setAdapter(emoteAdapter);
                return true;
            });
        }
    }

    @NonNull
    @Override
    public ChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_layout_between_2_user, parent, false);
        return new ChatMessageViewHolder(view);
    }

    public static class ChatMessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView tv_message_left, tv_message_right, tv_alias, tv_emote_left, tv_emote_right;
        private final ImageView img_picture_left, img_picture_right;
        private final CardView cardview_left, cardview_right;
        private final CircleImageView avt_in_chat;
        private final RecyclerView recyclerViewEmoteLeft, recyclerViewEmoteRight;

        public ChatMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_message_left = itemView.findViewById(R.id.tv_message_left);
            tv_message_right = itemView.findViewById(R.id.tv_message_right);
            img_picture_left = itemView.findViewById(R.id.img_picture_left);
            img_picture_right = itemView.findViewById(R.id.img_picture_right);
            cardview_left = itemView.findViewById(R.id.cardview_left);
            cardview_right = itemView.findViewById(R.id.cardview_right);
            avt_in_chat = itemView.findViewById(R.id.avt_user_in_chat);
            tv_alias = itemView.findViewById(R.id.tv_alias);
            recyclerViewEmoteLeft = itemView.findViewById(R.id.recycler_view_emote_left);
            recyclerViewEmoteRight = itemView.findViewById(R.id.recycler_view_emote_right);
            tv_emote_left = itemView.findViewById(R.id.tv_emote_in_chat_left);
            tv_emote_right = itemView.findViewById(R.id.tv_emote_in_chat_right);
        }
    }

}
