package com.example.connection.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connection.R;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class AnonymousRandomAdapter extends RecyclerView.Adapter<AnonymousRandomAdapter.AnonymousRandomViewHolder> {

    private final Context context;
    private final List<Integer> list;

    public AnonymousRandomAdapter(Context context, List<Integer> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public AnonymousRandomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_anonymous_avt_item, parent, false);
        return new AnonymousRandomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnonymousRandomViewHolder holder, int position) {
        holder.circleImageView.setImageResource(list.get(position));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class AnonymousRandomViewHolder extends RecyclerView.ViewHolder {

        private final CircleImageView circleImageView;

        public AnonymousRandomViewHolder(@NonNull View itemView) {
            super(itemView);
            circleImageView = itemView.findViewById(R.id.img_random_avt);
        }
    }

}
