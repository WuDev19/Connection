package com.example.connection.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.connection.R;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChangeAliasAdapter extends RecyclerView.Adapter<ChangeAliasAdapter.ChangeAliasViewHolder> {

    private final Context context;
    private final List<String> listId;
    private final OnItemClick onItemClick;
    private final Map<String, String> avtSender;
    private final Map<String, String> aliasSender;

    public ChangeAliasAdapter(Context context, List<String> listId, Map<String, String> avtSender, Map<String, String> aliasSender, OnItemClick onItemClick) {
        this.context = context;
        this.listId = listId;
        this.onItemClick = onItemClick;
        this.avtSender = avtSender;
        this.aliasSender = aliasSender;
    }

    public interface OnItemClick{
        void onClick(TextView text_alias, String id);
    }

    @NonNull
    @Override
    public ChangeAliasViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_set_alias_item, parent, false);
        return new ChangeAliasViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChangeAliasViewHolder holder, int position) {
        String id = listId.get(position);
        if(id == null) return;
        if(Objects.requireNonNull(aliasSender.get(id)).contains("_")){
            holder.tv_alias.setText(Objects.requireNonNull(aliasSender.get(id)).split("_")[1]);
        }
        else holder.tv_alias.setText(aliasSender.get(id));
        Glide.with(context).load(avtSender.get(id)).error(R.drawable.icon_avatar).into(holder.img_avt);
        holder.linear_alias.setOnClickListener(view -> onItemClick.onClick(holder.tv_alias, id));
    }

    @Override
    public int getItemCount() {
        if(!listId.isEmpty()) return listId.size();
        return 0;
    }

    public static class ChangeAliasViewHolder extends RecyclerView.ViewHolder {

        private final LinearLayout linear_alias;
        private final TextView tv_alias;
        private final CircleImageView img_avt;

        public ChangeAliasViewHolder(@NonNull View itemView) {
            super(itemView);
            linear_alias = itemView.findViewById(R.id.linear_alias);
            tv_alias = itemView.findViewById(R.id.tv_name_alias);
            img_avt = itemView.findViewById(R.id.img_avt_alias);
        }

    }

}
