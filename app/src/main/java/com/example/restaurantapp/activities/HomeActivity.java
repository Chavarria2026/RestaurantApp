package com.example.restaurantapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.example.restaurantapp.R;
import com.example.restaurantapp.fragments.CocinaFragment; // <-- IMPORT NUEVO
import com.example.restaurantapp.fragments.InicioFragment;
import com.example.restaurantapp.fragments.MapaFragment;
import com.example.restaurantapp.fragments.MenuFragment;
import com.example.restaurantapp.fragments.PedidosFragment;
import com.example.restaurantapp.fragments.PerfilFragment;
import com.example.restaurantapp.fragments.VentasFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private SharedPreferences preferences;
    private String usuarioLogueado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Inicio");
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_drawer_open,
                R.string.nav_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        preferences = getSharedPreferences("RestaurantPrefs", MODE_PRIVATE);
        usuarioLogueado = preferences.getString("loggedUser", "Usuario");

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_inicio) {
                cargarFragment(new InicioFragment());
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Inicio");
                return true;
            } else if (itemId == R.id.nav_menu) {
                cargarFragment(new MenuFragment());
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Menú");
                return true;
            } else if (itemId == R.id.nav_perfil) {
                cargarFragment(new PerfilFragment());
                if (getSupportActionBar() != null) getSupportActionBar().setTitle("Perfil");
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            cargarFragment(new InicioFragment());
            bottomNavigationView.setSelectedItemId(R.id.nav_inicio);
        }
    }

    private void cargarFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_nuevo_pedido) {
            Intent intent = new Intent(this, NuevoPedidoActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_inicio) {
            cargarFragment(new InicioFragment());
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Inicio");
            bottomNavigationView.setSelectedItemId(R.id.nav_inicio);
        } else if (id == R.id.nav_menu) {
            cargarFragment(new MenuFragment());
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Menú");
            bottomNavigationView.setSelectedItemId(R.id.nav_menu);
        } else if (id == R.id.nav_pedidos) {
            cargarFragment(new PedidosFragment());
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Mis Pedidos");
        } else if (id == R.id.nav_cocina) { // <-- NUEVO BLOQUE
            cargarFragment(new CocinaFragment());
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Modo Cocina");
        } else if (id == R.id.nav_ventas) {
            cargarFragment(new VentasFragment());
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Ventas");
        } else if (id == R.id.nav_perfil) {
            cargarFragment(new PerfilFragment());
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Perfil");
        } else if (id == R.id.nav_mapa) {
            cargarFragment(new MapaFragment());
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Ubicación");
        } else if (id == R.id.nav_config) {
            Intent intent = new Intent(this, ConfiguracionActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_salir) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}