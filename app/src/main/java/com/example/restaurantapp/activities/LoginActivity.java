package com.example.restaurantapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.restaurantapp.R;
import com.example.restaurantapp.database.AppDatabase;
import com.example.restaurantapp.models.Usuario;
import com.example.restaurantapp.utils.NetworkUtils;
import com.example.restaurantapp.utils.ValidationUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText; // NUEVO IMPORT
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    // CAMBIO: Ahora son de tipo TextInputEditText
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private Chip chipRecordar;
    private SharedPreferences preferences;
    private FirebaseAuth mAuth;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicialización de los nuevos campos
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        chipRecordar = findViewById(R.id.chipRecordar);

        preferences = getSharedPreferences("RestaurantPrefs", MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        db = AppDatabase.getInstance(this);

        // Si ya hay sesión activa, ir directo al Home
        if (preferences.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
            return;
        }

        // Cargar credenciales si el Chip estaba activo
        if (preferences.getBoolean("recordar", false)) {
            etEmail.setText(preferences.getString("savedEmail", ""));
            etPassword.setText(preferences.getString("savedPassword", ""));
            chipRecordar.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> intentarLogin());
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void intentarLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Correo inválido");
            return;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            etPassword.setError("Mínimo 8 caracteres (letras y números)");
            return;
        }

        if (NetworkUtils.isNetworkAvailable(this)) {
            // LOGIN CON FIREBASE
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            guardarSesion(email, password);
                        } else {
                            // Si falla en Firebase, intentamos con Room (por si acaso)
                            intentarLoginLocal(email, password);
                        }
                    });
        } else {
            // LOGIN SIN INTERNET (Local Room)
            intentarLoginLocal(email, password);
        }
    }

    private void intentarLoginLocal(String email, String password) {
        Usuario user = db.usuarioDao().login(email, password);
        if (user != null) {
            guardarSesion(email, password);
            Toast.makeText(this, "Modo Offline: Login Local exitoso", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ Credenciales incorrectas", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarSesion(String email, String password) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("loggedUser", email);
        editor.putString("email", email);

        // Guardar rol (si usaste la implementación de roles)
        Usuario user = db.usuarioDao().getUsuarioByEmail(email);
        if (user != null) {
            editor.putString("userRole", user.getRol());
        } else {
            editor.putString("userRole", "mesero");
        }

        // Guardar credenciales si el Chip está activo
        if (chipRecordar.isChecked()) {
            editor.putBoolean("recordar", true);
            editor.putString("savedEmail", email);
            editor.putString("savedPassword", password);
        } else {
            editor.putBoolean("recordar", false);
            editor.remove("savedEmail");
            editor.remove("savedPassword");
        }
        editor.apply();

        Toast.makeText(this, "✅ Inicio de sesión exitoso", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}