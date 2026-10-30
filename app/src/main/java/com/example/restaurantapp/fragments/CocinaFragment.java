package com.example.restaurantapp.fragments;

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
import com.example.restaurantapp.R;
import com.example.restaurantapp.database.AppDatabase;
import com.example.restaurantapp.models.DetallePedido;
import com.example.restaurantapp.models.Pedido;
import com.example.restaurantapp.repositories.PedidoRepository;
import java.util.ArrayList;
import java.util.List;

public class CocinaFragment extends Fragment {

    private LinearLayout layoutCocina;
    private AppDatabase db;
    private PedidoRepository pedidoRepository; // NUEVO

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cocina, container, false);
        layoutCocina = view.findViewById(R.id.layoutCocina);

        db = AppDatabase.getInstance(requireContext());
        pedidoRepository = new PedidoRepository(requireContext()); // NUEVO

        cargarPedidosCocina();
        return view;
    }

    private void cargarPedidosCocina() {
        layoutCocina.removeAllViews();
        List<Pedido> pendientes = db.pedidoDao().getPedidosByEstado("pendiente");
        List<Pedido> preparando = db.pedidoDao().getPedidosByEstado("preparacion");

        List<Pedido> todos = new ArrayList<>();
        if (pendientes != null) todos.addAll(pendientes);
        if (preparando != null) todos.addAll(preparando);

        if (todos.isEmpty()) {
            TextView tv = new TextView(getContext());
            tv.setText("✅ No hay pedidos pendientes en cocina");
            tv.setTextSize(16);
            tv.setPadding(20, 50, 20, 20);
            layoutCocina.addView(tv);
            return;
        }

        for (Pedido p : todos) {
            layoutCocina.addView(crearCardCocina(p));
        }
    }

    private LinearLayout crearCardCocina(Pedido pedido) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setElevation(4);

        TextView tvMesa = new TextView(getContext());
        tvMesa.setText("MESA: " + pedido.getNumeroMesa() + " | Pedido #" + pedido.getId());
        tvMesa.setTextSize(18);
        tvMesa.setTypeface(null, android.graphics.Typeface.BOLD);
        tvMesa.setTextColor(0xFF002366);

        TextView tvEstado = new TextView(getContext());
        tvEstado.setText("Estado: " + pedido.getEstado().toUpperCase());
        tvEstado.setTextColor(0xFF9C27B0);

        card.addView(tvMesa);
        card.addView(tvEstado);

        List<DetallePedido> detalles = db.detallePedidoDao().getDetallesByPedido(pedido.getId());
        for (DetallePedido d : detalles) {
            TextView tvProd = new TextView(getContext());
            tvProd.setText("• " + d.getCantidad() + "x " + d.getProductoNombre());
            tvProd.setTextSize(16);
            tvProd.setPadding(0, 8, 0, 0);
            card.addView(tvProd);
        }

        if (pedido.getNotas() != null && !pedido.getNotas().isEmpty()) {
            TextView tvNotas = new TextView(getContext());
            tvNotas.setText("📝 Notas: " + pedido.getNotas());
            tvNotas.setTextColor(0xFFF44336);
            card.addView(tvNotas);
        }

        Button btnListo = new Button(getContext());
        btnListo.setText("🔔 MARCAR COMO LISTO");
        btnListo.setBackgroundColor(0xFF2196F3);
        btnListo.setTextColor(0xFFFFFFFF);
        btnListo.setOnClickListener(v -> {
            pedido.setEstado("listo");
            db.pedidoDao().update(pedido); // 1. Guarda en local

            pedidoRepository.actualizarEstadoEnFirestore(pedido); // 2. Actualiza en la nube (NUEVO)

            Toast.makeText(getContext(), "Pedido marcado como Listo", Toast.LENGTH_SHORT).show();
            cargarPedidosCocina();
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 16, 0, 0);
        btnListo.setLayoutParams(btnParams);
        card.addView(btnListo);

        return card;
    }
}