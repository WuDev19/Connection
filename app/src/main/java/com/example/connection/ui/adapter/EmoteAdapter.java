package com.example.connection.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connection.R;

import java.util.List;

public class EmoteAdapter extends RecyclerView.Adapter<EmoteAdapter.EmoteViewHolder> {

    private final Context context;
    private final List<String> listEmote;
    private final OnEmoteClick onEmoteClick;

    public EmoteAdapter(Context context, List<String> listEmote, OnEmoteClick onEmoteClick) {
        this.context = context;
        this.listEmote = listEmote;
        this.onEmoteClick = onEmoteClick;
    }

    public interface OnEmoteClick {
        void onClick(String emote);
    }

    @NonNull
    @Override
    public EmoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_layout_emote_item, parent, false);
        return new EmoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmoteViewHolder holder, int position) {
        String emote = listEmote.get(position);
        if (emote != null) {
            holder.tv_emote.setText(emote);
            holder.tv_emote.setAlpha(0f);
            holder.tv_emote.setTranslationX(-50f);
            holder.tv_emote.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setStartDelay(position * 70L)
                    .setDuration(200)
                    .start();
            holder.tv_emote.setOnClickListener(view -> onEmoteClick.onClick(emote));
        }
    }

    @Override
    public int getItemCount() {
        if (!listEmote.isEmpty()) return listEmote.size();
        return 0;
    }

    public static class EmoteViewHolder extends RecyclerView.ViewHolder {

        private final TextView tv_emote;

        public EmoteViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_emote = itemView.findViewById(R.id.tv_emote_item);
        }
        
    }

}
