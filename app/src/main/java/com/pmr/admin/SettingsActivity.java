package com.pmr.admin;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private EditText editIndex, editIp, editPort;
    private Switch switchSimplex;
    private RadioGroup radioGroupCodec;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("pmr_prefs", MODE_PRIVATE);

        editIndex = findViewById(R.id.editIndex);
        editIp = findViewById(R.id.editIp);
        editPort = findViewById(R.id.editPort);
        switchSimplex = findViewById(R.id.switchSimplex);
        radioGroupCodec = findViewById(R.id.radioGroupCodec);
        btnSave = findViewById(R.id.btnSaveSettings);

        // Загрузка
        editIndex.setText(prefs.getString("MyMailIndex", "111111"));
        editIp.setText(prefs.getString("ip_server", "185.221.154.39"));
        editPort.setText(String.valueOf(prefs.getInt("PORT_prd", 16000)));
        
        boolean isSimplex = prefs.getBoolean("simplex", false);
        switchSimplex.setChecked(isSimplex);

        int codec = prefs.getInt("codec", 3);
        if (codec == 0) ((RadioButton) findViewById(R.id.rbCodec0)).setChecked(true);
        else if (codec == 1) ((RadioButton) findViewById(R.id.rbCodec1)).setChecked(true);
        else if (codec == 2) ((RadioButton) findViewById(R.id.rbCodec2)).setChecked(true);
        else if (codec == 3) ((RadioButton) findViewById(R.id.rbCodec3)).setChecked(true);
        else if (codec == 4) ((RadioButton) findViewById(R.id.rbCodec4)).setChecked(true);
        else if (codec == 5) ((RadioButton) findViewById(R.id.rbCodec5)).setChecked(true);
        else if (codec == 6) ((RadioButton) findViewById(R.id.rbCodec6)).setChecked(true);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putString("MyMailIndex", editIndex.getText().toString());
        editor.putString("ip_server", editIp.getText().toString());
        
        try {
            editor.putInt("PORT_prd", Integer.parseInt(editPort.getText().toString()));
        } catch (NumberFormatException e) {
            editor.putInt("PORT_prd", 16000);
        }

        editor.putBoolean("simplex", switchSimplex.isChecked());

        int selectedCodecId = radioGroupCodec.getCheckedRadioButtonId();
        int codec = 3;
        if (selectedCodecId == R.id.rbCodec0) codec = 0;
        else if (selectedCodecId == R.id.rbCodec1) codec = 1;
        else if (selectedCodecId == R.id.rbCodec2) codec = 2;
        else if (selectedCodecId == R.id.rbCodec3) codec = 3;
        else if (selectedCodecId == R.id.rbCodec4) codec = 4;
        else if (selectedCodecId == R.id.rbCodec5) codec = 5;
        else if (selectedCodecId == R.id.rbCodec6) codec = 6;
        editor.putInt("codec", codec);

        editor.apply();
        finish();
    }
}