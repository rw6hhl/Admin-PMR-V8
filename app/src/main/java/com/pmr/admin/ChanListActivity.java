package com.pmr.admin;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChanListActivity extends AppCompatActivity implements PmrSocket.OnChanListListener {

    public static class User {
        public int id;
        public String call;
        public String ip;
        public boolean isBanned;
        public User(int id, String call, String ip, boolean isBanned) {
            this.id = id;
            this.call = call;
            this.ip = ip;
            this.isBanned = isBanned;
        }
    }

    private List<User> users = new ArrayList<>();
    private ChanAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chan_list);

        RecyclerView recyclerView = findViewById(R.id.recyclerChanList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChanAdapter(this, users, (user, position) -> {
            if (PmrService.getPmrSocket() != null) {
                if (!user.isBanned) {
                    PmrService.getPmrSocket().sendBan(user.id);
                    user.isBanned = true;
                    Toast.makeText(this, "Бан: " + user.id, Toast.LENGTH_SHORT).show();
                } else {
                    PmrService.getPmrSocket().sendUnban(user.id);
                    user.isBanned = false;
                    Toast.makeText(this, "Разбан: " + user.id, Toast.LENGTH_SHORT).show();
                }
                adapter.notifyItemChanged(position);
            } else {
                Toast.makeText(this, "Сервис не запущен", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);

        PmrService.setChanListListener(this);

        if (PmrService.getPmrSocket() != null) {
            PmrService.getPmrSocket().requestChanList();
        } else {
            Toast.makeText(this, "Сервис еще не запущен", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onChanListReceived(List<PmrSocket.ChanEntry> entries) {
        runOnUiThread(() -> {
            if (adapter == null) return;
            users.clear();
            for (PmrSocket.ChanEntry e : entries) {
                String name = NameBook.getName(this, e.id);
                users.add(new User(e.id, name, e.ip, e.ban));
            }
            adapter.notifyDataSetChanged();
            AppLog.add("Список обновлен, абонентов: " + users.size());
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PmrService.setChanListListener(null);
    }
}