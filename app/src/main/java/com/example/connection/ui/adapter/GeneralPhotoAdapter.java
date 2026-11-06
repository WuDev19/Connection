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

import java.util.ArrayList;
import java.util.List;

public class GeneralPhotoAdapter extends RecyclerView.Adapter<GeneralPhotoAdapter.GeneralPhotoViewHolder> {

    private final List<Image> postList = new ArrayList<>();
    private final Context context;
    private final onItemClick click;

    public interface onItemClick {
        void itemClick(int position);
    }

    public GeneralPhotoAdapter(Context context, onItemClick click) {
        this.context = context;
        this.click = click;
    }

    public Image getItem(int position) {
        if (position >= 0 && position < postList.size()) {
            return postList.get(position);
        }
        return null;
    }

    public boolean containsPostId(String postId) {
        if (postId == null) return false;
        for (Image image : postList) {
            if (postId.equals(image.getPostId())) {
                return true;
            }
        }
        return false;
    }

    public void setInitialPosts(List<Image> initialPosts) {
        if (initialPosts == null) return;
        postList.clear();
        postList.addAll(initialPosts);
        notifyDataSetChanged();
    }

    public void updateExistingPosts(List<Image> updatedPosts) {
        if (updatedPosts == null) return;
        for (Image updated : updatedPosts) {
            if (updated.getPostId() == null) continue;
            for (int i = 0; i < postList.size(); i++) {
                if (updated.getPostId().equals(postList.get(i).getPostId())) {
                    Image old = postList.get(i);
                    boolean hasChanges = !old.equals(updated)
                            || old.getNumberEmote() != updated.getNumberEmote()
                            || old.getNumberComment() != updated.getNumberComment();
                    if (hasChanges) {
                        postList.set(i, updated);
                        notifyItemChanged(i);
                    }
                    break;
                }
            }
        }
    }

    public int insertNewPostsAtTop(List<Image> newPosts) {
        if (newPosts == null || newPosts.isEmpty()) return 0;
        List<Image> toAdd = new ArrayList<>();
        for (Image post : newPosts) {
            if (!containsPostId(post.getPostId())) {
                toAdd.add(post);
            }
        }
        if (!toAdd.isEmpty()) {
            postList.addAll(0, toAdd);
            notifyItemRangeInserted(0, toAdd.size());
            return toAdd.size();
        }
        return 0;
    }

    public void addOlderPosts(List<Image> olderPosts) {
        if (olderPosts == null || olderPosts.isEmpty()) return;
        List<Image> toAdd = new ArrayList<>();
        for (Image post : olderPosts) {
            if (!containsPostId(post.getPostId())) {
                toAdd.add(post);
            }
        }
        if (!toAdd.isEmpty()) {
            int start = postList.size();
            postList.addAll(toAdd);
            notifyItemRangeInserted(start, toAdd.size());
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    @NonNull
    @Override
    public GeneralPhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_general, parent, false);
        return new GeneralPhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GeneralPhotoViewHolder holder, int position) {
        Image model = getItem(position);
        if (model == null) return;

        if (model.getVideoUrl() != null && !model.getVideoUrl().isEmpty()) {
            holder.img_video_badge.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(Uri.parse(model.getVideoUrl()))
                    .frame(500000)
                    .centerCrop()
                    .into(holder.img_post_general);
        } else if (model.getImageUrl() != null && !model.getImageUrl().isEmpty()) {
            holder.img_video_badge.setVisibility(View.GONE);
            Glide.with(context)
                    .load(model.getImageUrl())
                    .centerCrop()
                    .into(holder.img_post_general);
        } else {
            holder.img_video_badge.setVisibility(View.GONE);
            holder.img_post_general.setImageDrawable(null);
        }

        holder.itemView.setOnClickListener(view -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && click != null) {
                click.itemClick(pos);
            }
        });
    }

    public static class GeneralPhotoViewHolder extends RecyclerView.ViewHolder {

        ImageView img_post_general;
        ImageView img_video_badge;

        public GeneralPhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            img_post_general = itemView.findViewById(R.id.img_post_general);
            img_video_badge = itemView.findViewById(R.id.img_video_badge);
        }
    }
}
