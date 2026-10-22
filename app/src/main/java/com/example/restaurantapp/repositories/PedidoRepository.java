package com.example.restaurantapp.repositories;

import android.content.Context;
import android.widget.Toast;
import com.example.restaurantapp.database.AppDatabase;
import com.example.restaurantapp.models.DetallePedido;
import com.example.restaurantapp.models.Pedido;
import com.example.restaurantapp.utils.NetworkUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PedidoRepository {

    private AppDatabase dbLocal;
    private FirebaseFirestore dbFirestore;
    private Context context;

    public PedidoRepository(Context context) {
        this.context = context;
        this.dbLocal = AppDatabase.getInstance(context);
        this.dbFirestore = FirebaseFirestore.getInstance();
    }

    public void insertarPedido(Pedido pedido) {
        long idLocal = dbLocal.pedidoDao().insert(pedido);
        pedido.setId((int) idLocal);

        if (pedido.getDetalles() != null) {
            for (DetallePedido detalle : pedido.getDetalles()) {
                detalle.setPedidoId((int) idLocal);
                dbLocal.detallePedidoDao().insert(detalle);
            }
        }

        if (NetworkUtils.isNetworkAvailable(context)) {
            subirPedidoAFirestore(pedido, false, null); // null porque no necesita avisar a nadie
        } else {
            Toast.makeText(context, "📶 Guardado localmente (Pendiente de sincronizar)", Toast.LENGTH_SHORT).show();
        }
    }

    private void subirPedidoAFirestore(Pedido pedido, boolean esSincronizacionMasiva, Runnable onComplete) {
        Map<String, Object> pedidoMap = new HashMap<>();
        pedidoMap.put("numeroMesa", pedido.getNumeroMesa());

        long fechaMillis = (pedido.getFecha() != null) ? pedido.getFecha().getTime() : System.currentTimeMillis();
        pedidoMap.put("fecha", fechaMillis);

        pedidoMap.put("subtotal", pedido.getSubtotal());
        pedidoMap.put("impuesto", pedido.getImpuesto());
        pedidoMap.put("total", pedido.getTotal());
        pedidoMap.put("estado", pedido.getEstado());
        pedidoMap.put("usuarioId", pedido.getUsuarioId());
        pedidoMap.put("notas", pedido.getNotas());

        List<Map<String, Object>> listaProductosMap = new ArrayList<>();
        List<DetallePedido> detalles = pedido.getDetalles();

        if (detalles == null || detalles.isEmpty()) {
            detalles = dbLocal.detallePedidoDao().getDetallesByPedido(pedido.getId());
        }

        if (detalles != null && !detalles.isEmpty()) {
            for (DetallePedido detalle : detalles) {
                Map<String, Object> detMap = new HashMap<>();
                detMap.put("productoId", detalle.getProductoId());
                detMap.put("productoNombre", detalle.getProductoNombre());
                detMap.put("cantidad", detalle.getCantidad());
                detMap.put("precioUnitario", detalle.getPrecioUnitario());
                detMap.put("subtotal", detalle.getSubtotal());
                listaProductosMap.add(detMap);
            }
        }
        pedidoMap.put("productos", listaProductosMap);

        dbFirestore.collection("pedidos").add(pedidoMap)
                .addOnSuccessListener(documentReference -> {
                    String firebaseId = documentReference.getId();
                    dbLocal.pedidoDao().marcarPedidoComoSincronizado(pedido.getId(), firebaseId);
                    if (!esSincronizacionMasiva) {
                        Toast.makeText(context, "✅ Pedido guardado en Nube y Local", Toast.LENGTH_SHORT).show();
                    }
                    if (onComplete != null) onComplete.run(); // Avisamos que terminó
                })
                .addOnFailureListener(e -> {
                    if (!esSincronizacionMasiva) {
                        Toast.makeText(context, "⚠️ Error en nube. Guardado solo en local.", Toast.LENGTH_SHORT).show();
                    }
                    if (onComplete != null) onComplete.run(); // Avisamos que terminó (aunque haya fallado)
                });
    }

    public void sincronizarPendientes(OnSyncCompleteListener listener) {
        List<Pedido> pendientes = dbLocal.pedidoDao().getPedidosNoSincronizados();

        if (pendientes.isEmpty()) {
            listener.onSyncFinished(0);
            return;
        }

        int totalPendientes = pendientes.size();
        final int[] completados = {0}; // Contador para saber cuántos han terminado

        for (Pedido pedido : pendientes) {
            subirPedidoAFirestore(pedido, true, () -> {
                completados[0]++;
                // Cuando el último pedido termine de subir, avisamos a la interfaz
                if (completados[0] == totalPendientes) {
                    listener.onSyncFinished(totalPendientes);
                }
            });
        }
    }

    public interface OnSyncCompleteListener {
        void onSyncFinished(int cantidadSincronizada);
    }
}