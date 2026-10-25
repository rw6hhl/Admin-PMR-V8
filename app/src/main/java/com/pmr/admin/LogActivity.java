package com.pmr.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;

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
        Button btnSendLog = findViewById(R.id.btnSendLog);
        Button btnClear = findViewById(R.id.btnClearLogs);

        tvLogs.setText(AppLog.getAllLogs());
        AppLog.setListener(this);

        btnEmail.setOnClickListener(v -> {
            String logs = AppLog.getAllLogs();
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rw6hhl@mail.ru"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Логи Admin PMR V9.39");
            emailIntent.putExtra(Intent.EXTRA_TEXT, logs);
            startActivity(Intent.createChooser(emailIntent, "Отправить лог через..."));
        });

        btnSendLog.setOnClickListener(v -> {
            File f = AppLog.getCurrentLogFile();
            if (f == null || !f.exists()) {
                Toast.makeText(this, "Файл лога ещё не создан", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Uri uri = FileProvider.getUriForFile(this,
                        "com.pmr.admin.fileprovider", f);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.putExtra(Intent.EXTRA_SUBJECT, "Log Admin PMR " + f.getName());
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(send, "Отправить файл лога..."));
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                AppLog.add("LogActivity: ошибка FileProvider — " + e.getMessage());
            }
        });

        btnClear.setOnClickListener(v -> {
            AppLog.clear();
            tvLogs.setText("");
        });
    }

    @Override
    public void onLogUpdated(String newLog) {
        if (isFinishing() || isDestroyed()) return;
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            if (tvLogs == null || scrollView == null) return;
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