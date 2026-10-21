package com.pmr.admin;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChanListActivity extends AppCompatActivity implements PmrSocket.OnChanListListener, PmrSocket.OnVoiceListener {

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
    private Map<Integer, Boolean> localBanState = new HashMap<>();
    private ChanAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.add("ChanListActivity: onCreate — старт");
        setContentView(R.layout.activity_chan_list);
        AppLog.add("ChanListActivity: setContentView OK");

        RecyclerView recyclerView = findViewById(R.id.recyclerChanList);
        if (recyclerView == null) {
            AppLog.add("ChanListActivity: ОШИБКА — recyclerChanList не найден");
            finish();
            return;
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChanAdapter(this, users, (user, position) -> {
            AppLog.add("ChanListActivity: кнопка БАН/РАЗБАН — id=" + user.id + ", isBanned=" + user.isBanned);
            if (PmrService.getPmrSocket() != null) {
                new Thread(() -> {
                    if (!user.isBanned) {
                        PmrService.getPmrSocket().sendBan(user.id);
                    } else {
                        PmrService.getPmrSocket().sendUnban(user.id);
                    }
                }).start();
                
                user.isBanned = !user.isBanned;
                localBanState.put(user.id, user.isBanned);
                Toast.makeText(this, (user.isBanned ? "Бан: " : "Разбан: ") + user.id, Toast.LENGTH_SHORT).show();
                adapter.notifyItemChanged(position);
            } else {
                Toast.makeText(this, "Сервис не запущен", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setAdapter(adapter);
        AppLog.add("ChanListActivity: adapter установлен");

        PmrService.setChanListListener(this);
        PmrService.setVoiceListener(this);
        AppLog.add("ChanListActivity: setChanListListener + setVoiceListener");

        if (PmrService.getPmrSocket() != null) {
            new Thread(() -> {
                try {
                    PmrService.getPmrSocket().requestChanList();
                } catch (Exception e) {
                    AppLog.add("ChanListActivity: ошибка requestChanList в фоне — " + e.getMessage());
                }
            }).start();
        } else {
            Toast.makeText(this, "Сервис еще не запущен", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onChanListReceived(List<PmrSocket.ChanEntry> entries) {
        AppLog.add("ChanListActivity: onChanListReceived — записей=" + entries.size());
        runOnUiThread(() -> {
            if (adapter == null) return;
            users.clear();
            for (PmrSocket.ChanEntry e : entries) {
                String name = NameBook.getName(this, e.id);
                boolean isBanned = localBanState.containsKey(e.id) ? localBanState.get(e.id) : e.ban;
                users.add(new User(e.id, name, e.ip, isBanned));
            }
            adapter.notifyDataSetChanged();
            AppLog.add("ChanListActivity: Список обновлён, абонентов: " + users.size());
        });
    }

    @Override
    public void onVoiceReceived(int clientId) {
        runOnUiThread(() -> {
            if (adapter != null) {
                adapter.setActiveClient(clientId);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppLog.add("ChanListActivity: onDestroy — setChanListListener(null)");
        PmrService.setChanListListener(null);
        PmrService.setVoiceListener(null);
    }
}