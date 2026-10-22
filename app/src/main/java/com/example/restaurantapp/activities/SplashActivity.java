package com.example.restaurantapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.restaurantapp.R;

public class SplashActivity extends AppCompatActivity {

    // Tiempo de duración del splash (2000 ms = 2 segundos)
    private static final int SPLASH_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Forzar modo claro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Usamos Handler para esperar 2 segundos y luego decidir a dónde ir
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Verificamos si el usuario ya tiene una sesión activa
            SharedPreferences preferences = getSharedPreferences("RestaurantPrefs", MODE_PRIVATE);
            boolean isLoggedIn = preferences.getBoolean("isLoggedIn", false);

            Intent intent;
            if (isLoggedIn) {
                // Si ya inició sesión, va directo al Home
                intent = new Intent(SplashActivity.this, HomeActivity.class);
            } else {
                // Si no, va al Login
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(intent);
            finish(); // Destruimos el Splash para que el usuario no pueda volver atrás con el botón físico

        }, SPLASH_DURATION);
    }
}