package com.example.restaurantapp.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.example.restaurantapp.models.DetallePedido;
import com.example.restaurantapp.models.Pedido;
import com.example.restaurantapp.models.Producto;
import com.example.restaurantapp.models.Usuario;

@Database(
        entities = {Usuario.class, Producto.class, Pedido.class, DetallePedido.class},
        version = 5, // <--- AQUÍ ESTÁ EL CAMBIO (antes era 2)
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract UsuarioDao usuarioDao();
    public abstract ProductoDao productoDao();
    public abstract PedidoDao pedidoDao();
    public abstract DetallePedidoDao detallePedidoDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "restaurant_database"
                    ).fallbackToDestructiveMigration() // Esto borrará los datos viejos y creará la tabla nueva
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}