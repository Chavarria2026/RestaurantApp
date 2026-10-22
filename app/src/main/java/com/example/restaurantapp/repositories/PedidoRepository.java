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
            subirPedidoAFirestore(pedido, false);
        } else {
            Toast.makeText(context, "📶 Guardado localmente (Pendiente de sincronizar)", Toast.LENGTH_SHORT).show();
        }
    }

    private void subirPedidoAFirestore(Pedido pedido, boolean esSincronizacionMasiva) {
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

        // CONVERTIR LA LISTA DE PRODUCTOS A UNA LISTA DE MAPAS
        List<Map<String, Object>> listaProductosMap = new ArrayList<>();
        List<DetallePedido> detalles = pedido.getDetalles();

        // Si la lista viene vacía (por ejemplo, en la sincronización), la buscamos en Room
        if (detalles == null || detalles.isEmpty()) {
            detalles = dbLocal.detallePedidoDao().getDetallesByPedido(pedido.getId());
        }

        // Imprimimos en Logcat para depurar
        Log.d("FIREBASE_DEBUG", "Cantidad de productos a subir: " + (detalles != null ? detalles.size() : "null"));

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

        // Asignamos la lista al documento principal
        pedidoMap.put("productos", listaProductosMap);

        dbFirestore.collection("pedidos").add(pedidoMap)
                .addOnSuccessListener(documentReference -> {
                    String firebaseId = documentReference.getId();
                    dbLocal.pedidoDao().marcarPedidoComoSincronizado(pedido.getId(), firebaseId);
                    if (!esSincronizacionMasiva) {
                        Toast.makeText(context, "✅ Pedido guardado en Nube y Local", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (!esSincronizacionMasiva) {
                        Toast.makeText(context, "⚠️ Error en nube. Guardado solo en local.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void sincronizarPendientes(OnSyncCompleteListener listener) {
        List<Pedido> pendientes = dbLocal.pedidoDao().getPedidosNoSincronizados();

        if (pendientes.isEmpty()) {
            listener.onSyncFinished(0);
            return;
        }

        for (Pedido pedido : pendientes) {
            subirPedidoAFirestore(pedido, true);
        }
        listener.onSyncFinished(pendientes.size());
    }

    public interface OnSyncCompleteListener {
        void onSyncFinished(int cantidadSincronizada);
    }
}