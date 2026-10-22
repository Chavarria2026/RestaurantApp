package com.example.restaurantapp.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.example.restaurantapp.R;
import com.example.restaurantapp.database.AppDatabase;
import com.example.restaurantapp.models.Pedido;
import com.example.restaurantapp.models.Usuario;
import com.example.restaurantapp.repositories.PedidoRepository;
import com.example.restaurantapp.utils.ValidationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PerfilFragment extends Fragment {

    private TextView tvUsuario, tvEmail, tvPendientes;
    private EditText etNombreUpdate, etPasswordUpdate;
    private Button btnSincronizar, btnGuardarCambios;

    private AppDatabase db;
    private PedidoRepository repository;
    private FirebaseAuth mAuth;
    private FirebaseFirestore dbFirestore;
    private String emailLogueado;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        tvUsuario = view.findViewById(R.id.tvUsuario);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPendientes = view.findViewById(R.id.tvPendientes);
        etNombreUpdate = view.findViewById(R.id.etNombreUpdate);
        etPasswordUpdate = view.findViewById(R.id.etPasswordUpdate);
        btnSincronizar = view.findViewById(R.id.btnSincronizar);
        btnGuardarCambios = view.findViewById(R.id.btnGuardarCambios);

        db = AppDatabase.getInstance(requireContext());
        repository = new PedidoRepository(requireContext());
        mAuth = FirebaseAuth.getInstance();
        dbFirestore = FirebaseFirestore.getInstance();

        SharedPreferences preferences = requireContext().getSharedPreferences("RestaurantPrefs", 0);
        String nombre = preferences.getString("loggedUser", "Usuario");
        emailLogueado = preferences.getString("email", "usuario@ejemplo.com");

        tvUsuario.setText(nombre);
        tvEmail.setText(emailLogueado);
        etNombreUpdate.setText(nombre);

        // Sincronización de Pedidos
        btnSincronizar.setOnClickListener(v -> {
            btnSincronizar.setEnabled(false);
            btnSincronizar.setText("Sincronizando...");
            repository.sincronizarPendientes(cantidad -> {
                requireActivity().runOnUiThread(() -> {
                    btnSincronizar.setEnabled(true);
                    btnSincronizar.setText("Sincronizar con la Nube");
                    if (cantidad > 0) {
                        Toast.makeText(getContext(), "✅ Se sincronizaron " + cantidad + " pedidos", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "No hay pedidos pendientes", Toast.LENGTH_SHORT).show();
                    }
                    actualizarContador();
                });
            });
        });

        // Actualizar Perfil
        btnGuardarCambios.setOnClickListener(v -> confirmarActualizacion());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        actualizarContador();
    }

    private void actualizarContador() {
        List<Pedido> pendientes = db.pedidoDao().getPedidosNoSincronizados();
        int cantidad = pendientes != null ? pendientes.size() : 0;

        if (cantidad > 0) {
            tvPendientes.setText("⚠️ Pedidos pendientes por subir: " + cantidad);
            tvPendientes.setTextColor(0xFFF44336);
        } else {
            tvPendientes.setText("✅ Todo sincronizado con la nube");
            tvPendientes.setTextColor(0xFF4CAF50);
        }
    }

    private void confirmarActualizacion() {
        String nuevoNombre = etNombreUpdate.getText().toString().trim();
        String nuevoPassword = etPasswordUpdate.getText().toString().trim();

        if (nuevoNombre.length() < 7) {
            etNombreUpdate.setError("Mínimo 7 caracteres");
            return;
        }
        if (!nuevoPassword.isEmpty() && !ValidationUtils.isValidPassword(nuevoPassword)) {
            etPasswordUpdate.setError("Mínimo 8 caracteres (letras y números)");
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Actualización")
                .setMessage("¿Estás seguro de actualizar tus datos?\n\nNombre: " + nuevoNombre +
                        (nuevoPassword.isEmpty() ? "" : "\nContraseña: (Se cambiará)"))
                .setPositiveButton("Sí, actualizar", (dialog, which) -> {
                    ejecutarActualizacion(nuevoNombre, nuevoPassword);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void ejecutarActualizacion(String nuevoNombre, String nuevoPassword) {
        btnGuardarCambios.setEnabled(false);

        // 1. ACTUALIZAR EN ROOM (LOCAL)
        Usuario usuarioLocal = db.usuarioDao().getUsuarioByEmail(emailLogueado);
        if (usuarioLocal != null) {
            usuarioLocal.setNombre(nuevoNombre);
            if (!nuevoPassword.isEmpty()) {
                usuarioLocal.setPassword(nuevoPassword);
            }
            db.usuarioDao().update(usuarioLocal);
        }

        // 2. ACTUALIZAR EN FIREBASE (NUBE)
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // 2.1 Actualizar el Nombre en Firestore
            Map<String, Object> updates = new HashMap<>();
            updates.put("nombre", nuevoNombre);
            dbFirestore.collection("usuarios").document(emailLogueado).update(updates);

            // 2.2 Actualizar Contraseña (si se ingresó una nueva)
            if (!nuevoPassword.isEmpty()) {
                user.updatePassword(nuevoPassword).addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(getContext(), "Nota: Firebase pide volver a iniciar sesión para cambiar contraseña (se actualizó en local).", Toast.LENGTH_LONG).show();
                    }
                });
            }

            // Actualizar SharedPreferences locales
            SharedPreferences preferences = requireContext().getSharedPreferences("RestaurantPrefs", 0);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("loggedUser", nuevoNombre);
            if (!nuevoPassword.isEmpty()) {
                editor.putString("savedPassword", nuevoPassword); // Si usó el chip
            }
            editor.apply();

            // Actualizar UI
            tvUsuario.setText(nuevoNombre);
            etPasswordUpdate.setText("");
            Toast.makeText(getContext(), "✅ Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
            btnGuardarCambios.setEnabled(true);
        }
    }
}