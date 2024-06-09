package com.leiva.prototipo_chatapp.Data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioData(
    @PrimaryKey
    val id: String,
    val nombre: String?,
    val apellido: String?,
    val telefono: String?,
    val infoAdicional: String?,
    val imagen: String?,
    val fondoPerfilUrl: String?,
)