package com.example.connection.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.data.local.MyMusic;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicYoutubeViewHolder> {

    private final Context context;
    private List<MyMusic> list;
    private final OnItemClick onClick;
    private boolean isPlaying = false;

    public interface OnItemClick{
        void onNameItemClick(String musicName, String singerName, String url, int position);
        void onPlayMusic(String url, int position, boolean isPlaying, ImageView imgPlay);
    }

    public MusicAdapter(Context context, List<MyMusic> list, OnItemClick onClick) {
        this.context = context;
        this.list = list;
        this.onClick = onClick;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<MyMusic> newList){
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MusicYoutubeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_music_item, parent, false);
        return new MusicYoutubeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicYoutubeViewHolder holder, int position) {
        MyMusic model = list.get(position);
        if(model == null) return;
        Glide.with(context).load(model.getImage()).error(R.drawable.icon_avatar).into(holder.img_music_item);
        holder.tv_music_name_item.setText(model.getMusicName());
        holder.tv_name_singer.setText(model.getSinger());
        holder.img_play.setOnClickListener(view -> onClick.onPlayMusic(model.getUrl(), position, isPlaying, holder.img_play));
        holder.relativeLayout.setOnClickListener(view -> onClick.onNameItemClick(model.getMusicName(), model.getSinger(), model.getUrl(), position));
        holder.img_play.setImageResource(R.drawable.icon_play);
    }

    @Override
    public int getItemCount() {
        if(!list.isEmpty()) return list.size();
        return 0;
    }

    public static class MusicYoutubeViewHolder extends RecyclerView.ViewHolder{

        ImageView img_music_item, img_play;
        TextView tv_music_name_item, tv_name_singer;
        RelativeLayout relativeLayout;

        public MusicYoutubeViewHolder(@NonNull View itemView) {
            super(itemView);
            img_music_item = itemView.findViewById(R.id.img_music_item);
            img_play = itemView.findViewById(R.id.img_play);
            tv_music_name_item = itemView.findViewById(R.id.tv_music_name_item);
            tv_name_singer = itemView.findViewById(R.id.tv_name_singer);
            relativeLayout = itemView.findViewById(R.id.relative_music);
        }
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

}
