package com.leiva.prototipo_chatapp.Data.database.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.leiva.prototipo_chatapp.Data.database.entities.UsuarioData

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insertarUsuario(usuario: UsuarioData): Long

    @Update
    suspend fun actualizarUsuario(usuario: UsuarioData)

    @Query("SELECT * FROM usuarios WHERE id = :userId")
    suspend fun obtenerUsuario(userId: String): UsuarioData? // Cambiar el tipo de dato de Long a String

    @Query("SELECT * FROM usuarios")
    suspend fun obtenerTodosUsuarios(): List<UsuarioData>

    @Query("DELETE FROM usuarios")
    suspend fun borrarUsuarios()
}