package com.pmr.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chan_list);

        RecyclerView recyclerView = findViewById(R.id.recyclerChanList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Тестовые данные для отображения (в будущем будут браться из PmrSocket)
        users.add(new User("26001", "RW6HHL", 1));
        users.add(new User("26002", "R1ABC", 0));
        users.add(new User("26003", "R2XYZ", 2));

        adapter = new ChanAdapter(users, (user, position) -> {
            if (user.status != 2) {
                user.status = 2; // Бан
                // Здесь будет отправка cmd=222 через PmrSocket
                adapter.notifyItemChanged(position);
            }
        });
        recyclerView.setAdapter(adapter);
    }
}