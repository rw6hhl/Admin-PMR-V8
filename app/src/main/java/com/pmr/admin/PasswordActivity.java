package com.pmr.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);

        EditText editPassword = findViewById(R.id.editPassword);
        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {
            String pass = editPassword.getText().toString();
            if (pass.equals("admin")) { // Пароль по умолчанию
                getSharedPreferences("pmr_prefs", MODE_PRIVATE).edit().putBoolean("is_admin_mode", true).apply();
                Toast.makeText(this, "Режим Админа активирован", Toast.LENGTH_SHORT).show();
                finish(); // Возвращаемся на MainActivity
            } else {
                Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show();
            }
        });
    }
}