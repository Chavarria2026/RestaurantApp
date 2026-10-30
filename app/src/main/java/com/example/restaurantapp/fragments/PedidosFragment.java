package com.example.restaurantapp.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.restaurantapp.R;
import com.example.restaurantapp.activities.EditarPedidoActivity;
import com.example.restaurantapp.database.AppDatabase;
import com.example.restaurantapp.models.Pedido;
import com.example.restaurantapp.repositories.PedidoRepository;
import java.util.List;

public class PedidosFragment extends Fragment {

    private LinearLayout layoutPedidos;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AppDatabase db;
    private PedidoRepository pedidoRepository; // NUEVO

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pedidos, container, false);
        layoutPedidos = view.findViewById(R.id.layoutPedidos);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        db = AppDatabase.getInstance(requireContext());
        pedidoRepository = new PedidoRepository(requireContext()); // NUEVO

        cargarPedidos();

        swipeRefreshLayout.setOnRefreshListener(() -> {
            cargarPedidos();
            swipeRefreshLayout.setRefreshing(false);
        });

        return view;
    }

    private void cargarPedidos() {
        layoutPedidos.removeAllViews();
        List<Pedido> pedidos = db.pedidoDao().getAllPedidos();

        if (pedidos == null || pedidos.isEmpty()) {
            TextView tvVacio = new TextView(getContext());
            tvVacio.setText("📭 No hay pedidos registrados");
            tvVacio.setTextSize(16);
            tvVacio.setTextColor(0xFF999999);
            tvVacio.setPadding(20, 50, 20, 20);
            layoutPedidos.addView(tvVacio);
            return;
        }

        for (Pedido pedido : pedidos) {
            LinearLayout card = crearCardPedido(pedido);
            layoutPedidos.addView(card);
        }
    }

    private LinearLayout crearCardPedido(Pedido pedido) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 15, 20, 15);
        card.setBackgroundColor(0xFFF5F5F5);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 15);
        card.setLayoutParams(params);

        TextView tvId = new TextView(getContext());
        tvId.setText("🆔 Pedido #" + pedido.getId() + " | 🍽️ Mesa: " + pedido.getNumeroMesa());
        tvId.setTextSize(16);
        tvId.setTextColor(0xFF333333);
        tvId.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvTotal = new TextView(getContext());
        tvTotal.setText("💰 Total: $" + String.format("%.2f", pedido.getTotal()));
        tvTotal.setTextColor(0xFF4CAF50);

        TextView tvEstado = new TextView(getContext());
        String estado = pedido.getEstado();
        tvEstado.setText("📌 Estado: " + getEstadoTexto(estado));
        tvEstado.setTextColor(getEstadoColor(estado));
        tvEstado.setPadding(0, 5, 0, 10);

        LinearLayout layoutBotones = new LinearLayout(getContext());
        layoutBotones.setOrientation(LinearLayout.HORIZONTAL);

        if (estado.equals("pendiente")) {
            Button btnEditar = new Button(getContext());
            btnEditar.setText("✏️ Editar");
            btnEditar.setBackgroundColor(0xFF2196F3);
            btnEditar.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            p.setMargins(0, 0, 8, 0);
            btnEditar.setLayoutParams(p);
            btnEditar.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), EditarPedidoActivity.class);
                intent.putExtra("pedido_id", pedido.getId());
                startActivity(intent);
            });
            layoutBotones.addView(btnEditar);

            Button btnCancelar = new Button(getContext());
            btnCancelar.setText("❌ Cancelar");
            btnCancelar.setBackgroundColor(0xFFF44336);
            btnCancelar.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            btnCancelar.setLayoutParams(p2);
            btnCancelar.setOnClickListener(v -> confirmarCancelar(pedido));
            layoutBotones.addView(btnCancelar);
        }

        if (estado.equals("pendiente")) {
            Button btnCocina = new Button(getContext());
            btnCocina.setText("👨‍🍳 A Cocina");
            btnCocina.setBackgroundColor(0xFFFF9800);
            btnCocina.setTextColor(0xFFFFFFFF);
            btnCocina.setOnClickListener(v -> cambiarEstado(pedido, "preparacion"));
            layoutBotones.addView(btnCocina);
        }

        if (estado.equals("listo")) {
            Button btnPagar = new Button(getContext());
            btnPagar.setText("💰 Pagar");
            btnPagar.setBackgroundColor(0xFF4CAF50);
            btnPagar.setTextColor(0xFFFFFFFF);
            btnPagar.setOnClickListener(v -> confirmarPagar(pedido));
            layoutBotones.addView(btnPagar);
        }

        card.addView(tvId);
        card.addView(tvTotal);
        card.addView(tvEstado);
        if (layoutBotones.getChildCount() > 0) {
            card.addView(layoutBotones);
        }

        return card;
    }

    private String getEstadoTexto(String estado) {
        switch (estado) {
            case "pendiente": return "🟡 Pendiente";
            case "preparacion": return "👨‍🍳 En Preparación";
            case "listo": return "🔔 Listo";
            case "pagado": return "🟢 Pagado";
            case "cancelado": return "🔴 Cancelado";
            default: return estado;
        }
    }

    private int getEstadoColor(String estado) {
        switch (estado) {
            case "pendiente": return 0xFFFF9800;
            case "preparacion": return 0xFF9C27B0;
            case "listo": return 0xFF2196F3;
            case "pagado": return 0xFF4CAF50;
            case "cancelado": return 0xFFF44336;
            default: return 0xFF666666;
        }
    }

    private void cambiarEstado(Pedido pedido, String nuevoEstado) {
        pedido.setEstado(nuevoEstado);
        db.pedidoDao().update(pedido); // 1. Guarda en local

        pedidoRepository.actualizarEstadoEnFirestore(pedido); // 2. Actualiza en la nube (NUEVO)

        Toast.makeText(getContext(), "Pedido #" + pedido.getId() + " actualizado", Toast.LENGTH_SHORT).show();
        cargarPedidos();
    }

    private void confirmarCancelar(Pedido pedido) {
        new AlertDialog.Builder(getContext())
                .setTitle("Cancelar Pedido")
                .setMessage("¿Estás seguro de cancelar el Pedido #" + pedido.getId() + "?")
                .setPositiveButton("Sí", (dialog, which) -> cambiarEstado(pedido, "cancelado"))
                .setNegativeButton("No", null).show();
    }

    private void confirmarPagar(Pedido pedido) {
        new AlertDialog.Builder(getContext())
                .setTitle("Pagar Pedido")
                .setMessage("¿Registrar pago del Pedido #" + pedido.getId() + "?\nTotal: $" + String.format("%.2f", pedido.getTotal()))
                .setPositiveButton("Sí, pagar", (dialog, which) -> cambiarEstado(pedido, "pagado"))
                .setNegativeButton("Cancelar", null).show();
    }
}