package com.example.restaurantapp.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.example.restaurantapp.R;
import com.example.restaurantapp.adapters.ProductoAdminAdapter;
import com.example.restaurantapp.database.AppDatabase;
import com.example.restaurantapp.models.Producto;
import java.util.ArrayList;
import java.util.List;

public class MenuFragment extends Fragment implements ProductoAdminAdapter.OnProductoChangedListener {

    private RecyclerView rvProductos;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etBuscarProducto;
    private ImageView ivLimpiarBusqueda;
    private Button btnAgregarProducto;
    private ProductoAdminAdapter adapter;
    private AppDatabase db;
    private List<Producto> listaOriginal;
    private List<Producto> listaFiltrada;

    // Variables para la galería
    private String imagenUriString = null;
    private ImageView ivImagenPreview;
    private ActivityResultLauncher<Intent> galeriaLauncher;

    // Para manejar la edición desde el adapter
    private ImageView activeImageView;
    private ProductoAdminAdapter.ImageCallback activeCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);

        rvProductos = view.findViewById(R.id.rvProductos);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        etBuscarProducto = view.findViewById(R.id.etBuscarProducto);
        ivLimpiarBusqueda = view.findViewById(R.id.ivLimpiarBusqueda);
        btnAgregarProducto = view.findViewById(R.id.btnAgregarProducto);

        etBuscarProducto.setTextColor(0xFF000000);
        rvProductos.setLayoutManager(new LinearLayoutManager(getContext()));
        db = AppDatabase.getInstance(requireContext());

        // Configurar la galería
        galeriaLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    requireContext().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    if (activeImageView != null) {
                        Glide.with(requireContext()).load(uri).into(activeImageView);
                    }
                    if (activeCallback != null) {
                        activeCallback.onImageSelected(uri.toString());
                    }
                }
            }
        });

        cargarProductos();

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

        swipeRefreshLayout.setOnRefreshListener(() -> {
            cargarProductos();
            new Handler(Looper.getMainLooper()).postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 1000);
        });

        btnAgregarProducto.setOnClickListener(v -> mostrarDialogoAgregar());

        return view;
    }

    @Override
    public void onRequestImage(ImageView imageView, ProductoAdminAdapter.ImageCallback callback) {
        this.activeImageView = imageView;
        this.activeCallback = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        galeriaLauncher.launch(intent);
    }

    private void cargarProductos() {
        listaOriginal = db.productoDao().getAllProductos();
        if (listaOriginal == null || listaOriginal.isEmpty()) {
            insertarProductosPorDefecto();
            listaOriginal = db.productoDao().getAllProductos();
        }
        listaFiltrada = new ArrayList<>(listaOriginal);
        adapter = new ProductoAdminAdapter(listaFiltrada, db, this, getContext());
        rvProductos.setAdapter(adapter);
    }

    private void insertarProductosPorDefecto() {
        String[][] productos = {
                {"🍔 Hamburguesa", "Hamburguesa con queso, lechuga y tomate", "8.99"},
                {"🍕 Pizza Pepperoni", "Pizza pepperoni con queso mozzarella", "12.99"},
                {"🍟 Papas fritas", "Papas crujientes", "3.50"},
                {"🥤 Refresco Cola", "Refresco de cola bien frío", "2.50"},
                {"🌮 Tacos al Pastor", "Tacos al pastor con piña y cilantro", "6.99"},
                {"🍰 Pastel Chocolate", "Pastel de chocolate con crema", "4.99"}
        };

        for (String[] p : productos) {
            Producto producto = new Producto();
            producto.setNombre(p[0]);
            producto.setDescripcion(p[1]);
            producto.setPrecio(Double.parseDouble(p[2]));
            producto.setCategoria("General");
            producto.setDisponible(true);
            db.productoDao().insert(producto);
        }
    }

    private void filtrarProductos(String texto) {
        listaFiltrada.clear();
        if (texto == null || texto.trim().isEmpty()) {
            listaFiltrada.addAll(listaOriginal);
        } else {
            String busqueda = texto.toLowerCase().trim();
            for (Producto producto : listaOriginal) {
                if (producto.getNombre().toLowerCase().contains(busqueda)) {
                    listaFiltrada.add(producto);
                }
            }
        }
        adapter.actualizarLista(listaFiltrada);
    }

    private void mostrarDialogoAgregar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_producto, null);

        EditText etNombre = view.findViewById(R.id.etNombre);
        EditText etDescripcion = view.findViewById(R.id.etDescripcion);
        EditText etPrecio = view.findViewById(R.id.etPrecio);
        ivImagenPreview = view.findViewById(R.id.ivImagenProducto);
        Button btnSeleccionarImagen = view.findViewById(R.id.btnSeleccionarImagen);

        imagenUriString = null;

        btnSeleccionarImagen.setOnClickListener(v -> {
            activeImageView = ivImagenPreview;
            activeCallback = uri -> imagenUriString = uri;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            galeriaLauncher.launch(intent);
        });

        builder.setTitle("➕ Agregar Producto")
                .setView(view)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombre = etNombre.getText().toString().trim();
                    String descripcion = etDescripcion.getText().toString().trim();
                    String precioStr = etPrecio.getText().toString().trim();

                    if (nombre.isEmpty() || precioStr.isEmpty()) {
                        Toast.makeText(getContext(), "Ingrese nombre y precio", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double precio = Double.parseDouble(precioStr);
                    Producto producto = new Producto();
                    producto.setNombre(nombre);
                    producto.setDescripcion(descripcion);
                    producto.setPrecio(precio);
                    producto.setCategoria("General");
                    producto.setDisponible(true);
                    producto.setImagenUrl(imagenUriString);

                    db.productoDao().insert(producto);
                    Toast.makeText(getContext(), "✅ Producto agregado", Toast.LENGTH_SHORT).show();
                    cargarProductos();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onProductoChanged() {
        cargarProductos();
    }
}