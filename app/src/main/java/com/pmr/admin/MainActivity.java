package com.pmr.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private TextView tvChannel, tvIndex, tvMode, tvReceive, tvSpeakingId;
    private ImageView imgRed, imgGreen;
    private Button btnMenu, btnUp, btnDown, btnSave1, btnSave2, btnSave3, btnSave4, btnSave5, btnCall, btnPTT, btnSecretMode;

    private int currentChannel = 1;
    private boolean isRecording = false;
    private boolean isAdminMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        prefs = getSharedPreferences("pmr_prefs", MODE_PRIVATE);
        currentChannel = prefs.getInt("current_channel", 1);
        isAdminMode = prefs.getBoolean("is_admin_mode", false);

        initViews();
        setupListeners();
        updateUI();
        
        Intent serviceIntent = new Intent(this, PmrService.class);
        startService(serviceIntent);
    }

    private void initViews() {
        tvChannel = findViewById(R.id.textView);
        tvIndex = findViewById(R.id.textView4);
        tvMode = findViewById(R.id.textView5);
        tvReceive = findViewById(R.id.textView8);
        tvSpeakingId = findViewById(R.id.tvSpeakingId);
        imgRed = findViewById(R.id.imageView);
        imgGreen = findViewById(R.id.imageView2);

        btnMenu = findViewById(R.id.button4);
        btnUp = findViewById(R.id.button1);
        btnDown = findViewById(R.id.button2);
        btnCall = findViewById(R.id.button);
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
        
        btnMenu.setOnLongClickListener(v -> {
            if (isAdminMode) {
                startActivity(new Intent(this, ChanListActivity.class));
                return true;
            }
            return false;
        });

        btnSecretMode.setOnClickListener(v -> {
            if (!isAdminMode) {
                startActivity(new Intent(this, PasswordActivity.class));
            } else {
                isAdminMode = false;
                prefs.edit().putBoolean("is_admin_mode", false).apply();
                Toast.makeText(this, "Режим Клиента", Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });

        // Логика переключения каналов
        btnUp.setOnClickListener(v -> {
            if (currentChannel < 8) {
                currentChannel++;
                updateUI();
            }
        });

        btnDown.setOnClickListener(v -> {
            if (currentChannel > 0) {
                currentChannel--;
                updateUI();
            }
        });

        // Логика ячеек (сохранение / загрузка по нажатию)
        btnSave1.setOnClickListener(v -> loadPreset(1));
        btnSave2.setOnClickListener(v -> loadPreset(2));
        btnSave3.setOnClickListener(v -> loadPreset(3));
        btnSave4.setOnClickListener(v -> loadPreset(4));
        btnSave5.setOnClickListener(v -> loadPreset(5));

        // Долгое нажатие для сохранения в ячейку
        btnSave1.setOnLongClickListener(v -> { savePreset(1); return true; });
        btnSave2.setOnLongClickListener(v -> { savePreset(2); return true; });
        btnSave3.setOnLongClickListener(v -> { savePreset(3); return true; });
        btnSave4.setOnLongClickListener(v -> { savePreset(4); return true; });
        btnSave5.setOnLongClickListener(v -> { savePreset(5); return true; });

        // PTT
        btnPTT.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isRecording = true;
                    imgRed.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Передача...", Toast.LENGTH_SHORT).show();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isRecording = false;
                    imgRed.setVisibility(View.INVISIBLE);
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
        editor.putInt("preset_codec_" + presetNum, prefs.getInt("codec", 3));
        editor.putBoolean("preset_simplex_" + presetNum, prefs.getBoolean("simplex", false));
        editor.apply();
        Toast.makeText(this, "Настройки сохранены в ячейку " + presetNum, Toast.LENGTH_SHORT).show();
    }

    private void loadPreset(int presetNum) {
        int channel = prefs.getInt("preset_channel_" + presetNum, currentChannel);
        String index = prefs.getString("preset_index_" + presetNum, prefs.getString("MyMailIndex", "111111"));
        String priznak = prefs.getString("preset_priznak_" + presetNum, prefs.getString("Priznak_pmr", "26001"));
        String ip = prefs.getString("preset_ip_" + presetNum, prefs.getString("ip_server", "185.221.154.39"));
        int codec = prefs.getInt("preset_codec_" + presetNum, prefs.getInt("codec", 3));
        boolean isSimplex = prefs.getBoolean("preset_simplex_" + presetNum, prefs.getBoolean("simplex", false));

        currentChannel = channel;
        
        prefs.edit()
            .putInt("current_channel", channel)
            .putString("MyMailIndex", index)
            .putString("Priznak_pmr", priznak)
            .putString("ip_server", ip)
            .putInt("codec", codec)
            .putBoolean("simplex", isSimplex)
            .apply();

        updateUI();
        Toast.makeText(this, "Загружена ячейка " + presetNum, Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        tvChannel.setText(currentChannel == 0 ? "R" : String.valueOf(currentChannel));
        tvIndex.setText(prefs.getString("MyMailIndex", "111111"));
        tvMode.setText(prefs.getBoolean("simplex", false) ? "simplex" : "duplex");
        tvReceive.setText("No receive"); 
        
        if (isAdminMode) {
            tvChannel.setTextColor(getResources().getColor(R.color.active));
        } else {
            tvChannel.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAdminMode = prefs.getBoolean("is_admin_mode", false);
        currentChannel = prefs.getInt("current_channel", 1);
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, PmrService.class);
        stopService(serviceIntent);
    }
}