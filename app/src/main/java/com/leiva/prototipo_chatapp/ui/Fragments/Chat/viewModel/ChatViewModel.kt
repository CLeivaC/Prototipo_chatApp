package com.leiva.prototipo_chatapp.ui.Fragments.Chat.viewModel
import android.content.ContentResolver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.leiva.prototipo_chatapp.Data.database.entities.ContactoEntity
import com.leiva.prototipo_chatapp.Modelo.Chat
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.Utilidades.MyApp
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    // LiveData para almacenar la lista de contactos
    private val _contactos = MutableLiveData<List<ContactoEntity>>()
    val contactos: LiveData<List<ContactoEntity>>
        get() = _contactos
    // Obtener la instancia actual del usuario de Firebase
    private val firebaseUser = FirebaseAuth.getInstance().currentUser

    fun onViewCreated(contentResolver: ContentResolver) {
        // Cargar la lista de contactos con conversaciones desde Room
        cargarContactosConConversacionDesdeRoom()
        // Buscar la lista de contactos con conversaciones en Firebase
        BuscarListaContactosConConversacion(contentResolver)
        // Actualizar el token de Firebase
        updateFirebaseToken()

    }

    // Método para actualizar el token de Firebase en la base de datos
    fun updateToken(token: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("Tokens")
        val token1 = com.leiva.prototipo_chatapp.Notificaciones.Token(token)
        // Actualizar el token asociado al usuario actual
        firebaseUser?.uid?.let { reference.child(it).setValue(token1) }
    }

    // Método para buscar la lista de contactos con conversaciones en Firebase
    fun BuscarListaContactosConConversacion(contentResolver: ContentResolver) {
        val tempMap: MutableMap<String, Usuario> = mutableMapOf()
        val reference = FirebaseDatabase.getInstance().reference.child("ListaMensajes").child(firebaseUser?.uid ?: "")

        // Escuchar una única vez los cambios en la referencia de la base de datos
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterar sobre los hijos de la referencia
                snapshot.children.forEach { usuarioSnapshot ->
                    val uidUsuario = usuarioSnapshot.key
                    uidUsuario?.let { uid ->
                        // Cargar los detalles del usuario y almacenarlos en un mapa temporal
                        cargarDetallesUsuario(uid, contentResolver) { usuario ->
                            tempMap[usuario.getUid()] = usuario
                            // Si se han cargado todos los detalles de los usuarios, guardar los contactos en Room
                            if (tempMap.size == snapshot.childrenCount.toInt()) {
                                val sortedContactos = tempMap.values.toList().sortedByDescending { it.ultimoMensajeTimestamp }
                                guardarContactosEnRoom(sortedContactos)
                            }
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    // Función para cargar detalles de un usuario desde Firebase
    private fun cargarDetallesUsuario(uidUsuario: String, contentResolver: ContentResolver, callback: (Usuario) -> Unit) {
        // Obtener la referencia del usuario en la base de datos Firebase
        val referenciaUsuario = FirebaseDatabase.getInstance().reference.child("usuarios").child(uidUsuario)

        // Escuchar una única vez los cambios en la referencia del usuario
        referenciaUsuario.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Obtener los datos del usuario desde el snapshot
                val usuario = dataSnapshot.getValue(Usuario::class.java)
                // Obtener el número de teléfono del usuario
                val numeroTelefonoUsuario = usuario?.getTelefono()
                // Si se obtiene un número de teléfono válido
                numeroTelefonoUsuario?.let { numero ->
                    // Obtener el nombre de usuario asociado al número de teléfono
                    usuario.setN_Usuario(UtilidadesChat.obtenerNombreDesdeTelefono(contentResolver, numero))
                    // Obtener el último mensaje del usuario
                    obtenerUltimoMensaje(uidUsuario) { ultimoMensajeTimestamp ->
                        // Asignar el timestamp del último mensaje al usuario
                        usuario.ultimoMensajeTimestamp = ultimoMensajeTimestamp
                        // Llamar al callback con el usuario cargado
                        callback(usuario)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    // Función para obtener el timestamp del último mensaje entre dos usuarios desde Firebase
    private fun obtenerUltimoMensaje(uidUsuario: String, callback: (Long) -> Unit) {
        // Verificar si el usuario actual de Firebase está autenticado
        firebaseUser?.let {
            // Si el usuario está autenticado
            val reference = FirebaseDatabase.getInstance().reference.child("chats")
            // Escuchar los cambios en la referencia de los chats en la base de datos Firebase
            reference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Inicializar el timestamp del último mensaje como 0
                    var ultimoMensajeTimestamp = 0L
                    // Iterar sobre los chats en el snapshot de datos
                    snapshot.children.forEach { dataSnapshot ->
                        // Obtener un objeto Chat desde el snapshot
                        val chat = dataSnapshot.getValue(Chat::class.java)
                        chat?.let {
                            // Verificar si el chat corresponde a la conversación entre los dos usuarios
                            if ((it.getEmisor() == firebaseUser.uid && it.getReceptor() == uidUsuario) ||
                                (it.getEmisor() == uidUsuario && it.getReceptor() == firebaseUser.uid)
                            ) {
                                // Obtener el timestamp del mensaje
                                val timestamp = it.getHora()
                                // Actualizar el timestamp del último mensaje si el timestamp actual es mayor
                                if (timestamp > ultimoMensajeTimestamp) {
                                    ultimoMensajeTimestamp = timestamp
                                }
                            }
                        }
                    }
                    // Llamar al callback con el timestamp del último mensaje
                    callback(ultimoMensajeTimestamp)
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }


    // Función para guardar los contactos en la base de datos Room y actualizar la lista de contactos observables
    private fun guardarContactosEnRoom(contactos: List<Usuario>) {
        // Utilizar viewModelScope para lanzar una corrutina en el ámbito del ViewModel
        viewModelScope.launch {
            // Obtener el DAO de la base de datos
            val dao = MyApp.database.contactoDao()
            // Obtener el UID del propietario actual
            val ownerUid = firebaseUser?.uid ?: return@launch
            // Iterar sobre la lista de contactos recibida
            contactos.forEach { usuario ->
                // Verificar si el contacto ya existe en la base de datos
                val contactoExistente = dao.getUserById(usuario.getUid(), ownerUid)
                // Crear una entidad de contacto con los datos del usuario actual
                val contactoEntity = ContactoEntity(
                    uid = usuario.getUid(),
                    n_usuario = usuario.getN_Usuario(),
                    telefono = usuario.getTelefono(),
                    imagen = usuario.getImagen(),
                    oculto = usuario.getOculto() ?: true,
                    haTenidoConversacion = true,
                    ultimoMensajeTimestamp = usuario.ultimoMensajeTimestamp,
                    infoAdicional = usuario.getInfoAdicional(),
                    imagenFondoPerfil = usuario.getFondoPerfilUrl(),
                    ownerUid = ownerUid
                )
                // Insertar o actualizar el contacto en la base de datos según su existencia
                if (contactoExistente == null) {
                    dao.insertarContacto(contactoEntity)
                } else {
                    dao.actualizarContacto(contactoEntity)
                }
            }
            // Obtener la lista actualizada de contactos con conversación del propietario actual
            val contactosConConversacion = dao.getContactosConConversacion(ownerUid)
            // Actualizar la lista de contactos observables
            _contactos.postValue(contactosConConversacion)
        }
    }

    // Función para cargar los contactos con conversaciones desde la base de datos Room
    private fun cargarContactosConConversacionDesdeRoom() {
        // Lanzar una corrutina en el ámbito del ViewModel
        viewModelScope.launch {
            // Obtener el UID del propietario actual
            val ownerUid = firebaseUser?.uid ?: return@launch
            // Obtener los contactos con conversaciones del propietario actual desde la base de datos Room
            val contactosConConversacion = MyApp.database.contactoDao().getContactosConConversacion(ownerUid)
            // Actualizar la lista de contactos observables
            _contactos.postValue(contactosConConversacion)
        }
    }

    // Función para actualizar el token de Firebase
    private fun updateFirebaseToken() {
        // Obtener el token de Firebase Messaging
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let { token ->
                        // Actualizar el token de Firebase en la base de datos
                        updateToken(token)
                    }
                }
            }
    }
}