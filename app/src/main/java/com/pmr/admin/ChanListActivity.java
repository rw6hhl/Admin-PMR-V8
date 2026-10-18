package com.pmr.admin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChanListActivity extends AppCompatActivity {

    public static class User {
        public String id;
        public String call;
        public int status; // 0 = в сети (желтый), 1 = говорит (зеленый), 2 = забанен (красный)
        public User(String id, String call, int status) { this.id = id; this.call = call; this.status = status; }
    }

    private final List<User> users = new ArrayList<>();
    private ChanAdapter adapter;
    private Button btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chan_list);

        RecyclerView recyclerView = findViewById(R.id.recyclerChanList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        btnRefresh = findViewById(R.id.btnRefreshList); // Добавим кнопку в layout позже

        // Тестовые данные (пока PmrSocket не парсит список)
        users.add(new User("26001", "RW6HHL", 1));
        users.add(new User("26002", "R1ABC", 0));
        users.add(new User("26003", "R2XYZ", 2));

        adapter = new ChanAdapter(users, (user, position) -> {
            if (user.status != 2) {
                user.status = 2; 
                // Здесь будет отправка cmd=222 через PmrSocket
                adapter.notifyItemChanged(position);
            }
        });
        recyclerView.setAdapter(adapter);
    }
}