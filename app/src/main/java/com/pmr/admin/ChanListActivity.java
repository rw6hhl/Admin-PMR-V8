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
        public boolean isBanned;
        public User(int id, String call, boolean isBanned) { 
            this.id = id; this.call = call; this.isBanned = isBanned; 
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
            // Отправляем бан/разбан
            if (PmrService.getPmrSocket() != null) {
                if (!user.isBanned) {
                    PmrService.getPmrSocket().sendBan(user.id);
                    user.isBanned = true;
                    Toast.makeText(this, "Бан: " + user.id, Toast.LENGTH_SHORT).show();
                } else {
                    // Разбан (команда 221 с ID)
                    PmrService.getPmrSocket().sendUnban(user.id);
                    user.isBanned = false;
                    Toast.makeText(this, "Разбан: " + user.id, Toast.LENGTH_SHORT).show();
                }
                adapter.notifyItemChanged(position);
            }
        });
        recyclerView.setAdapter(adapter);

        // Подписываемся на обновления списка
        PmrService.setChanListListener(this);
        
        // Запрашиваем список
        if (PmrService.getPmrSocket() != null) {
            PmrService.getPmrSocket().requestChanList();
        }
    }

    @Override
    public void onChanListReceived(List<Integer> ids) {
        runOnUiThread(() -> {
            users.clear();
            // Заглушка для позывных (пока не парсим list.txt внутри приложения)
            for (int id : ids) {
                users.add(new User(id, "Абонент", false));
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PmrService.setChanListListener(null);
    }
}