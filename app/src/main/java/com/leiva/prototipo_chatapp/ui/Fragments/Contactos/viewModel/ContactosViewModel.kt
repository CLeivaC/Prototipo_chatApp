package com.leiva.prototipo_chatapp.ui.Fragments.Contactos.viewModel

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.TypeConverter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.leiva.prototipo_chatapp.Data.database.AppDatabase
import com.leiva.prototipo_chatapp.Data.database.dao.UsuarioDao
import com.leiva.prototipo_chatapp.Data.database.entities.ContactoEntity
import com.leiva.prototipo_chatapp.Data.database.entities.UsuarioData
import com.leiva.prototipo_chatapp.Modelo.Contacto
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.Utilidades.MyApp
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat
import kotlinx.coroutines.launch
import java.util.*

class ContactosViewModel() : ViewModel() {
    private val TAG = "ContactosViewModel"
    val READ_CONTACTS_PERMISSION_REQUEST_CODE = 1

    private val _usuariosOriginalesLiveData = MutableLiveData<List<ContactoEntity>>()
    val usuariosOriginalesLiveData: LiveData<List<ContactoEntity>>
        get() = _usuariosOriginalesLiveData

    // LiveData para la lista filtrada de usuarios
    private val _usuariosFiltradosLiveData = MutableLiveData<List<ContactoEntity>>()
    val usuariosFiltradosLiveData: LiveData<List<ContactoEntity>>
        get() = _usuariosFiltradosLiveData

    private val currentUser = FirebaseAuth.getInstance().currentUser
    // Método para filtrar usuarios
    fun filtrarUsuarios(searchText: String) {
        val usuariosOriginales = _usuariosOriginalesLiveData.value ?: return
        val usuariosFiltrados = if (searchText.isNotBlank()) {
            // Filtrar la lista si el texto de búsqueda no está vacío
            usuariosOriginales.filter {
                it.n_usuario?.toLowerCase(Locale.getDefault())?.contains(searchText.toLowerCase(Locale.getDefault())) ?: false ||
                        it.telefono?.toLowerCase(Locale.getDefault())?.contains(searchText.toLowerCase(Locale.getDefault())) ?: false
            }
        } else {
            // Mostrar todos los usuarios si el texto de búsqueda está vacío
            usuariosOriginales
        }
        _usuariosFiltradosLiveData.postValue(usuariosFiltrados)
    }

    fun obtenerYVerificarContactos(context: Context) {
        viewModelScope.launch {
            //Obtiene la lista de contactos
                val contactos = getDeviceContacts(context)
                obtenerMiNumero { miNumero ->
                    val contactosNormalizados = contactos.map { it.replace(Regex("[^+\\d]"), "") }
                    val contactosExcluyendoMiNumero = contactosNormalizados.filter { it != miNumero }
                    Log.d(TAG, "Contactos obtenidos: $contactosExcluyendoMiNumero")
                    //Verifica los contactos en firebase, excluyendo el número propio
                    verificarContactosEnFirebase(contactosExcluyendoMiNumero, context)
                }
        }
    }

    //Obtener contactos de la agenda del teléfono
    private fun getDeviceContacts(context: Context): List<String> {
        val contactsList = mutableListOf<String>()
        val contentResolver = context.contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        cursor?.use {
            val phoneNumberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                if (phoneNumberIndex != -1) {
                    val phoneNumber = it.getString(phoneNumberIndex)
                    Log.d(TAG, "Número de teléfono obtenido: $phoneNumber")
                    contactsList.add(phoneNumber)
                }
            }
        }
        Log.d(TAG, "Lista de contactos obtenida: $contactsList")
        return contactsList
    }


    private fun guardarContactosEnRoom(contactos: List<ContactoEntity>, context: Context) {
        viewModelScope.launch {

            val contactosDispositivo = getDeviceContacts(context)
            val contactosNormalizados = contactosDispositivo.map { it.replace(Regex("[^+\\d]"), "") }

            // Filtrar los contactos para incluir solo aquellos presentes en la agenda
            val contactosAGuardar = contactos.filter { contacto ->
                contactosNormalizados.contains(contacto.telefono)
            }
            // Insertar los contactos nuevos filtrados en la base de datos local
            MyApp.database.contactoDao().insertarContactos(contactosAGuardar)

            // Obtener todos los contactos guardados en la base de datos local después de la inserción
            val contactosGuardados = MyApp.database.contactoDao().getAllContactos(currentUser!!.uid)
                .filter { contacto ->
                    contactosNormalizados.contains(contacto.telefono)
                }

            // Verificar si los contactos guardados coinciden con los contactos agregados y filtrados
            if (contactosGuardados.containsAll(contactosAGuardar)) {
                Log.d(TAG, "Los contactos se han guardado correctamente en Room.")
            } else {
                Log.e(TAG, "Error: Algunos contactos no se han guardado correctamente en Room.")
            }
            val contactosOrdenados = ordenarContactosAlfabeticamente(contactosGuardados)
            _usuariosOriginalesLiveData.postValue(contactosOrdenados)
        }
    }

    fun cargarContactosDesdeRoom(context: Context) {
        viewModelScope.launch {
            val ownerUid = currentUser?.uid
            Log.d("uid","Este es mi ownerUId:$ownerUid")
            if (ownerUid == null) {
                Log.e(TAG, "UID del usuario actual no encontrado")
                return@launch
            }
            val contactosAgregados = MyApp.database.contactoDao().getAllContactos(ownerUid)
            val contactosDispositivo = getDeviceContacts(context)

            val contactosNormalizados = contactosDispositivo.map { it.replace(Regex("[^+\\d]"), "") }

            // Filtrar los contactos agregados para incluir solo aquellos cuyos números de teléfono estén presentes en el dispositivo
            val contactosFiltrados = contactosAgregados.filter { contacto ->
                contactosNormalizados.contains(contacto.telefono)
            }

            // Obtener solo los contactos cuyos números de teléfono estén presentes tanto en Room como en el dispositivo
            val contactosOrdenados = ordenarContactosAlfabeticamente(contactosFiltrados)

            _usuariosOriginalesLiveData.postValue(contactosOrdenados)
        }
    }


    private fun ordenarContactosAlfabeticamente(contactos: List<ContactoEntity>): List<ContactoEntity> {
        return contactos.sortedBy { it.n_usuario?.toLowerCase(Locale.getDefault()) }
    }


    private fun verificarContactosEnFirebase(contactos: List<String>, context: Context) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("usuarios")
        val contentResolver = context.contentResolver
        val uidUsuario = firebaseUser?.uid
        Log.d("uid","Este es mi uid: $uidUsuario")

        // Primero elimina los contactos existentes en Firebase
        eliminarContactosDeUsuarioEnFirebase(uidUsuario!!) { success ->
            if (success) {
                reference.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val contactosFirebase = mutableListOf<ContactoEntity>()
                        for (sh in snapshot.children) {
                            val usuario: Usuario? = sh.getValue(Usuario::class.java)
                            val numeroTelefonoUsuario = usuario?.getTelefono()
                            if (numeroTelefonoUsuario != null && contactos.contains(numeroTelefonoUsuario)) {
                                // Obtener nombre de contacto de la agenda del teléfono.
                                val nombreContacto = UtilidadesChat.obtenerNombreDesdeTelefono(contentResolver, numeroTelefonoUsuario)
                                // Creación de contacto Entity
                                val contacto = ContactoEntity(uid = usuario.getUid(), n_usuario = nombreContacto,
                                    telefono = numeroTelefonoUsuario, imagen = usuario.getImagen(),
                                    oculto = usuario.getOculto() ?: false, ultimoMensajeTimestamp = usuario.ultimoMensajeTimestamp,
                                    imagenFondoPerfil = usuario.getFondoPerfilUrl(), infoAdicional = usuario.getInfoAdicional(),
                                    ownerUid = uidUsuario,
                                    haTenidoConversacion = null
                                )
                                // Agregación de contacto a la lista
                                contactosFirebase.add(contacto)
                                agregarContactoAUsuarioEnFirebase(uidUsuario, contacto.n_usuario, contacto.telefono)
                                // Verifica si el UID del usuario es diferente del UID del usuario autenticado
                                if (usuario.getUid() == uidUsuario) {
                                    continue // Omitir el propio contacto
                                }
                                // Si el nombre es distinto, se actualiza en firebase y en ROOM
                                if (usuario.getN_Usuario() != nombreContacto) {
                                    usuario.setN_Usuario(nombreContacto)
                                    actualizarNombreContactoEnFirebase(uidUsuario, nombreContacto, numeroTelefonoUsuario)
                                    actualizarNombreContactoEnRoom(numeroTelefonoUsuario, nombreContacto)
                                }
                            }
                        }
                        // Llamada a la función para eliminar y guardar contactos en Room
                        viewModelScope.launch {
                            guardarContactosEnRoom(contactosFirebase, context)
                            cargarContactosDesdeRoom(context)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error al obtener datos de la base de datos: $error")
                    }
                })
            } else {
                Log.e(TAG, "Error al eliminar los contactos existentes de Firebase")
            }
        }
    }

    private fun eliminarContactosDeUsuarioEnFirebase(uidUsuario: String, callback: (Boolean) -> Unit) {
        val reference = FirebaseDatabase.getInstance().reference
        val contactosReference = reference.child("usuarios").child(uidUsuario).child("contactos")

        contactosReference.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Contactos eliminados correctamente de Firebase")
                callback(true)
            } else {
                Log.e(TAG, "Error al eliminar los contactos de Firebase: ${task.exception}")
                callback(false)
            }
        }
    }

    private fun agregarContactoAUsuarioEnFirebase(uidUsuario: String, nombreContacto: String, telefonoContacto: String) {
        val reference = FirebaseDatabase.getInstance().reference
        val contactosReference = reference.child("usuarios").child(uidUsuario).child("contactos")
        val query = contactosReference.orderByChild("telefono").equalTo(telefonoContacto)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // El contacto no existe, agregarlo a Firebase
                    val nuevoContactoKey = contactosReference.push().key
                    if (nuevoContactoKey != null) {
                        val nuevoContacto = HashMap<String, Any>()
                        nuevoContacto["nombre"] = nombreContacto
                        nuevoContacto["telefono"] = telefonoContacto
                        contactosReference.child(nuevoContactoKey).setValue(nuevoContacto)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Contacto agregado correctamente a Firebase
                                    Log.d(TAG, "Contacto agregado correctamente a Firebase")
                                } else {
                                    // Error al agregar el contacto a Firebase
                                    Log.e(TAG, "Error al agregar contacto a Firebase: ${task.exception}")
                                }
                            }
                    }
                } else {
                    // El contacto ya existe en Firebase, no es necesario agregarlo
                    Log.d(TAG, "El contacto ya existe en Firebase")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al consultar la lista de contactos del usuario: $error")
            }
        })
    }



    private fun actualizarNombreContactoEnFirebase(uidUsuario: String?, nuevoNombre: String, telefonoContacto: String) {
        // Obtener una referencia a la raíz de la base de datos de Firebase
        val reference = FirebaseDatabase.getInstance().reference

        // Obtener una referencia al nodo de contactos del usuario especificado
        val contactosReference = reference.child("usuarios").child(uidUsuario!!).child("contactos")

        // Crear una consulta para encontrar el contacto con el número de teléfono dado
        val query = contactosReference.orderByChild("telefono").equalTo(telefonoContacto)

        // Agregar un ValueEventListener para escuchar los cambios en la consulta
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Verificar si existen datos en la consulta
                if (snapshot.exists()) {
                    // Iterar a través de los resultados de la consulta
                    for (contactoSnapshot in snapshot.children) {
                        // Obtener la clave (ID) del contacto
                        val key = contactoSnapshot.key
                        // Obtener el nombre actual del contacto
                        val nombreActual = contactoSnapshot.child("nombre").getValue(String::class.java)
                        // Verificar si el nombre actual es diferente al nuevo nombre proporcionado
                        if (nombreActual != nuevoNombre) {
                            // Actualizar el nombre del contacto en la base de datos de Firebase
                            contactosReference.child(key!!).child("nombre").setValue(nuevoNombre)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Log.d(TAG, "Nombre de contacto actualizado en Firebase")
                                        // Llamar a la función para actualizar el nombre del contacto en la base de datos local
                                        actualizarNombreContactoEnRoom(telefonoContacto, nuevoNombre)
                                    } else {
                                        Log.e(TAG, "Error al actualizar nombre de contacto en Firebase: ${task.exception}")
                                    }
                                }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al consultar la lista de contactos del usuario: $error")
            }
        })
    }

    private fun actualizarNombreContactoEnRoom(telefonoContacto: String, nuevoNombre: String) {
        // Lanzar una nueva corrutina utilizando viewModelScope
        viewModelScope.launch {
            // Obtener el contacto de la base de datos local mediante su número de teléfono
            val contacto = MyApp.database.contactoDao().getContactoByTelefono(telefonoContacto)
            // Verificar si se encontró un contacto con el número de teléfono dado
            contacto?.let {
                // Actualizar el nombre del contacto con el nuevo nombre proporcionado
                it.n_usuario = nuevoNombre
                // Actualizar el contacto en la base de datos local
                MyApp.database.contactoDao().actualizarContacto(it)
            }
        }
    }

    private fun obtenerMiNumero(callback: (String?) -> Unit) {
        // Obtener el usuario actualmente autenticado
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        // Referencia a la ubicación en la base de datos de Firebase donde se almacena la información del usuario
        val reference = FirebaseDatabase.getInstance().reference.child("usuarios")
            .child(firebaseUser?.uid.orEmpty())

        // Agregar un ValueEventListener para escuchar cambios en los datos del usuario
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Inicializar la variable para almacenar el número de teléfono del usuario
                var miNumero: String? = null
                // Verificar si existe un snapshot válido
                if (snapshot.exists()) {
                    // Obtener el objeto Usuario del snapshot
                    val usuario: Usuario? = snapshot.getValue(Usuario::class.java)
                    // Obtener el número de teléfono del objeto Usuario
                    miNumero = usuario?.getTelefono()
                }
                // Llamar al callback con el número de teléfono obtenido
                callback(miNumero)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al obtener mi número de teléfono: $error")
                // Llamar al callback con un valor nulo para indicar un error
                callback(null)
            }
        })
    }
}
