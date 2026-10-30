package com.example.restaurantapp.repositories;

import android.content.Context;
import android.util.Log;
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
            subirPedidoAFirestore(pedido, false, null);
        } else {
            Toast.makeText(context, "📶 Guardado localmente (Pendiente de sincronizar)", Toast.LENGTH_SHORT).show();
        }
    }

    private void subirPedidoAFirestore(Pedido pedido, boolean esSincronizacionMasiva, Runnable onComplete) {
        Map<String, Object> pedidoMap = crearMapaPedido(pedido);

        dbFirestore.collection("pedidos").add(pedidoMap)
                .addOnSuccessListener(documentReference -> {
                    String firebaseId = documentReference.getId();
                    dbLocal.pedidoDao().marcarPedidoComoSincronizado(pedido.getId(), firebaseId);
                    if (!esSincronizacionMasiva) {
                        Toast.makeText(context, "✅ Pedido guardado en Nube y Local", Toast.LENGTH_SHORT).show();
                    }
                    if (onComplete != null) onComplete.run();
                })
                .addOnFailureListener(e -> {
                    if (!esSincronizacionMasiva) {
                        Toast.makeText(context, "⚠️ Error en nube. Guardado solo en local.", Toast.LENGTH_SHORT).show();
                    }
                    if (onComplete != null) onComplete.run();
                });
    }

    // --- NUEVO MÉTODO PARA ACTUALIZAR EL ESTADO ---
    public void actualizarEstadoEnFirestore(Pedido pedido) {
        if (pedido.getFirebaseId() != null && !pedido.getFirebaseId().isEmpty()) {
            dbFirestore.collection("pedidos").document(pedido.getFirebaseId())
                    .update("estado", pedido.getEstado())
                    .addOnSuccessListener(aVoid -> Log.d("FIREBASE_SYNC", "Estado actualizado en la nube a: " + pedido.getEstado()))
                    .addOnFailureListener(e -> Log.e("FIREBASE_SYNC", "Error actualizando estado en la nube", e));
        }
    }

    // --- NUEVO MÉTODO PARA ACTUALIZAR EL PEDIDO COMPLETO (EDICIÓN) ---
    public void actualizarPedidoEnFirestore(Pedido pedido) {
        if (pedido.getFirebaseId() != null && !pedido.getFirebaseId().isEmpty()) {
            Map<String, Object> pedidoMap = crearMapaPedido(pedido);
            // Usamos set() para reemplazar los datos, ya que cambió el arreglo de productos
            dbFirestore.collection("pedidos").document(pedido.getFirebaseId())
                    .set(pedidoMap)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("FIREBASE_SYNC", "Pedido editado actualizado en Firestore");
                        Toast.makeText(context, "✅ Pedido actualizado en la Nube", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Log.e("FIREBASE_SYNC", "Error actualizando pedido en Firestore", e));
        } else {
            // Si no tiene firebaseId (se creó offline), lo subimos como nuevo
            subirPedidoAFirestore(pedido, false, null);
        }
    }

    private Map<String, Object> crearMapaPedido(Pedido pedido) {
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

        return pedidoMap;
    }

    public void sincronizarPendientes(OnSyncCompleteListener listener) {
        List<Pedido> pendientes = dbLocal.pedidoDao().getPedidosNoSincronizados();

        if (pendientes.isEmpty()) {
            listener.onSyncFinished(0);
            return;
        }

        int totalPendientes = pendientes.size();
        final int[] completados = {0};

        for (Pedido pedido : pendientes) {
            subirPedidoAFirestore(pedido, true, () -> {
                completados[0]++;
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