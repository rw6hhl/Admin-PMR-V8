package com.pmr.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LogActivity extends AppCompatActivity implements AppLog.LogUpdateListener {

    private TextView tvLogs;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        tvLogs = findViewById(R.id.tvLogs);
        scrollView = findViewById(R.id.scrollView);
        Button btnEmail = findViewById(R.id.btnSendEmail);
        Button btnClear = findViewById(R.id.btnClearLogs);

        tvLogs.setText(AppLog.getAllLogs());
        AppLog.setListener(this);

        btnEmail.setOnClickListener(v -> {
            String logs = AppLog.getAllLogs();
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rw6hhl@mail.ru"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Логи Admin PMR V8");
            emailIntent.putExtra(Intent.EXTRA_TEXT, logs);
            startActivity(Intent.createChooser(emailIntent, "Отправить лог через..."));
        });

        btnClear.setOnClickListener(v -> {
            AppLog.clear();
            tvLogs.setText("");
        });
    }

    @Override
    public void onLogUpdated(String newLog) {
        runOnUiThread(() -> {
            tvLogs.append(newLog + "\n");
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppLog.setListener(null);
    }
}