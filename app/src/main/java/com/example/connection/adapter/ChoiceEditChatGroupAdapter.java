package com.example.connection.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.connection.R;
import com.example.connection.model.ChoiceEditGroupModel;
import java.util.List;

public class ChoiceEditChatGroupAdapter extends RecyclerView.Adapter<ChoiceEditChatGroupAdapter.ChoiceEditChatGroupViewHolder> {

    private final Context context;
    private final List<ChoiceEditGroupModel> list;
    private final OnItemClick onItemClick;

    public ChoiceEditChatGroupAdapter(Context context, List<ChoiceEditGroupModel> list, OnItemClick onItemClick) {
        this.context = context;
        this.list = list;
        this.onItemClick = onItemClick;
    }

    public interface OnItemClick {
        void onItemClickListener(String tag);
    }

    @NonNull
    @Override
    public ChoiceEditChatGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.choice_chat_group_item, parent, false);
        return new ChoiceEditChatGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChoiceEditChatGroupViewHolder holder, int position) {
        ChoiceEditGroupModel model = list.get(position);
        if (model == null) return;
        holder.imageView.setImageResource(model.getRes());
        holder.textView.setText(model.getNameChoice());
        holder.textView.setOnClickListener(view -> onItemClick.onItemClickListener(model.getTag()));
    }

    @Override
    public int getItemCount() {
        if (!list.isEmpty()) return list.size();
        return 0;
    }

    public static class ChoiceEditChatGroupViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final TextView textView;

        public ChoiceEditChatGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_choice_chat_group);
            textView = itemView.findViewById(R.id.tv_choice_chat_group);
        }
    }

}
