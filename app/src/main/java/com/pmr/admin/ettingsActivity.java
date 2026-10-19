package com.pmr.admin;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQ_OPEN_LIST = 201;
    private SharedPreferences prefs;
    private EditText editMailIndex, editPriznak, editPortPrm, editPortPrd, editIp;
    private LinearLayout layoutServerSettings;
    private Button btnChanList, btnLogs, btnLoadList;

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
        layoutServerSettings = findViewById(R.id.layoutServerSettings);
        btnChanList = findViewById(R.id.btnChanList);
        btnLogs = findViewById(R.id.btnLogs);
        btnLoadList = findViewById(R.id.btnLoadList);

        editMailIndex.setText(prefs.getString("MyMailIndex", "51953"));
        editPriznak.setText(prefs.getString("Priznak_pmr", "26000"));
        editPortPrm.setText(String.valueOf(prefs.getInt("PORT_prm", 5254)));
        editPortPrd.setText(String.valueOf(prefs.getInt("PORT_prd", 16000)));
        editIp.setText(prefs.getString("ip_server", "185.221.154.39"));

        if (prefs.getBoolean("is_admin_mode", false)) {
            btnChanList.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btnToggleServer).setOnClickListener(v -> {
            layoutServerSettings.setVisibility(layoutServerSettings.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
        });

        findViewById(R.id.btnCodecDialog).setOnClickListener(v -> showCodecDialog());

        btnChanList.setOnClickListener(v -> startActivity(new Intent(this, ChanListActivity.class)));
        btnLogs.setOnClickListener(v -> startActivity(new Intent(this, LogActivity.class)));

        btnLoadList.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            startActivityForResult(intent, REQ_OPEN_LIST);
        });

        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveSettings());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OPEN_LIST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                if (is == null) {
                    Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show();
                    return;
                }
                int count = NameBook.importFromStream(this, is);
                is.close();
                if (count == 0) {
                    Toast.makeText(this, "Файл пуст или неверный формат", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Загружено имён: " + count, Toast.LENGTH_LONG).show();
                    AppLog.add("NameBook: загружено имён из list.txt: " + count);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                AppLog.add("NameBook: ошибка загрузки list.txt: " + e.getMessage());
            }
        }
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
        editor.apply();
        finish();
    }
}