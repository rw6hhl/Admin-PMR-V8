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
    private int currentChannel = 4;

    private BroadcastReceiver voiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int clientId = intent.getIntExtra("clientId", -1);
            if (clientId != -1) {
                AppLog.add("MainActivity: PMR_VOICE_RECEIVED clientId=" + clientId);
                tvSpeakingId.setText(String.valueOf(clientId));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppLog.init(getApplicationContext());

        prefs = getSharedPreferences("pmr_prefs", MODE_PRIVATE);
        AppLog.add("MainActivity: onCreate");

        String savedVersion = prefs.getString("app_version", "0");
        if (!savedVersion.equals("9.41")) {
            prefs.edit()
                .putString("app_version", "9.41")
                .putString("MyMailIndex", "51953")
                .putString("Priznak_pmr", "26000")
                .putInt("PORT_prm", 5253)
                .putInt("PORT_prd", 16000)
                .putString("ip_server", "185.221.154.39")
                .putInt("current_channel", 4)
                .putBoolean("simplex", true)
                .putBoolean("is_admin_mode", false)
                .apply();
            AppLog.add("MainActivity: Настройки сброшены (V9.41): Priznak=26000, Mail=51953, Ch=4, PORT_prm=5253, PORT_prd=16000");
        }

        requestMicPermission();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PMR::WakeLock");
            wakeLock.acquire();
        }

        currentChannel = prefs.getInt("current_channel", 4);
        isAdminMode = prefs.getBoolean("is_admin_mode", false);
        AppLog.add("MainActivity: currentChannel=" + currentChannel + ", isAdminMode=" + isAdminMode);

        initViews();
        setupListeners();
        updateUI();

        new Handler().postDelayed(() -> {
            AppLog.add("MainActivity: запуск PmrService");
            Intent serviceIntent = new Intent(this, PmrService.class);
            startService(serviceIntent);
        }, 500);
    }

    private void requestMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            AppLog.add("MainActivity: запрос разрешения на микрофон");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        } else {
            AppLog.add("MainActivity: разрешение на микрофон уже есть");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AppLog.add("MainActivity: Разрешение на микрофон получено");
            } else {
                AppLog.add("MainActivity: ОШИБКА — разрешение на микрофон отклонено!");
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
        AppLog.add("MainActivity: initViews — найдены кнопки и TextView");
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> {
            AppLog.add("MainActivity: нажата MENU → SettingsActivity");
            startActivity(new Intent(this, SettingsActivity.class));
        });

        btnCallOrList.setOnClickListener(v -> {
            AppLog.add("MainActivity: нажата CALL/LIST, isAdminMode=" + isAdminMode);
            if (isAdminMode) {
                AppLog.add("MainActivity: открываю ChanListActivity");
                startActivity(new Intent(this, ChanListActivity.class));
            } else {
                Toast.makeText(this, "Вызов (CALL)", Toast.LENGTH_SHORT).show();
            }
        });

        btnSecretMode.setOnClickListener(v -> {
            isAdminMode = !isAdminMode;
            prefs.edit().putBoolean("is_admin_mode", isAdminMode).apply();
            AppLog.add("MainActivity: переключение режима → isAdminMode=" + isAdminMode);
            Toast.makeText(this, isAdminMode ? "РЕЖИМ АДМИНА" : "РЕЖИМ КЛИЕНТА", Toast.LENGTH_SHORT).show();
            updateUI();
        });

        btnUp.setOnClickListener(v -> {
            if (currentChannel < 8) {
                int old = currentChannel;
                currentChannel++;
                updateUI();
                prefs.edit().putInt("current_channel", currentChannel).apply();
                AppLog.add("MainActivity: UP, канал " + old + " → " + currentChannel);
            }
        });

        btnDown.setOnClickListener(v -> {
            if (currentChannel > 0) {
                int old = currentChannel;
                currentChannel--;
                updateUI();
                prefs.edit().putInt("current_channel", currentChannel).apply();
                AppLog.add("MainActivity: DOWN, канал " + old + " → " + currentChannel);
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
                    AppLog.add("MainActivity: PTT ACTION_DOWN");
                    imgRed.setVisibility(View.VISIBLE);
                    sendBroadcast(new Intent("PMR_START_RECORDING"));
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    AppLog.add("MainActivity: PTT ACTION_UP/CANCEL");
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
        editor.putString("preset_index_" + presetNum, prefs.getString("MyMailIndex", "51953"));
        editor.putString("preset_priznak_" + presetNum, prefs.getString("Priznak_pmr", "26000"));
        editor.putString("preset_ip_" + presetNum, prefs.getString("ip_server", "185.221.154.39"));
        editor.putInt("preset_port_prm_" + presetNum, prefs.getInt("PORT_prm", 5253));
        editor.putInt("preset_port_prd_" + presetNum, prefs.getInt("PORT_prd", 16000));
        editor.apply();

        String info = "Сохранено в ячейку " + presetNum + ":\n" +
                "Канал: " + currentChannel + "\n" +
                "Индекс: " + prefs.getString("MyMailIndex", "") + "\n" +
                "Признак: " + prefs.getString("Priznak_pmr", "") + "\n" +
                "PORT_prm: " + prefs.getInt("PORT_prm", 5253) + "\n" +
                "PORT_prd: " + prefs.getInt("PORT_prd", 16000) + "\n" +
                "IP: " + prefs.getString("ip_server", "");
        AppLog.add("MainActivity: savePreset(" + presetNum + ") — channel=" + currentChannel
                + ", index=" + prefs.getString("MyMailIndex", "")
                + ", priznak=" + prefs.getString("Priznak_pmr", "")
                + ", PORT_prm=" + prefs.getInt("PORT_prm", 5253)
                + ", PORT_prd=" + prefs.getInt("PORT_prd", 16000));
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }

    private void loadPreset(int presetNum) {
        int channel = prefs.getInt("preset_channel_" + presetNum, currentChannel);
        String index = prefs.getString("preset_index_" + presetNum, prefs.getString("MyMailIndex", "51953"));
        String priznak = prefs.getString("preset_priznak_" + presetNum, prefs.getString("Priznak_pmr", "26000"));
        String ip = prefs.getString("preset_ip_" + presetNum, prefs.getString("ip_server", "185.221.154.39"));
        int prm = prefs.getInt("preset_port_prm_" + presetNum, prefs.getInt("PORT_prm", 5253));
        int prd = prefs.getInt("preset_port_prd_" + presetNum, prefs.getInt("PORT_prd", 16000));

        currentChannel = channel;

        prefs.edit()
            .putInt("current_channel", channel)
            .putString("MyMailIndex", index)
            .putString("Priznak_pmr", priznak)
            .putString("ip_server", ip)
            .putInt("PORT_prm", prm)
            .putInt("PORT_prd", prd)
            .apply();

        AppLog.add("MainActivity: loadPreset(" + presetNum + ") — channel=" + channel
                + ", index=" + index + ", priznak=" + priznak
                + ", PORT_prm=" + prm + ", PORT_prd=" + prd + ", ip=" + ip);

        if (PmrService.pmrSocket != null) {
            AppLog.add("MainActivity: loadPreset → recreateSocket");
            PmrService.pmrSocket.recreateSocket(this);
        } else {
            AppLog.add("MainActivity: loadPreset — pmrSocket == null (сервис не запущен), recreateSocket пропущен");
        }

        updateUI();

        String info = "Загружено из ячейки " + presetNum + ":\n" +
                "Канал: " + channel + "\n" +
                "Индекс: " + index + "\n" +
                "Признак: " + priznak + "\n" +
                "PORT_prm: " + prm + "\n" +
                "PORT_prd: " + prd + "\n" +
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
        currentChannel = prefs.getInt("current_channel", 4);
        AppLog.add("MainActivity: onResume, isAdminMode=" + isAdminMode + ", currentChannel=" + currentChannel);
        updateUI();
        registerReceiver(voiceReceiver, new IntentFilter("PMR_VOICE_RECEIVED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppLog.add("MainActivity: onPause");
        unregisterReceiver(voiceReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppLog.add("MainActivity: onDestroy");
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopService(new Intent(this, PmrService.class));
    }
}