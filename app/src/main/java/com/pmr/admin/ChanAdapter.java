package com.pmr.admin;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
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
    private static final int MAX_NAME_LEN = 16;
    private final Context ctx;
    private final List<ChanListActivity.User> users;
    private final OnBanClickListener listener;
    private int activeClient = -1;
    
    private final Handler resetHandler = new Handler(Looper.getMainLooper());
    private final Runnable resetRunnable;

    public interface OnBanClickListener {
        void onBanClick(ChanListActivity.User user, int position);
    }

    public ChanAdapter(Context ctx, List<ChanListActivity.User> users, OnBanClickListener listener) {
        this.ctx = ctx;
        this.users = users;
        this.listener = listener;
        
        // Инициализация Runnable внутри конструктора, чтобы users уже был присвоен
        this.resetRunnable = () -> {
            if (this.activeClient != -1) {
                int old = this.activeClient;
                this.activeClient = -1;
                for (int i = 0; i < this.users.size(); i++) {
                    if (this.users.get(i).id == old) {
                        notifyItemChanged(i);
                        break;
                    }
                }
            }
        };
    }

    public void setActiveClient(int clientId) {
        if (this.activeClient != clientId) {
            this.activeClient = clientId;
            notifyDataSetChanged();
        }
        resetHandler.removeCallbacks(resetRunnable);
        if (clientId != -1) {
            resetHandler.postDelayed(resetRunnable, 3000); // сброс через 3 секунды
        }
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
        String name = user.call == null ? "" : user.call.trim();
        if (name.length() > MAX_NAME_LEN) {
            name = name.substring(0, MAX_NAME_LEN - 1) + "…";
        }
        // Отображаем номер на сервере (number) и признак (id)
        h.itemText.setText(String.format("№%d ID%05d %s", user.number, user.id, name));
        
        if (user.id == activeClient) {
            h.itemText.setTextColor(Color.RED);
            h.itemText.setTypeface(null, Typeface.BOLD);
        } else {
            h.itemText.setTextColor(Color.WHITE);
            h.itemText.setTypeface(null, Typeface.NORMAL);
        }

        if (user.isBanned) {
            h.itemBtnBan.setText("РАЗБАН");
            h.itemBtnBan.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.holo_red_dark));
        } else {
            h.itemBtnBan.setText("БАН");
            h.itemBtnBan.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.holo_green_dark));
        }
        h.itemBtnBan.setEnabled(true);
        h.itemBtnBan.setOnClickListener(v -> {
            if (listener != null) listener.onBanClick(user, position);
        });
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