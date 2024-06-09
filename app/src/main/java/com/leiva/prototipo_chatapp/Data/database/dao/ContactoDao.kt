package com.leiva.prototipo_chatapp.Data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.leiva.prototipo_chatapp.Data.database.entities.ContactoEntity
import com.leiva.prototipo_chatapp.Data.database.entities.UsuarioData

@Dao
interface ContactoDao {

    //Insertar contactos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarContactos(contactos: List<ContactoEntity>)

    // Consulta para obtener todos los contactos del usuario actual
    @Query("SELECT * FROM contactos WHERE ownerUid = :ownerUid")
    suspend fun getAllContactos(ownerUid: String): List<ContactoEntity>

    //Obtener contacto por número de teléfono
    @Query("SELECT * FROM contactos WHERE telefono = :telefono")
    suspend fun getContactoByTelefono(telefono: String): ContactoEntity?

    //Actualizar contacto
    @Update
    suspend fun actualizarContacto(contacto: ContactoEntity)

    //Actualizar infoAdicional de contacto
    @Query("UPDATE contactos SET infoAdicional = :infoAdicional WHERE uid = :uid")
    suspend fun actualizarInfoAdicional(uid: String, infoAdicional: String?)

    //Eliminar lista de contactos
    @Delete
    suspend fun eliminarContactos(contactos: List<ContactoEntity>)

    //Obtener contacto por "id"
    @Query("SELECT * FROM contactos WHERE uid = :uid AND ownerUid = :ownerUid")
    suspend fun getUserById(uid: String, ownerUid: String): ContactoEntity?

    //Obtener contacto actual
    @Query("SELECT * FROM contactos WHERE uid = :uid")
    suspend fun getCurrentUser(uid: String): ContactoEntity?

    //Insertar contacto
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarContacto(contacto: ContactoEntity)

    //Obtener contactos con conversación
    @Query("SELECT * FROM contactos WHERE haTenidoConversacion = 1 AND ownerUid = :ownerUid ORDER BY ultimoMensajeTimestamp DESC")
    suspend fun getContactosConConversacion(ownerUid: String): List<ContactoEntity>

    //Eliminar todos los contactos
    @Query("DELETE FROM contactos")
    suspend fun borrarTodosLosContactos()

    //Actualizar el estado del contacto (online||offline)
    @Query("UPDATE contactos SET oculto = :oculto WHERE uid = :uid")
    suspend fun actualizarEstadoOculto(uid: String, oculto: Boolean)

}


