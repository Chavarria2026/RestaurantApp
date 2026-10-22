package com.example.restaurantapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.restaurantapp.models.Pedido;
import java.util.Date;
import java.util.List;

@Dao
public interface PedidoDao {

    @Insert
    long insert(Pedido pedido);

    @Update
    int update(Pedido pedido);

    @Delete
    int delete(Pedido pedido);

    @Query("SELECT * FROM pedidos ORDER BY id DESC")
    List<Pedido> getAllPedidos();

    @Query("SELECT * FROM pedidos WHERE id = :id")
    Pedido getPedidoById(int id);

    @Query("SELECT * FROM pedidos WHERE estado = :estado ORDER BY id DESC")
    List<Pedido> getPedidosByEstado(String estado);

    @Query("SELECT * FROM pedidos WHERE usuario_id = :usuarioId ORDER BY id DESC")
    List<Pedido> getPedidosByUsuario(int usuarioId);

    @Query("SELECT * FROM pedidos WHERE numero_mesa = :mesaId ORDER BY id DESC")
    List<Pedido> getPedidosByMesa(int mesaId);

    @Query("SELECT * FROM pedidos WHERE fecha BETWEEN :inicio AND :fin ORDER BY fecha DESC")
    List<Pedido> getPedidosByFecha(Date inicio, Date fin);

    @Query("SELECT * FROM pedidos WHERE date(fecha) = date('now') ORDER BY id DESC")
    List<Pedido> getPedidosDelDia();

    @Query("SELECT SUM(total) FROM pedidos WHERE estado = 'pagado' AND date(fecha) = date('now')")
    Double getTotalVentasDelDia();

    @Query("SELECT COUNT(*) FROM pedidos WHERE date(fecha) = date('now')")
    int getCantidadPedidosDelDia();

    @Query("SELECT SUM(total) FROM pedidos WHERE estado = :estado")
    Double getTotalVentasByEstado(String estado);

    @Query("UPDATE pedidos SET estado = :estado WHERE id = :id")
    void actualizarEstado(int id, String estado);

    // NUEVOS MÉTODOS PARA SINCRONIZACIÓN
    @Query("SELECT * FROM pedidos WHERE sincronizado = 0")
    List<Pedido> getPedidosNoSincronizados();

    @Query("UPDATE pedidos SET sincronizado = 1, firebase_id = :firebaseId WHERE id = :pedidoId")
    void marcarPedidoComoSincronizado(int pedidoId, String firebaseId);

    @Query("DELETE FROM pedidos")
    void deleteAll();
}