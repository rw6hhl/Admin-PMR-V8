package com.pmr.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChanAdapter extends RecyclerView.Adapter<ChanAdapter.ViewHolder> {
    private final List<ChanListActivity.User> users;
    private final OnBanClickListener listener;

    public interface OnBanClickListener {
        void onBanClick(ChanListActivity.User user, int position);
    }

    public ChanAdapter(List<ChanListActivity.User> users, OnBanClickListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChanListActivity.User user = users.get(position);
        holder.tvId.setText(user.id);
        holder.tvCall.setText(user.call);

        // Установка цвета для динамика
        int colorRes = android.R.color.holo_orange_light; // Желтый (в сети)
        if (user.status == 1) colorRes = android.R.color.holo_green_light; // Зеленый (говорит)
        else if (user.status == 2) colorRes = android.R.color.holo_red_light; // Красный (бан)

        holder.imgSpeaker.setColorFilter(holder.itemView.getContext().getResources().getColor(colorRes));
        holder.imgSpeaker.setOnClickListener(v -> listener.onBanClick(user, position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvCall;
        ImageView imgSpeaker;

        ViewHolder(View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tvChanId);
            tvCall = itemView.findViewById(R.id.tvChanCall);
            imgSpeaker = itemView.findViewById(R.id.imgSpeaker);
        }
    }
}