package com.leiva.prototipo_chatapp.Data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.leiva.prototipo_chatapp.Data.database.entities.MensajeEntity

@Dao
interface MensajeDao {
    //Insertar mensaje
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMensaje(mensaje: MensajeEntity)

    //Obtener mensajes de conversación
    @Query("SELECT * FROM mensajes WHERE (emisorId = :emisorId AND receptorId = :receptorId) OR (receptorId = :emisorId AND emisorId = :receptorId)")
    suspend fun obtenerMensajesDeRoom(emisorId: String, receptorId: String): List<MensajeEntity>

    //Obtener último mensaje de conversación
    @Query("SELECT * FROM mensajes WHERE (emisorId = :emisorId AND receptorId = :receptorId) OR (emisorId = :receptorId AND receptorId = :emisorId) ORDER BY hora DESC LIMIT 1")
    suspend fun obtenerUltimoMensaje(emisorId: String, receptorId: String): MensajeEntity?

    //Eliminar mensaje por "id"
    @Query("DELETE FROM mensajes WHERE id_mensaje = :idMensaje")
    suspend fun borrarMensajePorId(idMensaje: String)


}
