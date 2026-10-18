package com.pmr.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChanAdapter extends RecyclerView.Adapter<ChanAdapter.VH> {
    private final Context ctx;
    private final List<ChanListActivity.User> users;
    private final OnBanClickListener listener;

    public interface OnBanClickListener {
        void onBanClick(ChanListActivity.User user, int position);
    }

    public ChanAdapter(Context ctx, List<ChanListActivity.User> users, OnBanClickListener listener) {
        this.ctx = ctx;
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chan, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChanListActivity.User user = users.get(position);
        h.itemText.setText(String.format("%05d %s", user.id, user.call));
        
        if (user.isBanned) {
            h.itemBtnBan.setBackgroundTintList(ContextCompat.getColorStateList(ctx, android.R.color.holo_red_dark));
        } else {
            h.itemBtnBan.setBackgroundTintList(ContextCompat.getColorStateList(ctx, android.R.color.holo_green_dark));
        }
        
        h.itemBtnBan.setOnClickListener(v -> listener.onBanClick(user, position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView itemText;
        Button itemBtnBan;
        VH(@NonNull View v) {
            super(v);
            itemText = v.findViewById(R.id.itemText);
            itemBtnBan = v.findViewById(R.id.itemBtnBan);
        }
    }
}