package com.example.connection.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import com.example.connection.model.EmoteModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import de.hdodenhof.circleimageview.CircleImageView;

public class EmotePostAdapter extends FirestoreRecyclerAdapter<EmoteModel, EmotePostAdapter.EmotePostViewHolder> {

    private final Context context;

    public EmotePostAdapter(@NonNull FirestoreRecyclerOptions<EmoteModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull EmotePostViewHolder holder, int position, @NonNull EmoteModel model) {
        holder.tv_username.setText(model.getUsername());
        holder.tv_emote.setText(model.getEmote());
        Glide.with(context)
                .load(model.getUrlAvt())
                .error(R.drawable.icon_avatar)
                .into(holder.img_avt);
    }

    @NonNull
    @Override
    public EmotePostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_emote_post_item, parent, false);
        return new EmotePostViewHolder(view);
    }

    public static class EmotePostViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView img_avt;
        private final TextView tv_username;
        private final TextView tv_emote;

        public EmotePostViewHolder(@NonNull View itemView) {
            super(itemView);
            img_avt = itemView.findViewById(R.id.img_avt_emote_post);
            tv_emote = itemView.findViewById(R.id.tv_emote_post);
            tv_username = itemView.findViewById(R.id.tv_username_emote_post);
        }
    }

}
