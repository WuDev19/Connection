package com.example.connection.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.BackGround;
import java.util.List;

public class BackGroundChatAdapter extends RecyclerView.Adapter<BackGroundChatAdapter.BackGroundChatViewHolder> {

    private final Context context;
    private final List<BackGround> list;
    private final OnItemClick onItemClick;

    public BackGroundChatAdapter(Context context, List<BackGround> list, OnItemClick onItemClick) {
        this.context = context;
        this.list = list;
        this.onItemClick = onItemClick;
    }

    public interface OnItemClick{
        void onClick(String url);
    }

    @NonNull
    @Override
    public BackGroundChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_background_chat_item, parent, false);
        return new BackGroundChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BackGroundChatViewHolder holder, int position) {
        BackGround model = list.get(position);
        if(model != null){
            Glide.with(context)
                    .load(model.getUrlBackGround())
                    .error(R.drawable.background_3)
                    .into(holder.img);
            int width = context.getResources().getDisplayMetrics().widthPixels;
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) holder.cardView.getLayoutParams();
            layoutParams.width = (int) (0.31 * width);
            holder.cardView.setLayoutParams(layoutParams);
            holder.img.setOnClickListener(view -> onItemClick.onClick(model.getUrlBackGround()));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class BackGroundChatViewHolder extends RecyclerView.ViewHolder {

        private final ImageView img;
        private final CardView cardView;

        public BackGroundChatViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_back_ground_chat);
            cardView = itemView.findViewById(R.id.cardview_background);
        }
    }

}
