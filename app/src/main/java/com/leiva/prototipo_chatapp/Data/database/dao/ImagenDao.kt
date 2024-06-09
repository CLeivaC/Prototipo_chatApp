package com.leiva.prototipo_chatapp.Data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.leiva.prototipo_chatapp.Data.database.entities.ImagenEntity

@Dao
interface ImagenDao {

    //Insertar imagen
    @Insert
    suspend fun insertarImagen(imagen: ImagenEntity)

    //Obtener imágenes de conversación
    @Query("SELECT * FROM imagenes WHERE id_mensaje IN (SELECT id_mensaje FROM mensajes WHERE (emisorId = :emisorId AND receptorId = :receptorId) " +
            "OR (emisorId = :receptorId AND receptorId = :emisorId))")
    fun obtenerImagenesConversacion(emisorId: String, receptorId: String): List<ImagenEntity>

    //Borrar imágenes
    @Query("DELETE FROM imagenes")
    suspend fun limpiarImagenes()
}