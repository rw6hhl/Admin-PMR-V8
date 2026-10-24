package com.pmr.admin;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_MIC = 101;
    private SharedPreferences prefs;
    private TextView tvChannel, tvIndex, tvSpeakingId, tvVersion, tvActiveUsers;
    private ImageView imgRed, btnPower;
    private Button btnMenu, btnUp, btnDown, btnSave1, btnSave2, btnSave3, btnSave4, btnSave5, btnCallOrList, btnPTT, btnSecretMode;
    private boolean isAdminMode = false;
    private PowerManager.WakeLock wakeLock;
    private int currentChannel = 4;
    private ToneGenerator toneGen;
    private boolean soundEnabled = true;

    private int secretClickCount = 0;
    private final Handler secretHandler = new Handler(Looper.getMainLooper());
    private final Runnable secretReset = () -> {
        secretClickCount = 0;
        AppLog.add("MainActivity: secretClickCount сброшен (таймаут)");
    };

    private final Handler speakingHandler = new Handler(Looper.getMainLooper());
    private final Runnable resetSpeakingId = () -> {
        tvSpeakingId.setText("");
        AppLog.add("MainActivity: resetSpeakingId — сброс текста CALL (400 мс)");
    };

    private BroadcastReceiver voiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int clientNumber = intent.getIntExtra("clientId", -1);
            AppLog.add("MainActivity: BroadcastReceiver получил PMR_VOICE_RECEIVED, number=" + clientNumber);
            if (clientNumber != -1) {
                int id = PmrSocket.getClientIdByNumber(clientNumber);
                if (id == -1) {
                    AppLog.add("MainActivity: CALL пропущен — number=" + clientNumber + " не найден в lastChanList");
                    return;
                }
                if (PmrSocket.isAdminObserver(id)) {
                    AppLog.add("MainActivity: CALL пропущен — id=" + id + " (админ/наблюдатель)");
                    return;
                }
                String name = NameBook.getName(MainActivity.this, id);
                String text = String.format("%d %s", id, name);
                AppLog.add("MainActivity: вывод в CALL — number=" + clientNumber + ", id=" + id + ", text=" + text);
                tvSpeakingId.setText(text);
                tvSpeakingId.setTextColor(Color.RED);
                tvSpeakingId.setTypeface(null, Typeface.BOLD);
                speakingHandler.removeCallbacks(resetSpeakingId);
                speakingHandler.postDelayed(resetSpeakingId, 400);
            }
        }
    };

    private BroadcastReceiver usersReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int count = intent.getIntExtra("count", -1);
            if (count != -1) {
                updateActiveUsers(count);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppLog.init(getApplicationContext(), BuildConfig.VERSION_NAME);

        prefs = getSharedPreferences("pmr_prefs", MODE_PRIVATE);
        AppLog.add("MainActivity: onCreate");

        soundEnabled = prefs.getBoolean("sound_enabled", true);
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
        } catch (Exception e) {
            AppLog.add("MainActivity: ToneGenerator ошибка — " + e.getMessage());
        }

        String savedVersion = prefs.getString("app_version", "0");
        if (!savedVersion.equals("9.77")) {
            prefs.edit()
                .putString("app_version", "9.77")
                .putString("MyMailIndex", "51953")
                .putString("Priznak_pmr", "26000")
                .putInt("PORT_prm", 5253)
                .putInt("PORT_prd", 16000)
                .putString("ip_server", "185.221.154.39")
                .putInt("current_channel", 4)
                .putBoolean("simplex", true)
                .putBoolean("is_admin_mode", false)
                .apply();
            AppLog.add("MainActivity: Настройки сброшены (V9.77): Priznak=26000, Mail=51953, Ch=4, PORT_prm=5253, PORT_prd=16000");
        }

        requestMicPermission();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "PMR::WakeLock");
            wakeLock.acquire();
        }

        currentChannel = prefs.getInt("current_channel", 4);
        isAdminMode = prefs.getBoolean("is_admin_mode", false);
        AppLog.add("MainActivity: currentChannel=" + currentChannel + ", isAdminMode=" + isAdminMode);

        initViews();
        setupListeners();
        updateUI();
        updatePowerButton();

        AppLog.add("MainActivity: onCreate — сервис НЕ стартует автоматически (V9.77)");
    }

    private void beep() {
        if (!soundEnabled || toneGen == null) return;
        try {
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100);
        } catch (Exception ignored) {}
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
        tvActiveUsers = findViewById(R.id.textView6);
        imgRed = findViewById(R.id.imageView);
        btnPower = findViewById(R.id.btnPower);

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
        AppLog.add("MainActivity: initViews — найдены кнопки и TextView (btnPower есть)");
    }

    private void setupListeners() {
        btnPower.setOnClickListener(v -> {
            beep();
            if (PmrService.isRunning) {
                AppLog.add("MainActivity: btnPower → ВЫКЛ (stopService)");
                stopService(new Intent(this, PmrService.class));
            } else {
                AppLog.add("MainActivity: btnPower → ВКЛ (startService)");
                startService(new Intent(this, PmrService.class));
            }
            new Handler(Looper.getMainLooper()).postDelayed(this::updatePowerButton, 300);
        });

        btnMenu.setOnClickListener(v -> {
            beep();
            AppLog.add("MainActivity: нажата MENU → SettingsActivity");
            startActivity(new Intent(this, SettingsActivity.class));
        });

        btnCallOrList.setOnClickListener(v -> {
            beep();
            AppLog.add("MainActivity: нажата CALL/LIST, isAdminMode=" + isAdminMode);
            if (isAdminMode) {
                try {
                    AppLog.add("MainActivity: открываю ChanListActivity");
                    startActivity(new Intent(this, ChanListActivity.class));
                    AppLog.add("MainActivity: startActivity(ChanListActivity) — OK");
                } catch (Exception e) {
                    AppLog.add("MainActivity: ОШИБКА startActivity(ChanListActivity) — "
                            + e.getClass().getSimpleName() + ": " + e.getMessage());
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Вызов (CALL)", Toast.LENGTH_SHORT).show();
            }
        });

        btnSecretMode.setOnClickListener(v -> {
            beep();
            secretClickCount++;
            AppLog.add("MainActivity: нажата скрытая кнопка, count=" + secretClickCount);
            secretHandler.removeCallbacks(secretReset);
            if (secretClickCount == 2) {
                secretClickCount = 0;
                AppLog.add("MainActivity: ДВОЙНОЕ нажатие скрытой кнопки");
                if (prefs.getBoolean("ask_password", true)) {
                    showPasswordDialog();
                } else {
                    toggleAdminMode();
                }
            } else {
                secretHandler.postDelayed(secretReset, 500); // окно для второго нажатия - 500 мс
            }
        });

        btnUp.setOnClickListener(v -> {
            beep();
            if (currentChannel < 8) {
                int old = currentChannel;
                currentChannel++;
                updateUI();
                prefs.edit().putInt("current_channel", currentChannel).apply();
                AppLog.add("MainActivity: UP, канал " + old + " → " + currentChannel + " (recreateSocket)");
                if (PmrService.pmrSocket != null) {
                    PmrService.pmrSocket.recreateSocket(this);
                }
            }
        });

        btnDown.setOnClickListener(v -> {
            beep();
            if (currentChannel > 0) {
                int old = currentChannel;
                currentChannel--;
                updateUI();
                prefs.edit().putInt("current_channel", currentChannel).apply();
                AppLog.add("MainActivity: DOWN, канал " + old + " → " + currentChannel + " (recreateSocket)");
                if (PmrService.pmrSocket != null) {
                    PmrService.pmrSocket.recreateSocket(this);
                }
            }
        });

        btnSave1.setOnClickListener(v -> { beep(); loadPreset(1); });
        btnSave2.setOnClickListener(v -> { beep(); loadPreset(2); });
        btnSave3.setOnClickListener(v -> { beep(); loadPreset(3); });
        btnSave4.setOnClickListener(v -> { beep(); loadPreset(4); });
        btnSave5.setOnClickListener(v -> { beep(); loadPreset(5); });

        btnSave1.setOnLongClickListener(v -> { beep(); savePreset(1); return true; });
        btnSave2.setOnLongClickListener(v -> { beep(); savePreset(2); return true; });
        btnSave3.setOnLongClickListener(v -> { beep(); savePreset(3); return true; });
        btnSave4.setOnLongClickListener(v -> { beep(); savePreset(4); return true; });
        btnSave5.setOnLongClickListener(v -> { beep(); savePreset(5); return true; });

        btnPTT.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    beep();
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

    private void showPasswordDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Пароль");
        
        new AlertDialog.Builder(this)
                .setTitle("Вход в режим Админа")
                .setMessage("Введите пароль:")
                .setView(input)
                .setPositiveButton("Войти", (dialog, which) -> {
                    String pass = input.getText().toString();
                    if ("1234".equals(pass)) {
                        AppLog.add("MainActivity: пароль верный");
                        toggleAdminMode();
                    } else {
                        AppLog.add("MainActivity: неверный пароль");
                        Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void toggleAdminMode() {
        isAdminMode = !isAdminMode;
        prefs.edit().putBoolean("is_admin_mode", isAdminMode).apply();
        AppLog.add("MainActivity: переключение режима → isAdminMode=" + isAdminMode);
        Toast.makeText(this, isAdminMode ? "РЕЖИМ АДМИНА" : "РЕЖИМ КЛИЕНТА", Toast.LENGTH_SHORT).show();
        updateUI();
    }

    private void updatePowerButton() {
        if (btnPower == null) return;
        if (PmrService.isRunning) {
            btnPower.setAlpha(1.0f);
            btnPower.setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_ATOP);
            AppLog.add("MainActivity: updatePowerButton → ЗЕЛЁНЫЙ (работает)");
        } else {
            btnPower.setAlpha(1.0f);
            btnPower.setColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);
            AppLog.add("MainActivity: updatePowerButton → КРАСНЫЙ (выключено)");
        }
    }

    private void updateActiveUsers(int count) {
        if (tvActiveUsers == null) return;
        if (currentChannel != 4 && currentChannel != 5) {
            tvActiveUsers.setText(String.valueOf(count));
        } else {
            tvActiveUsers.setText("");
        }
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
            tvChannel.setTextColor(getResources().getColor(R.color.admin_channel));
            btnCallOrList.setText("LIST");
        } else {
            tvChannel.setTextColor(getResources().getColor(android.R.color.black));
            btnCallOrList.setText("CALL");
        }
        
        // Скрываем счетчик если канал 4 или 5
        if (tvActiveUsers != null && (currentChannel == 4 || currentChannel == 5)) {
            tvActiveUsers.setText("");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        soundEnabled = prefs.getBoolean("sound_enabled", true);
        isAdminMode = prefs.getBoolean("is_admin_mode", false);
        currentChannel = prefs.getInt("current_channel", 4);
        AppLog.add("MainActivity: onResume, isAdminMode=" + isAdminMode + ", currentChannel=" + currentChannel);
        updateUI();
        updatePowerButton();
        registerReceiver(voiceReceiver, new IntentFilter("PMR_VOICE_RECEIVED"));
        registerReceiver(usersReceiver, new IntentFilter("PMR_USERS_CHANGED"));

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppLog.add("MainActivity: onPause");
        unregisterReceiver(voiceReceiver);
        try { unregisterReceiver(usersReceiver); } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppLog.add("MainActivity: onDestroy");
        speakingHandler.removeCallbacks(resetSpeakingId);
        secretHandler.removeCallbacks(secretReset);
        if (toneGen != null) {
            try { toneGen.release(); } catch (Exception ignored) {}
            toneGen = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }
}