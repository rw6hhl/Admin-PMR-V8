package com.pmr.admin;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_MIC = 101;
    private SharedPreferences prefs;
    private TextView tvChannel, tvIndex, tvSpeakingId, tvVersion;
    private ImageView imgRed;
    private Button btnMenu, btnUp, btnDown, btnSave1, btnSave2, btnSave3, btnSave4, btnSave5, btnCallOrList, btnPTT, btnSecretMode;
    private boolean isAdminMode = false;
    private PowerManager.WakeLock wakeLock;
    private int currentChannel = 1;

    private BroadcastReceiver voiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int clientId = intent.getIntExtra("clientId", -1);
            if (clientId != -1) {
                tvSpeakingId.setText(String.valueOf(clientId));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences("pmr_prefs", MODE_PRIVATE);
        
        String savedVersion = prefs.getString("app_version", "0");
        if (!savedVersion.equals("9.33")) {
            prefs.edit()
                .putString("app_version", "9.33")
                .putString("MyMailIndex", "51953")
                .putString("Priznak_pmr", "26000")
                .putInt("PORT_prm", 5254)
                .putInt("PORT_prd", 16000)
                .putString("ip_server", "185.221.154.39")
                .putInt("current_channel", 1)
                .putBoolean("simplex", true)
                .putBoolean("is_admin_mode", false)
                .apply();
            AppLog.add("Настройки сброшены (V9.33)");
        }

        requestMicPermission();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PMR::WakeLock");
            wakeLock.acquire();
        }

        currentChannel = prefs.getInt("current_channel", 1);
        isAdminMode = prefs.getBoolean("is_admin_mode", false);

        initViews();
        setupListeners();
        updateUI();
        
        new Handler().postDelayed(() -> {
            Intent serviceIntent = new Intent(this, PmrService.class);
            startService(serviceIntent);
        }, 500);
    }

    private void requestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AppLog.add("Разрешение на микрофон получено");
                // Сервис уже запущен в onCreate. Перезапуск не нужен.
                // AudioEngine.startRecording() создаёт AudioRecord в момент PTT,
                // когда разрешение уже выдано. Stop/start здесь давал гонку EADDRINUSE.
            } else {
                AppLog.add("ОШИБКА: Разрешение отклонено!");
            }
        }
    }

    private void initViews() {
        tvChannel = findViewById(R.id.textView);
        tvIndex = findViewById(R.id.textView4);
        tvSpeakingId = findViewById(R.id.tvSpeakingId);
        tvVersion = findViewById(R.id.tvVersion);
        imgRed = findViewById(R.id.imageView);

        btnMenu = findViewById(R.id.button4);
        btnUp = findViewById(R.id.button1);
        btnDown = findViewById(R.id.button2);
        btnCallOrList = findViewById(R.id.button);
        btnSave1 = findViewById(R.id.buttonsave1);
        btnSave2 = findViewById(R.id.buttonsave2);
        btnSave3 = findViewById(R.id.buttonsave3);
        btnSave4 = findViewById(R.id.buttonsave4);
        btnSave5 = findViewById(R.id.buttonsave5);
        btnPTT = findViewById(R.id.btnPTT);
        btnSecretMode = findViewById(R.id.btnSecretMode);
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        
        btnCallOrList.setOnClickListener(v -> {
            if (isAdminMode) {
                startActivity(new Intent(this, ChanListActivity.class));
            } else {
                Toast.makeText(this, "Вызов (CALL)", Toast.LENGTH_SHORT).show();
            }
        });

        btnSecretMode.setOnClickListener(v -> {
            isAdminMode = !isAdminMode;
            prefs.edit().putBoolean("is_admin_mode", isAdminMode).apply();
            Toast.makeText(this, isAdminMode ? "РЕЖИМ АДМИНА" : "РЕЖИМ КЛИЕНТА", Toast.LENGTH_SHORT).show();
            updateUI();
        });

        btnUp.setOnClickListener(v -> {
            if (currentChannel < 8) {
                currentChannel++;
                updateUI();
                prefs.edit().putInt("current_channel", currentChannel).apply();
            }
        });

        btnDown.setOnClickListener(v -> {
            if (currentChannel > 0) {
                currentChannel--;
                updateUI();
                prefs.edit().putInt("current_channel", currentChannel).apply();
            }
        });

        btnSave1.setOnClickListener(v -> loadPreset(1));
        btnSave2.setOnClickListener(v -> loadPreset(2));
        btnSave3.setOnClickListener(v -> loadPreset(3));
        btnSave4.setOnClickListener(v -> loadPreset(4));
        btnSave5.setOnClickListener(v -> loadPreset(5));

        btnSave1.setOnLongClickListener(v -> { savePreset(1); return true; });
        btnSave2.setOnLongClickListener(v -> { savePreset(2); return true; });
        btnSave3.setOnLongClickListener(v -> { savePreset(3); return true; });
        btnSave4.setOnLongClickListener(v -> { savePreset(4); return true; });
        btnSave5.setOnLongClickListener(v -> { savePreset(5); return true; });

        btnPTT.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    imgRed.setVisibility(View.VISIBLE);
                    sendBroadcast(new Intent("PMR_START_RECORDING"));
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    imgRed.setVisibility(View.INVISIBLE);
                    sendBroadcast(new Intent("PMR_STOP_RECORDING"));
                    return true;
            }
            return false;
        });
    }

    private void savePreset(int presetNum) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("preset_channel_" + presetNum, currentChannel);
        editor.putString("preset_index_" + presetNum, prefs.getString("MyMailIndex", ""));
        editor.putString("preset_priznak_" + presetNum, prefs.getString("Priznak_pmr", ""));
        editor.putString("preset_ip_" + presetNum, prefs.getString("ip_server", ""));
        editor.apply();
        
        String info = "Сохранено в ячейку " + presetNum + ":\n" +
                "Канал: " + currentChannel + "\n" +
                "Индекс: " + prefs.getString("MyMailIndex", "") + "\n" +
                "Признак: " + prefs.getString("Priznak_pmr", "") + "\n" +
                "IP: " + prefs.getString("ip_server", "");
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }

    private void loadPreset(int presetNum) {
        int channel = prefs.getInt("preset_channel_" + presetNum, currentChannel);
        String index = prefs.getString("preset_index_" + presetNum, prefs.getString("MyMailIndex", "51953"));
        String priznak = prefs.getString("preset_priznak_" + presetNum, prefs.getString("Priznak_pmr", "26000"));
        String ip = prefs.getString("preset_ip_" + presetNum, prefs.getString("ip_server", "185.221.154.39"));
        
        currentChannel = channel;
        
        prefs.edit()
            .putInt("current_channel", channel)
            .putString("MyMailIndex", index)
            .putString("Priznak_pmr", priznak)
            .putString("ip_server", ip)
            .apply();
        
        if (PmrService.pmrSocket != null) {
            PmrService.pmrSocket.recreateSocket(this);
        }
        
        updateUI();
        
        String info = "Загружено из ячейки " + presetNum + ":\n" +
                "Канал: " + channel + "\n" +
                "Индекс: " + index + "\n" +
                "Признак: " + priznak + "\n" +
                "IP: " + ip;
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }

    private void updateUI() {
        tvChannel.setText(currentChannel == 0 ? "R" : String.valueOf(currentChannel));
        tvIndex.setText(prefs.getString("MyMailIndex", "51953"));
        tvVersion.setText("v" + BuildConfig.VERSION_NAME);
        
        if (isAdminMode) {
            tvChannel.setTextColor(getResources().getColor(R.color.active));
            btnCallOrList.setText("LIST");
        } else {
            tvChannel.setTextColor(getResources().getColor(android.R.color.black));
            btnCallOrList.setText("CALL");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAdminMode = prefs.getBoolean("is_admin_mode", false);
        currentChannel = prefs.getInt("current_channel", 1);
        updateUI();
        registerReceiver(voiceReceiver, new IntentFilter("PMR_VOICE_RECEIVED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(voiceReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopService(new Intent(this, PmrService.class));
    }
}