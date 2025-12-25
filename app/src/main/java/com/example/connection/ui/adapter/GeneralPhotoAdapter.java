package com.example.connection.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.Image;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

public class GeneralPhotoAdapter extends FirestoreRecyclerAdapter<Image, GeneralPhotoAdapter.GeneralPhotoViewHolder> {

    private final Context context;
    public onItemClick click;

    public interface onItemClick{
        void itemClick(int position);
    }

    public GeneralPhotoAdapter(@NonNull FirestoreRecyclerOptions<Image> options, Context context, onItemClick click) {
        super(options);
        this.context = context;
        this.click = click;
    }

    @Override
    protected void onBindViewHolder(@NonNull GeneralPhotoViewHolder holder, int position, @NonNull Image model) {
        if(model.getImageUrl() != null){
            Glide.with(context).load(model.getImageUrl()).error(R.drawable.icon_avatar).into(holder.img_post_general);
        } else if (model.getVideoUrl() != null) {
            Glide.with(context).load(Uri.parse(model.getVideoUrl())).frame(500000).into(holder.img_post_general);
        }
        holder.img_post_general.setOnClickListener(view -> {
            click.itemClick(position);
        });
    }

    @NonNull
    @Override
    public GeneralPhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_general, parent, false);
        return new GeneralPhotoViewHolder(view);
    }

    public static class GeneralPhotoViewHolder extends RecyclerView.ViewHolder {

        ImageView img_post_general;

        public GeneralPhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            img_post_general = itemView.findViewById(R.id.img_post_general);
        }
    }

}
