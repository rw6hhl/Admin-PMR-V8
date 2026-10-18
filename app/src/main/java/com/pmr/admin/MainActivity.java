package com.pmr.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private TextView tvChannel, tvIndex, tvMode, tvInChannel, tvReceive, tvSpeakingId;
    private ImageView imgRed, imgGreen;
    private Button btnMenu, btnUp, btnDown, btnSave1, btnSave2, btnSave3, btnSave4, btnSave5, btnCall, btnPTT, btnSecretMode;
    private CheckBox checkBoxDtmf;

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
        tvInChannel = findViewById(R.id.textView6);
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
        checkBoxDtmf = findViewById(R.id.checkBox);
        btnSecretMode = findViewById(R.id.btnSecretMode);
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> {
            if (isAdminMode) {
                // Если админ, можно открыть список или настройки. Например, долгим нажатием.
                startActivity(new Intent(this, SettingsActivity.class));
            } else {
                startActivity(new Intent(this, SettingsActivity.class));
            }
        });

        // Долгое нажатие на MENU в режиме Админа открывает список абонентов
        btnMenu.setOnLongClickListener(v -> {
            if (isAdminMode) {
                startActivity(new Intent(this, ChanListActivity.class));
                return true;
            }
            return false;
        });

        // Вход в админ-режим по клику на скрытую область
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

        // Ячейки сохранения (логика сохранения текущих настроек)
        btnSave1.setOnClickListener(v -> savePreset(1));
        btnSave2.setOnClickListener(v -> savePreset(2));
        btnSave3.setOnClickListener(v -> savePreset(3));
        btnSave4.setOnClickListener(v -> savePreset(4));
        btnSave5.setOnClickListener(v -> savePreset(5));

        // PTT
        btnPTT.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isRecording = true;
                    imgRed.setVisibility(View.VISIBLE);
                    // audioEngine.startRecording(); // Раскомментировать при интеграции движка
                    Toast.makeText(this, "Передача...", Toast.LENGTH_SHORT).show();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isRecording = false;
                    imgRed.setVisibility(View.INVISIBLE);
                    // audioEngine.stopRecording();
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
        editor.apply();
        Toast.makeText(this, "Настройки сохранены в ячейку " + presetNum, Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        tvChannel.setText(String.valueOf(currentChannel));
        tvIndex.setText(prefs.getString("MyMailIndex", "111111"));
        tvMode.setText(prefs.getBoolean("simplex", false) ? "simplex" : "duplex");
        tvInChannel.setText("In chanel: " + currentChannel);
        
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
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, PmrService.class);
        stopService(serviceIntent);
    }
}