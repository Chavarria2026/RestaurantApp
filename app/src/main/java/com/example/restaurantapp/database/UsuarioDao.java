package com.example.restaurantapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.restaurantapp.models.Usuario;
import java.util.List;

@Dao
public interface UsuarioDao {

    @Insert
    long insert(Usuario usuario);

    @Insert
    List<Long> insertAll(Usuario... usuarios);

    @Update
    int update(Usuario usuario);

    @Delete
    int delete(Usuario usuario);

    @Query("SELECT * FROM usuarios ORDER BY nombre ASC")
    List<Usuario> getAllUsuarios();

    @Query("SELECT * FROM usuarios WHERE id = :id")
    Usuario getUsuarioById(int id);

    @Query("SELECT * FROM usuarios WHERE nombre = :nombre")
    Usuario getUsuarioByNombre(String nombre);

    @Query("SELECT * FROM usuarios WHERE email = :email")
    Usuario getUsuarioByEmail(String email);

    @Query("SELECT * FROM usuarios WHERE email = :email AND password = :password")
    Usuario login(String email, String password); // Cambiado de nombre a email para que coincida con LoginActivity

    @Query("SELECT * FROM usuarios WHERE rol = :rol")
    List<Usuario> getUsuariosByRol(String rol);

    @Query("SELECT COUNT(*) > 0 FROM usuarios WHERE nombre = :nombre")
    boolean existeUsuario(String nombre);

    // NUEVAS CONSULTAS PARA SINCRONIZACIÓN (FASE 3)
    @Query("SELECT * FROM usuarios WHERE sincronizado = 0")
    List<Usuario> getUsuariosNoSincronizados();

    @Query("UPDATE usuarios SET sincronizado = 1 WHERE email = :email")
    void marcarComoSincronizado(String email);

    @Query("DELETE FROM usuarios")
    void deleteAll();
}