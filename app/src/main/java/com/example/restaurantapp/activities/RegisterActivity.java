package com.example.restaurantapp.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.restaurantapp.R;
import com.example.restaurantapp.database.AppDatabase;
import com.example.restaurantapp.models.Usuario;
import com.example.restaurantapp.utils.NetworkUtils;
import com.example.restaurantapp.utils.ValidationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNombre, etEmail, etPassword, etConfirmPassword;
    private Button btnGuardar, btnRegresar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore dbFirestore;
    private AppDatabase dbLocal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // INICIALIZAMOS CON LOS NOMBRES DEL NUEVO XML
        etNombre = findViewById(R.id.etNombreCompleto);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnRegresar = findViewById(R.id.btnRegresar);

        // Forzar color de texto
        etNombre.setTextColor(0xFF000000);
        etEmail.setTextColor(0xFF000000);
        etPassword.setTextColor(0xFF000000);
        etConfirmPassword.setTextColor(0xFF000000);

        mAuth = FirebaseAuth.getInstance();
        dbFirestore = FirebaseFirestore.getInstance();
        dbLocal = AppDatabase.getInstance(this);

        btnGuardar.setOnClickListener(v -> registrarUsuario());
        btnRegresar.setOnClickListener(v -> finish());
    }

    private void registrarUsuario() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // VALIDACIONES SEGÚN EL PDF
        if (nombre.length() < 7) {
            etNombre.setError("El nombre debe tener al menos 7 caracteres");
            etNombre.requestFocus();
            return;
        }
        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Correo electrónico no válido");
            etEmail.requestFocus();
            return;
        }
        if (!ValidationUtils.isValidPassword(password)) {
            etPassword.setError("Mínimo 8 caracteres (letras y números)");
            etPassword.requestFocus();
            return;
        }
        if (!ValidationUtils.doPasswordsMatch(password, confirmPassword)) {
            etConfirmPassword.setError("Las contraseñas no coinciden");
            etConfirmPassword.requestFocus();
            return;
        }

        // REGISTRO EN FIREBASE O LOCAL
        if (NetworkUtils.isNetworkAvailable(this)) {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            guardarEnFirestoreYRoom(nombre, email, password, true);
                        } else {
                            Toast.makeText(this, "Error Firebase: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            // Sin internet: Guardamos solo en local pendiente de sincronizar
            guardarEnFirestoreYRoom(nombre, email, password, false);
        }
    }

    private void guardarEnFirestoreYRoom(String nombre, String email, String password, boolean online) {
        // 1. Guardar en Room (Local)
        Usuario nuevoUsuario = new Usuario(nombre, email, password, "mesero");
        nuevoUsuario.setSincronizado(online); // Si no hay internet, se guarda como false
        long idLocal = dbLocal.usuarioDao().insert(nuevoUsuario);

        if (online) {
            // 2. Guardar en Firestore (Nube)
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("nombre", nombre);
            userMap.put("email", email);
            userMap.put("rol", "mesero");

            // Usamos el email como ID del documento para que sea más fácil de buscar
            dbFirestore.collection("usuarios").document(email)
                    .set(userMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "✅ Registrado en Nube y Local", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // Si falla Firestore, actualizamos Room a no sincronizado
                        // (asumiendo que tienes el método marcarComoSincronizado en tu DAO)
                        dbLocal.usuarioDao().marcarComoSincronizado(email);
                        Toast.makeText(this, "Guardado en local (Fallo Firestore)", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            Toast.makeText(this, "📶 Sin internet: Guardado en local. Ve a Perfil para sincronizar.", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}