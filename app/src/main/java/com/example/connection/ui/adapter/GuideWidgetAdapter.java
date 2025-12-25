package com.example.connection.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connection.R;
import com.example.connection.model.Guide;
import java.util.List;

public class GuideWidgetAdapter extends RecyclerView.Adapter<GuideWidgetAdapter.GuideWidgetViewHolder> {

    private final Context context;
    private final List<Guide> list;

    public GuideWidgetAdapter(Context context, List<Guide> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public GuideWidgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_guide_item, parent, false);
        return new GuideWidgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GuideWidgetViewHolder holder, int position) {
        holder.img_guide.setImageResource(list.get(position).getRes());
        holder.tv_guide.setText(list.get(position).getGuide());
    }

    @Override
    public int getItemCount() {
        if(list.isEmpty()) return 0;
        return list.size();
    }

    public static class GuideWidgetViewHolder extends RecyclerView.ViewHolder {

        private final ImageView img_guide;
        private final TextView tv_guide;

        public GuideWidgetViewHolder(@NonNull View itemView) {
            super(itemView);
            img_guide = itemView.findViewById(R.id.img_guide);
            tv_guide = itemView.findViewById(R.id.tv_guide);
        }
    }
}
