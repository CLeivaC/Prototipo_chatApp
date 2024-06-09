package com.leiva.prototipo_chatapp.Data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
/*
 * Definición de entidad Imagen en SQlite
 * El "id_mensaje" es clave foránea de la entidad.
 */
@Entity(
    tableName = "imagenes",
    foreignKeys = [
        ForeignKey(
            entity = MensajeEntity::class,
            parentColumns = ["id_mensaje"],
            childColumns = ["id_mensaje"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["id_mensaje"])]
)
data class ImagenEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val id_mensaje: String,
    val url: String
)

