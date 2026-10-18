package com.pmr.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
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
    
    private TextView tvChannel, tvIndex, tvMode, tvInChannel, tvReceive;
    private ImageView imgRed, imgGreen;
    private Button btnMenu, btnUp, btnDown, btnSave1, btnSave2, btnSave3, btnSave4, btnSave5, btnCall;
    private CheckBox checkBoxDtmf;
    private Button btnPTT, btnSecretMode;

    private int currentChannel = 1;
    private boolean isRecording = false;
    private boolean isAdminMode = false;
    private long secretClickTime = 0;

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
        
        // Запуск сервиса
        Intent serviceIntent = new Intent(this, PmrService.class);
        startService(serviceIntent);
    }

    private void initViews() {
        tvChannel = findViewById(R.id.textView);
        tvIndex = findViewById(R.id.textView4);
        tvMode = findViewById(R.id.textView5);
        tvInChannel = findViewById(R.id.textView6);
        tvReceive = findViewById(R.id.textView8);
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
        
        btnPTT = findViewById(R.id.button5);
        checkBoxDtmf = findViewById(R.id.checkBox);
        btnSecretMode = findViewById(R.id.btnSecretMode);
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnUp.setOnClickListener(v -> {
            if (currentChannel < 8) {
                currentChannel++;
                updateUI();
                saveChannel();
            }
        });

        btnDown.setOnClickListener(v -> {
            if (currentChannel > 0) {
                currentChannel--;
                if (currentChannel == 0) tvChannel.setText("R");
                else tvChannel.setText(String.valueOf(currentChannel));
                saveChannel();
            }
        });

        btnSave1.setOnClickListener(v -> savePreset(1));
        btnSave2.setOnClickListener(v -> savePreset(2));
        btnSave3.setOnClickListener(v -> savePreset(3));
        btnSave4.setOnClickListener(v -> savePreset(4));
        btnSave5.setOnClickListener(v -> savePreset(5));

        btnCall.setOnClickListener(v -> {
            Toast.makeText(this, "Вызов отправлен", Toast.LENGTH_SHORT).show();
        });

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

        // Скрытая кнопка (долгое нажатие для входа/выхода из режима Админа)
        btnSecretMode.setOnLongClickListener(v -> {
            isAdminMode = !isAdminMode;
            prefs.edit().putBoolean("is_admin_mode", isAdminMode).apply();
            String modeStr = isAdminMode ? "РЕЖИМ АДМИНА" : "РЕЖИМ КЛИЕНТА";
            Toast.makeText(this, modeStr, Toast.LENGTH_SHORT).show();
            updateUI();
            return true;
        });
    }

    private void savePreset(int presetNum) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("preset_channel_" + presetNum, currentChannel);
        editor.apply();
        Toast.makeText(this, "Канал " + currentChannel + " сохранен в ячейку " + presetNum, Toast.LENGTH_SHORT).show();
    }

    private void saveChannel() {
        prefs.edit().putInt("current_channel", currentChannel).apply();
    }

    private void updateUI() {
        tvChannel.setText(String.valueOf(currentChannel));
        String index = prefs.getString("MyMailIndex", "111111");
        tvIndex.setText(index);
        boolean isSimplex = prefs.getBoolean("simplex", false);
        tvMode.setText(isSimplex ? "simplex" : "duplex");
        tvInChannel.setText("In chanel: " + currentChannel);
        
        if (isAdminMode) {
            // Визуальное отличие для админа (например, меняем цвет текста канала)
            tvChannel.setTextColor(getResources().getColor(R.color.active));
        } else {
            tvChannel.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, PmrService.class);
        stopService(serviceIntent);
    }
}