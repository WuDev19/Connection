package com.example.connection.adapter;

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
import com.example.connection.model.Comment;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import de.hdodenhof.circleimageview.CircleImageView;

public class CommentPostAdapter extends FirestoreRecyclerAdapter<Comment, CommentPostAdapter.CommentPostViewHolder> {

    private final Context context;
    private final SetOnItemClick setOnItemClick;

    public CommentPostAdapter(@NonNull FirestoreRecyclerOptions<Comment> options, Context context, SetOnItemClick setOnItemClick) {
        super(options);
        this.context = context;
        this.setOnItemClick = setOnItemClick;
    }

    public interface SetOnItemClick{
        void onDelete(String commentId);
    }

    @Override
    protected void onBindViewHolder(@NonNull CommentPostViewHolder holder, int position, @NonNull Comment model) {
        Glide.with(context).load(model.getAvtComment()).error(R.drawable.icon_avatar).into(holder.img_avt);
        holder.tv_username.setText(model.getUserNameComment());
        holder.tv_comment.setText(model.getComment());
        holder.relativeLayout.setOnLongClickListener(view -> {
            setOnItemClick.onDelete(model.getIdComment());
            return true;
        });
    }

    @NonNull
    @Override
    public CommentPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_comment_item, parent, false);
        return new CommentPostViewHolder(view);
    }

    public static class CommentPostViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView img_avt;
        private final TextView tv_username;
        private final TextView tv_comment;
        private final RelativeLayout relativeLayout;

        public CommentPostViewHolder(@NonNull View itemView) {
            super(itemView);
            img_avt = itemView.findViewById(R.id.img_avt_comment);
            tv_username = itemView.findViewById(R.id.tv_username_comment);
            tv_comment = itemView.findViewById(R.id.tv_comment_item);
            relativeLayout = itemView.findViewById(R.id.relative_comment);
        }
    }

}
