package com.example.connection.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.ChatMessage;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatMessageAdapter extends FirestoreRecyclerAdapter<ChatMessage, ChatMessageAdapter.ChatMessageViewHolder> {

    private final Context context;
    private final String TAG = this.getClass().getName();
    private final OnImageClick onImageClick;
    private final Map<String, String> avtSender;
    private final Map<String, String> aliasSender;

    public ChatMessageAdapter(@NonNull FirestoreRecyclerOptions<ChatMessage> options, Context context, OnImageClick onImageClick, Map<String, String> avtSender, Map<String, String> aliasSender) {
        super(options);
        this.context = context;
        this.onImageClick = onImageClick;
        this.avtSender = avtSender;
        this.aliasSender = aliasSender;
    }

    public interface OnImageClick {
        void onClick(String urlImage);
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position, @NonNull ChatMessage model) {
        if (model.getSenderId().equals(FirebaseAuth.getInstance().getUid())) {
            holder.avt_in_chat.setVisibility(View.GONE);
            holder.tv_alias.setVisibility(View.GONE);
            if (model.getMessage().contains("_")) {
                String[] mess = model.getMessage().split("_");
                holder.tv_message_left.setVisibility(View.GONE);
                holder.cardview_left.setVisibility(View.GONE);
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
                    onImageClick.onClick(urlPost);
                    Log.e(TAG, "onBindViewHolder: " + urlPost);
                });
            } else {
                if (model.getMessage().contains("https://")) {
                    holder.tv_message_right.setVisibility(View.GONE);
                    holder.tv_message_left.setVisibility(View.GONE);
                    holder.cardview_left.setVisibility(View.GONE);
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
                        onImageClick.onClick(urlPost);
                        Log.e(TAG, "onBindViewHolder: " + urlPost);
                    });
                } else {
                    holder.tv_message_left.setVisibility(View.GONE);
                    holder.cardview_left.setVisibility(View.GONE);
                    holder.cardview_right.setVisibility(View.GONE);
                    holder.tv_message_right.setVisibility(View.VISIBLE);
                    holder.tv_message_right.setText(model.getMessage());
                }
            }

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
            if (model.getMessage().contains("_")) {
                String[] mess = model.getMessage().split("_");
                holder.tv_message_left.setVisibility(View.VISIBLE);
                holder.cardview_left.setVisibility(View.VISIBLE);
                holder.tv_message_right.setVisibility(View.GONE);
                holder.cardview_right.setVisibility(View.GONE);
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
                holder.img_picture_left.setOnClickListener(view -> onImageClick.onClick(urlPost));
            } else {
                if (model.getMessage().contains("https://")) {
                    Log.e(TAG, "This is picture message");
                    holder.tv_message_right.setVisibility(View.GONE);
                    holder.tv_message_left.setVisibility(View.GONE);
                    holder.cardview_right.setVisibility(View.GONE);
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
                    holder.img_picture_left.setOnClickListener(view -> onImageClick.onClick(urlPost));
                } else {
                    holder.tv_message_right.setVisibility(View.GONE);
                    holder.cardview_right.setVisibility(View.GONE);
                    holder.cardview_left.setVisibility(View.GONE);
                    holder.tv_message_left.setVisibility(View.VISIBLE);
                    holder.tv_message_left.setText(model.getMessage());
                }
            }
        }
    }

    @NonNull
    @Override
    public ChatMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_layout_between_2_user, parent, false);
        return new ChatMessageViewHolder(view);
    }

    public static class ChatMessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView tv_message_left, tv_message_right, tv_alias;
        private final ImageView img_picture_left, img_picture_right;
        private final CardView cardview_left, cardview_right;
        private final CircleImageView avt_in_chat;

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
        }
    }

}
