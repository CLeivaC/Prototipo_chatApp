package com.leiva.prototipo_chatapp.Data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * Definición de entidad Contacto en SQlite
 */

@Entity(tableName = "contactos", primaryKeys = ["uid", "ownerUid"])
data class ContactoEntity(
    val uid: String,
    var n_usuario: String,
    val telefono: String,
    var imagen: String,
    var oculto: Boolean,
    var haTenidoConversacion: Boolean?,
    var infoAdicional: String?,
    var imagenFondoPerfil: String,
    val ultimoMensajeTimestamp: Long,
    val ownerUid: String // Añade esta línea
)