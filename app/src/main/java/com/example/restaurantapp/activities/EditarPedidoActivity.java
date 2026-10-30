package com.example.restaurantapp.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
import com.example.restaurantapp.repositories.PedidoRepository; // NUEVO IMPORT
import java.util.ArrayList;
import java.util.List;

public class EditarPedidoActivity extends AppCompatActivity {

    private EditText etNotas;
    private RecyclerView rvProductos;
    private LinearLayout layoutCarrito, layoutTotales;
    private TextView tvSubtotal, tvImpuesto, tvTotal, tvCarritoVacio;
    private Button btnGuardarCambios, btnAgregarProductos;

    private AppDatabase db;
    private PedidoRepository pedidoRepository; // NUEVA VARIABLE
    private List<Producto> listaProductos;
    private List<ItemCarrito> carrito;
    private ProductoSelectorAdapter productoAdapter;
    private int pedidoId;
    private Pedido pedidoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_pedido);

        pedidoId = getIntent().getIntExtra("pedido_id", -1);
        if (pedidoId == -1) {
            Toast.makeText(this, "Error: Pedido no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etNotas = findViewById(R.id.etNotas);
        rvProductos = findViewById(R.id.rvProductos);
        layoutCarrito = findViewById(R.id.layoutCarrito);
        tvCarritoVacio = findViewById(R.id.tvCarritoVacio);
        layoutTotales = findViewById(R.id.layoutTotales);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvImpuesto = findViewById(R.id.tvImpuesto);
        tvTotal = findViewById(R.id.tvTotal);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnAgregarProductos = findViewById(R.id.btnAgregarProductos);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("✏️ Editar Pedido");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = AppDatabase.getInstance(this);
        pedidoRepository = new PedidoRepository(this); // INICIALIZAMOS EL REPOSITORIO
        carrito = new ArrayList<>();

        cargarPedido();
        cargarProductos();

        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        productoAdapter = new ProductoSelectorAdapter(listaProductos, this::agregarAlCarrito);
        rvProductos.setAdapter(productoAdapter);

        btnAgregarProductos.setOnClickListener(v -> toggleProductos());
        btnGuardarCambios.setOnClickListener(v -> guardarCambios());
    }

    private void cargarPedido() {
        pedidoActual = db.pedidoDao().getPedidoById(pedidoId);
        if (pedidoActual == null) {
            Toast.makeText(this, "Pedido no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etNotas.setText(pedidoActual.getNotas());

        List<DetallePedido> detalles = db.detallePedidoDao().getDetallesByPedido(pedidoId);
        for (DetallePedido detalle : detalles) {
            carrito.add(new ItemCarrito(
                    detalle.getProductoId(),
                    detalle.getProductoNombre(),
                    detalle.getCantidad(),
                    detalle.getPrecioUnitario()
            ));
        }
        actualizarCarrito();
    }

    private void cargarProductos() {
        listaProductos = db.productoDao().getAllProductos();
        if (listaProductos == null || listaProductos.isEmpty()) {
            listaProductos = new ArrayList<>();
        }
    }

    private void agregarAlCarrito(Producto producto) {
        for (ItemCarrito item : carrito) {
            if (item.getProductoId() == producto.getId()) {
                item.setCantidad(item.getCantidad() + 1);
                actualizarCarrito();
                Toast.makeText(this, "✅ Cantidad actualizada: " + producto.getNombre(), Toast.LENGTH_SHORT).show();
                return;
            }
        }
        carrito.add(new ItemCarrito(producto.getId(), producto.getNombre(), 1, producto.getPrecio()));
        actualizarCarrito();
        Toast.makeText(this, "✅ Agregado: " + producto.getNombre(), Toast.LENGTH_SHORT).show();
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

            View itemView = getLayoutInflater().inflate(R.layout.item_carrito_editable, null);

            TextView tvNombre = itemView.findViewById(R.id.tvNombreCarrito);
            TextView tvCantidad = itemView.findViewById(R.id.tvCantidadCarrito);
            TextView tvPrecio = itemView.findViewById(R.id.tvPrecioCarrito);
            Button btnMas = itemView.findViewById(R.id.btnMas);
            Button btnMenos = itemView.findViewById(R.id.btnMenos);
            Button btnEliminar = itemView.findViewById(R.id.btnEliminar);

            tvNombre.setText(item.getNombre());
            tvCantidad.setText(String.valueOf(item.getCantidad()));
            tvPrecio.setText("$" + String.format("%.2f", item.getSubtotal()));

            btnMas.setOnClickListener(v -> {
                item.setCantidad(item.getCantidad() + 1);
                actualizarCarrito();
            });

            btnMenos.setOnClickListener(v -> {
                if (item.getCantidad() > 1) {
                    item.setCantidad(item.getCantidad() - 1);
                    actualizarCarrito();
                }
            });

            btnEliminar.setOnClickListener(v -> {
                carrito.remove(item);
                actualizarCarrito();
            });

            layoutCarrito.addView(itemView);
        }

        double impuesto = subtotal * 0.13;
        double total = subtotal + impuesto;

        tvSubtotal.setText("$" + String.format("%.2f", subtotal));
        tvImpuesto.setText("$" + String.format("%.2f", impuesto));
        tvTotal.setText("$" + String.format("%.2f", total));
    }

    private void toggleProductos() {
        if (rvProductos.getVisibility() == View.VISIBLE) {
            rvProductos.setVisibility(View.GONE);
            btnAgregarProductos.setText("➕ Agregar Productos");
        } else {
            rvProductos.setVisibility(View.VISIBLE);
            btnAgregarProductos.setText("📋 Ocultar Productos");
        }
    }

    private void guardarCambios() {
        if (carrito.isEmpty()) {
            Toast.makeText(this, "❌ El pedido no puede estar vacío", Toast.LENGTH_SHORT).show();
            return;
        }

        double subtotal = 0;
        for (ItemCarrito item : carrito) {
            subtotal += item.getSubtotal();
        }
        double impuesto = subtotal * 0.13;
        double total = subtotal + impuesto;

        // 1. Actualizamos los datos del pedido en el objeto
        pedidoActual.setSubtotal(subtotal);
        pedidoActual.setImpuesto(impuesto);
        pedidoActual.setTotal(total);
        pedidoActual.setNotas(etNotas.getText().toString().trim());

        // 2. Guardamos en Room (Base de datos local)
        db.pedidoDao().update(pedidoActual);
        db.detallePedidoDao().deleteDetallesByPedido(pedidoId);

        List<DetallePedido> nuevaListaDetalles = new ArrayList<>();
        for (ItemCarrito item : carrito) {
            DetallePedido detalle = new DetallePedido(
                    pedidoId,
                    item.getProductoId(),
                    item.getNombre(),
                    item.getCantidad(),
                    item.getPrecioUnitario(),
                    ""
            );
            db.detallePedidoDao().insert(detalle);
            nuevaListaDetalles.add(detalle);
        }

        pedidoActual.setDetalles(nuevaListaDetalles);

        // 3. ¡AQUÍ ESTÁ LA MAGIA! Actualizamos en Firestore (Nube)
        pedidoRepository.actualizarPedidoEnFirestore(pedidoActual);

        Toast.makeText(this, "✅ Pedido actualizado localmente", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}