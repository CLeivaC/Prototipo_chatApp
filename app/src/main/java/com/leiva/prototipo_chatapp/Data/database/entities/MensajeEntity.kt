package com.leiva.prototipo_chatapp.Data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * Definición de entidad Mensaje en SQlite
 */

@Entity(tableName = "mensajes")
data class MensajeEntity(
    @PrimaryKey val id_mensaje:String,
    val emisorId: String,
    val receptorId: String,
    val mensaje: String,
    val url:String?,
    var visto:Boolean = false,
    val hora: Long,
)