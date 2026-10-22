package com.example.restaurantapp.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.ProductoSelectorAdapter;
import com.example.restaurantapp.database.AppDatabase;
import com.example.restaurantapp.models.DetallePedido;
import com.example.restaurantapp.models.ItemCarrito;
import com.example.restaurantapp.models.Pedido;
import com.example.restaurantapp.models.Producto;
import com.example.restaurantapp.repositories.PedidoRepository;
import java.util.ArrayList;
import java.util.List;

public class NuevoPedidoActivity extends AppCompatActivity {

    private EditText etNumeroMesa, etNotas, etBuscarProducto;
    private ImageView ivLimpiarBusqueda;
    private RecyclerView rvProductos;
    private LinearLayout layoutCarrito, layoutTotales;
    private TextView tvSubtotal, tvImpuesto, tvTotal, tvCarritoVacio;
    private Button btnConfirmarPedido;

    private AppDatabase db;
    private PedidoRepository pedidoRepository;
    private List<Producto> listaProductos, listaProductosOriginal;
    private List<ItemCarrito> carrito;
    private ProductoSelectorAdapter productoAdapter;
    private int usuarioId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nuevo_pedido);

        etNumeroMesa = findViewById(R.id.etNumeroMesa);
        etNotas = findViewById(R.id.etNotas);
        etBuscarProducto = findViewById(R.id.etBuscarProducto);
        ivLimpiarBusqueda = findViewById(R.id.ivLimpiarBusqueda);
        rvProductos = findViewById(R.id.rvProductos);
        layoutCarrito = findViewById(R.id.layoutCarrito);
        tvCarritoVacio = findViewById(R.id.tvCarritoVacio);
        layoutTotales = findViewById(R.id.layoutTotales);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvImpuesto = findViewById(R.id.tvImpuesto);
        tvTotal = findViewById(R.id.tvTotal);
        btnConfirmarPedido = findViewById(R.id.btnConfirmarPedido);

        etNumeroMesa.setTextColor(0xFF000000);
        etNotas.setTextColor(0xFF000000);
        etBuscarProducto.setTextColor(0xFF000000);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Nuevo Pedido");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = AppDatabase.getInstance(this);
        pedidoRepository = new PedidoRepository(this);
        carrito = new ArrayList<>();

        SharedPreferences preferences = getSharedPreferences("RestaurantPrefs", MODE_PRIVATE);
        usuarioId = preferences.getInt("userId", 1);

        cargarProductos();

        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        productoAdapter = new ProductoSelectorAdapter(listaProductos, this::agregarAlCarrito);
        rvProductos.setAdapter(productoAdapter);

        etBuscarProducto.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarProductos(s.toString());
                ivLimpiarBusqueda.setVisibility(s.toString().trim().isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        ivLimpiarBusqueda.setOnClickListener(v -> {
            etBuscarProducto.setText("");
            filtrarProductos("");
        });

        btnConfirmarPedido.setOnClickListener(v -> confirmarPedido());
    }

    private void cargarProductos() {
        listaProductosOriginal = db.productoDao().getAllProductos();
        if (listaProductosOriginal == null || listaProductosOriginal.isEmpty()) {
            listaProductosOriginal = new ArrayList<>();
            // Código de productos por defecto omitido para brevedad
        }
        listaProductos = new ArrayList<>(listaProductosOriginal);
    }

    private void filtrarProductos(String texto) {
        listaProductos.clear();
        if (texto == null || texto.trim().isEmpty()) {
            listaProductos.addAll(listaProductosOriginal);
        } else {
            String busqueda = texto.toLowerCase().trim();
            for (Producto p : listaProductosOriginal) {
                if (p.getNombre().toLowerCase().contains(busqueda)) {
                    listaProductos.add(p);
                }
            }
        }
        productoAdapter.actualizarLista(listaProductos);
    }

    private void agregarAlCarrito(Producto producto) {
        for (ItemCarrito item : carrito) {
            if (item.getProductoId() == producto.getId()) {
                item.setCantidad(item.getCantidad() + 1);
                actualizarCarrito();
                return;
            }
        }
        carrito.add(new ItemCarrito(producto.getId(), producto.getNombre(), 1, producto.getPrecio()));
        actualizarCarrito();
    }

    private void actualizarCarrito() {
        layoutCarrito.removeAllViews();

        if (carrito.isEmpty()) {
            tvCarritoVacio.setVisibility(View.VISIBLE);
            layoutCarrito.setVisibility(View.GONE);
            layoutTotales.setVisibility(View.GONE);
            return;
        }

        tvCarritoVacio.setVisibility(View.GONE);
        layoutCarrito.setVisibility(View.VISIBLE);
        layoutTotales.setVisibility(View.VISIBLE);

        double subtotal = 0;

        for (ItemCarrito item : carrito) {
            subtotal += item.getSubtotal();
            View itemView = getLayoutInflater().inflate(R.layout.item_carrito, null);
            TextView tvNombre = itemView.findViewById(R.id.tvNombreCarrito);
            TextView tvCantidad = itemView.findViewById(R.id.tvCantidadCarrito);
            TextView tvPrecio = itemView.findViewById(R.id.tvPrecioCarrito);
            Button btnMas = itemView.findViewById(R.id.btnMas);
            Button btnMenos = itemView.findViewById(R.id.btnMenos);
            Button btnEliminar = itemView.findViewById(R.id.btnEliminar);

            tvNombre.setText(item.getNombre());
            tvCantidad.setText(String.valueOf(item.getCantidad()));
            tvPrecio.setText("$" + String.format("%.2f", item.getSubtotal()));

            btnMas.setOnClickListener(v -> { item.setCantidad(item.getCantidad() + 1); actualizarCarrito(); });
            btnMenos.setOnClickListener(v -> { if (item.getCantidad() > 1) { item.setCantidad(item.getCantidad() - 1); actualizarCarrito(); }});
            btnEliminar.setOnClickListener(v -> { carrito.remove(item); actualizarCarrito(); });

            layoutCarrito.addView(itemView);
        }

        double impuesto = subtotal * 0.13;
        double total = subtotal + impuesto;

        tvSubtotal.setText("$" + String.format("%.2f", subtotal));
        tvImpuesto.setText("$" + String.format("%.2f", impuesto));
        tvTotal.setText("$" + String.format("%.2f", total));
    }

    private void confirmarPedido() {
        String numeroMesaStr = etNumeroMesa.getText().toString().trim();

        if (numeroMesaStr.isEmpty()) {
            etNumeroMesa.setError("Ingrese el número de mesa");
            return;
        }
        if (carrito.isEmpty()) {
            Toast.makeText(this, "❌ Agregue productos al carrito", Toast.LENGTH_SHORT).show();
            return;
        }

        int numeroMesa = Integer.parseInt(numeroMesaStr);
        double subtotal = 0;
        for (ItemCarrito item : carrito) {
            subtotal += item.getSubtotal();
        }
        double impuesto = subtotal * 0.13;
        double total = subtotal + impuesto;
        String notas = etNotas.getText().toString().trim();

        // 1. Creamos el Pedido
        Pedido pedido = new Pedido(numeroMesa, subtotal, impuesto, total, "pendiente", usuarioId, notas);
        pedido.setSincronizado(false);

        // 2. Creamos la lista de detalles (PRODUCTOS)
        List<DetallePedido> listaDetalles = new ArrayList<>();
        for (ItemCarrito item : carrito) {
            listaDetalles.add(new DetallePedido(
                    0,
                    item.getProductoId(),
                    item.getNombre(),
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    ""
            ));
        }
        // 3. IMPORTANTE: Asignamos los detalles al pedido ANTES de enviarlo
        pedido.setDetalles(listaDetalles);

        // 4. Enviamos al repositorio
        pedidoRepository.insertarPedido(pedido);

        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}