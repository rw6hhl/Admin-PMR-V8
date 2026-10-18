package com.pmr.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private EditText editMailIndex, editPriznak, editPortPrm, editPortPrd, editIp;
    private Switch switchSimplex;
    private LinearLayout layoutServerSettings;
    private Button btnChanList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("pmr_prefs", MODE_PRIVATE);

        editMailIndex = findViewById(R.id.editMailIndex);
        editPriznak = findViewById(R.id.editPriznak);
        editPortPrm = findViewById(R.id.editPortPrm);
        editPortPrd = findViewById(R.id.editPortPrd);
        editIp = findViewById(R.id.editIp);
        switchSimplex = findViewById(R.id.switchSimplex);
        layoutServerSettings = findViewById(R.id.layoutServerSettings);
        btnChanList = findViewById(R.id.btnChanList);

        // Загрузка
        editMailIndex.setText(prefs.getString("MyMailIndex", "111111"));
        editPriznak.setText(prefs.getString("Priznak_pmr", "26001"));
        editPortPrm.setText(String.valueOf(prefs.getInt("PORT_prm", 5323)));
        editPortPrd.setText(String.valueOf(prefs.getInt("PORT_prd", 16000)));
        editIp.setText(prefs.getString("ip_server", "185.221.154.39"));
        switchSimplex.setChecked(prefs.getBoolean("simplex", false));

        // Показываем кнопку "Список" только в режиме Админа
        if (prefs.getBoolean("is_admin_mode", false)) {
            btnChanList.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btnToggleServer).setOnClickListener(v -> {
            if (layoutServerSettings.getVisibility() == View.GONE) {
                layoutServerSettings.setVisibility(View.VISIBLE);
            } else {
                layoutServerSettings.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.btnCodecDialog).setOnClickListener(v -> showCodecDialog());
        
        btnChanList.setOnClickListener(v -> {
            startActivity(new Intent(this, ChanListActivity.class));
        });

        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveSettings());
    }

    private void showCodecDialog() {
        String[] codecs = {"GSM 32 kbps", "PCM 8 bit 8000 Hz", "G711 8000 Hz", "G711 16000 Hz", "PCM 8 bit 16000 Hz", "PCM 16 bit 8000 Hz", "PCM 16 bit 16000 Hz"};
        int currentCodec = prefs.getInt("codec", 3);
        new AlertDialog.Builder(this)
                .setTitle("Режим передачи")
                .setSingleChoiceItems(codecs, currentCodec, (dialog, which) -> {
                    prefs.edit().putInt("codec", which).apply();
                    dialog.dismiss();
                })
                .show();
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("MyMailIndex", editMailIndex.getText().toString());
        editor.putString("Priznak_pmr", editPriznak.getText().toString());
        editor.putInt("PORT_prm", Integer.parseInt(editPortPrm.getText().toString()));
        editor.putInt("PORT_prd", Integer.parseInt(editPortPrd.getText().toString()));
        editor.putString("ip_server", editIp.getText().toString());
        editor.putBoolean("simplex", switchSimplex.isChecked());
        editor.apply();
        finish();
    }
}