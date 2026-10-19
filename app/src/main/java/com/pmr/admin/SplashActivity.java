package com.pmr.admin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tvCall = findViewById(R.id.tvCall);
        TextView tvQth = findViewById(R.id.tvQth);

        SharedPreferences prefs = getSharedPreferences("pmr_prefs", MODE_PRIVATE);
        String call = prefs.getString("Call", "ПОЗЫВНОЙ");
        String qth = prefs.getString("QTH", "QTH");

        tvCall.setText(call);
        tvQth.setText(qth);

        // Переход на главный экран через 2 секунды
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }
}