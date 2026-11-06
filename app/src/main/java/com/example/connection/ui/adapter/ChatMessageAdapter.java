package com.example.connection.ui.adapter;

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
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ChatMessageViewHolder> {

    private final Context context;
    private final String TAG = this.getClass().getName();
    private final OnItemClick onItemClick;
    private Map<String, String> avtSender;
    private Map<String, String> aliasSender;
    private final List<String> emoteList;
    private final String myId = FirebaseAuth.getInstance().getUid();
    private final List<Integer> positionIsShowingEmote = new ArrayList<>();
    private final List<ChatMessage> messageList = new ArrayList<>();

    public ChatMessageAdapter(Context context, OnItemClick onItemClick, Map<String, String> avtSender, Map<String, String> aliasSender, List<String> emoteList) {
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

    private void renderMedia(ImageView imageView, String mediaUrl, String messageType) {
        if ("VIDEO".equals(messageType) || (mediaUrl != null && mediaUrl.contains(".mp4"))) {
            Glide.with(context)
                    .load(mediaUrl)
                    .frame(500000)
                    .error(R.drawable.icon_avatar)
                    .into(imageView);
        } else {
            Glide.with(context)
                    .load(mediaUrl)
                    .error(R.drawable.icon_avatar)
                    .into(imageView);
        }
    }

    private void bindEmote(TextView tvEmote, Map<String, String> emoteMap, String idMessage) {
        if (emoteMap == null || emoteMap.isEmpty()) {
            tvEmote.setVisibility(View.GONE);
            return;
        }
        tvEmote.setVisibility(View.VISIBLE);
        int count = 1;
        StringBuilder emoteStr = new StringBuilder();
        for (Map.Entry<String, String> entry : emoteMap.entrySet()) {
            if (count <= 2) {
                emoteStr.append(entry.getValue());
                count++;
            } else {
                break;
            }
        }
        if (count >= 3 && emoteMap.size() - 2 > 0) {
            emoteStr.insert(0, (emoteMap.size() - 2) + "+");
        }
        tvEmote.setText(emoteStr.toString());
        tvEmote.setOnClickListener(view -> onItemClick.onMessageClickToShowEmote(idMessage, emoteMap));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMessages(List<ChatMessage> list) {
        this.messageList.clear();
        if (list != null) {
            this.messageList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void addOrUpdateMessage(ChatMessage message) {
        if (message == null || message.getIdMessage() == null) return;
        int idx = findPositionById(message.getIdMessage());
        if (idx != -1) {
            messageList.set(idx, message);
            notifyItemChanged(idx);
        } else {
            messageList.add(0, message);
            notifyItemInserted(0);
        }
    }

    public void updateMessage(ChatMessage message) {
        if (message == null || message.getIdMessage() == null) return;
        int idx = findPositionById(message.getIdMessage());
        if (idx != -1) {
            messageList.set(idx, message);
            notifyItemChanged(idx);
        }
    }

    public void removeMessage(String idMessage) {
        if (idMessage == null) return;
        int idx = findPositionById(idMessage);
        if (idx != -1) {
            messageList.remove(idx);
            notifyItemRemoved(idx);
        }
    }

    public void addOlderMessages(List<ChatMessage> olderMessages) {
        if (olderMessages == null || olderMessages.isEmpty()) return;
        int startPos = messageList.size();
        messageList.addAll(olderMessages);
        notifyItemRangeInserted(startPos, olderMessages.size());
    }

    public Long getOldestTimestamp() {
        if (messageList.isEmpty()) return null;
        return messageList.get(messageList.size() - 1).getTimestamp();
    }

    public int findPositionById(String idMessage) {
        for (int i = 0; i < messageList.size(); i++) {
            if (idMessage.equals(messageList.get(i).getIdMessage())) {
                return i;
            }
        }
        return -1;
    }

    public ChatMessage getItem(int position) {
        return (position >= 0 && position < messageList.size()) ? messageList.get(position) : null;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ChatMessageViewHolder holder, int position) {
        ChatMessage model = messageList.get(position);
        if (model == null) return;

        holder.recyclerViewEmoteLeft.setAdapter(null);
        holder.recyclerViewEmoteLeft.setVisibility(View.GONE);

        holder.recyclerViewEmoteRight.setAdapter(null);
        holder.recyclerViewEmoteRight.setVisibility(View.GONE);

        String rawMessage = model.getMessage() != null ? model.getMessage() : "";
        String mediaUrl = model.getMediaUrl();
        String messageType = model.getMessageType();
        boolean hasMedia = mediaUrl != null && !mediaUrl.isEmpty();
        boolean hasCaption = rawMessage.contains("_");
        String textMessage = hasCaption ? rawMessage.split("_")[0] : (hasMedia ? "" : rawMessage);

        if (model.getSenderId().equals(myId)) {
            // Tin nhắn của tôi (Bên phải)
            holder.avt_in_chat.setVisibility(View.GONE);
            holder.tv_alias.setVisibility(View.GONE);
            holder.tv_message_left.setVisibility(View.GONE);
            holder.cardview_left.setVisibility(View.GONE);
            holder.tv_emote_left.setVisibility(View.GONE);

            bindEmote(holder.tv_emote_right, model.getEmote(), model.getIdMessage());

            if (hasMedia) {
                holder.cardview_right.setVisibility(View.VISIBLE);
                renderMedia(holder.img_picture_right, mediaUrl, messageType);
                final String finalMediaUrl = mediaUrl;
                holder.img_picture_right.setOnClickListener(view -> {
                    onItemClick.onImageClick(finalMediaUrl);
                    Log.e(TAG, "onBindViewHolder: " + finalMediaUrl);
                });
                if (hasCaption && !textMessage.isEmpty()) {
                    holder.tv_message_right.setVisibility(View.VISIBLE);
                    holder.tv_message_right.setText(textMessage);
                } else {
                    holder.tv_message_right.setVisibility(View.GONE);
                }
            } else {
                holder.cardview_right.setVisibility(View.GONE);
                holder.tv_message_right.setVisibility(View.VISIBLE);
                holder.tv_message_right.setText(textMessage);
            }

            View.OnLongClickListener longClickListener = view -> {
                if (!positionIsShowingEmote.isEmpty() && position != positionIsShowingEmote.get(0)) {
                    notifyItemChanged(positionIsShowingEmote.get(0));
                }
                positionIsShowingEmote.clear();
                positionIsShowingEmote.add(position);
                holder.recyclerViewEmoteRight.setVisibility(View.VISIBLE);
                holder.recyclerViewEmoteLeft.setVisibility(View.GONE);
                EmoteAdapter emoteAdapter = new EmoteAdapter(context, emoteList, emote -> onItemClick.onMessageLongClick(model.getIdMessage(), myId, emote));
                holder.recyclerViewEmoteRight.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                holder.recyclerViewEmoteRight.setAdapter(emoteAdapter);
                return true;
            };

            holder.tv_message_right.setOnLongClickListener(longClickListener);
            holder.cardview_right.setOnLongClickListener(longClickListener);

        } else {
            // Tin nhắn của đối phương (Bên trái)
            holder.avt_in_chat.setVisibility(View.VISIBLE);
            if (avtSender != null) {
                Glide.with(context)
                        .load(avtSender.get(model.getSenderId()))
                        .error(R.drawable.icon_avatar)
                        .into(holder.avt_in_chat);
            }
            holder.tv_alias.setVisibility(View.VISIBLE);
            if (aliasSender != null) {
                String alias = aliasSender.get(model.getSenderId());
                if (alias != null) {
                    if (alias.contains("_")) {
                        holder.tv_alias.setText(alias.split("_")[1]);
                    } else {
                        holder.tv_alias.setText(alias);
                    }
                }
            }

            holder.tv_emote_right.setVisibility(View.GONE);
            holder.tv_message_right.setVisibility(View.GONE);
            holder.cardview_right.setVisibility(View.GONE);

            bindEmote(holder.tv_emote_left, model.getEmote(), model.getIdMessage());

            if (hasMedia) {
                holder.cardview_left.setVisibility(View.VISIBLE);
                renderMedia(holder.img_picture_left, mediaUrl, messageType);
                final String finalMediaUrl = mediaUrl;
                holder.img_picture_left.setOnClickListener(view -> {
                    onItemClick.onImageClick(finalMediaUrl);
                    Log.e(TAG, "onBindViewHolder: " + finalMediaUrl);
                });
                if (hasCaption && !textMessage.isEmpty()) {
                    holder.tv_message_left.setVisibility(View.VISIBLE);
                    holder.tv_message_left.setText(textMessage);
                } else {
                    holder.tv_message_left.setVisibility(View.GONE);
                }
            } else {
                holder.cardview_left.setVisibility(View.GONE);
                holder.tv_message_left.setVisibility(View.VISIBLE);
                holder.tv_message_left.setText(textMessage);
            }

            View.OnLongClickListener longClickListener = view -> {
                if (!positionIsShowingEmote.isEmpty() && position != positionIsShowingEmote.get(0)) {
                    notifyItemChanged(positionIsShowingEmote.get(0));
                }
                positionIsShowingEmote.clear();
                positionIsShowingEmote.add(position);
                holder.recyclerViewEmoteLeft.setVisibility(View.VISIBLE);
                holder.recyclerViewEmoteRight.setVisibility(View.GONE);
                EmoteAdapter emoteAdapter = new EmoteAdapter(context, emoteList, emote -> onItemClick.onMessageLongClick(model.getIdMessage(), myId, emote));
                holder.recyclerViewEmoteLeft.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
                holder.recyclerViewEmoteLeft.setAdapter(emoteAdapter);
                return true;
            };

            holder.tv_message_left.setOnLongClickListener(longClickListener);
            holder.cardview_left.setOnLongClickListener(longClickListener);
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
