package com.leiva.prototipo_chatapp.ui.Activities.Mensajes

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.leiva.prototipo_chatapp.Data.database.entities.MensajeEntity
import com.leiva.prototipo_chatapp.ui.Activities.Mensajes.Adaptador.AdaptadorMensaje
import com.leiva.prototipo_chatapp.ui.Activities.InfoPerfilChat.InfoPerfilChatActivity
import com.leiva.prototipo_chatapp.Modelo.Chat
import com.leiva.prototipo_chatapp.Modelo.Contacto
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.Notificaciones.APIService
import com.leiva.prototipo_chatapp.Notificaciones.Client
import com.leiva.prototipo_chatapp.Notificaciones.Data
import com.leiva.prototipo_chatapp.Notificaciones.MyResponse
import com.leiva.prototipo_chatapp.Notificaciones.Sender
import com.leiva.prototipo_chatapp.Notificaciones.Token
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.MyApp
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Response

class MensajesActivity : AppCompatActivity() {

    private lateinit var toolbar_chat: androidx.appcompat.widget.Toolbar
    private var receptorActivo: Boolean = false
    private lateinit var imagen_perfil_chat: ImageView
    private lateinit var N_usuario_chat: TextView
    private lateinit var IB_Adjuntar: ImageButton
    var uid_usuario_seleccionado: String = ""
    private lateinit var ET_mensaje: EditText
    private lateinit var IB_enviar: ImageButton
    private lateinit var IB_EnviarTexto: ImageButton
    var firebaseUser: FirebaseUser? = null
    private var imagenUri: Uri? = null
    lateinit var RV_chats: RecyclerView
    var chatAdapter: AdaptadorMensaje? = null
    var chatList: ArrayList<Chat>?= ArrayList()
    var notificar = false
    var apiService: APIService? = null
    val timestamp = ServerValue.TIMESTAMP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mensajes)
        initComponents()
        obtenerUID()
        leerInfoUsuarioSeleccionado()
        marcarMensajesComoLeidos()
        initListeners()
        requestNotificationPermission()
    }

    //Solicitar permiso de notificaciones
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permiso ya concedido, puedes mostrar notificaciones
                }
                else -> {
                    // Solicita el permiso
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido, puedes mostrar notificaciones
        } else {

        }
    }

    //Inicialización de componentes
    private fun initComponents() {

        toolbar_chat = findViewById(R.id.toolbar_chat)
        imagen_perfil_chat = findViewById(R.id.imagen_perfil_chat)
        N_usuario_chat = findViewById(R.id.N_usuario_chat)
        ET_mensaje = findViewById(R.id.ET_mensaje)
        IB_enviar = findViewById(R.id.IB_Enviar)
        IB_EnviarTexto = findViewById(R.id.IB_EnviarTexto)
        IB_Adjuntar = findViewById(R.id.IB_Adjuntar)
        firebaseUser = FirebaseAuth.getInstance().currentUser
        RV_chats = findViewById(R.id.RV_chats)
        RV_chats.setHasFixedSize(true)
        var linearLayoutManager = LinearLayoutManager(applicationContext)
        linearLayoutManager.stackFromEnd = true
        RV_chats.layoutManager = linearLayoutManager
        apiService =
            Client.Client.getClient("https://fcm.googleapis.com/")!!.create(APIService::class.java)
    }

    private fun initListeners() {
        val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES


        if (isDarkTheme) {
            ET_mensaje.setTextColor(Color.WHITE)
        } else {
            ET_mensaje.setTextColor(Color.BLACK)
        }


        // Configurar el TextWatcher
        ET_mensaje.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Eliminar los espacios en blanco al principio y al final del texto
                val trimmedText = s?.trim()
                // Cambiar el color del botón si hay al menos un carácter
                if (trimmedText != null) {
                    if (trimmedText.isNotEmpty()|| trimmedText.isNotBlank()) {
                        IB_enviar.visibility = View.GONE
                        IB_EnviarTexto.visibility = View.VISIBLE
                    } else {
                        IB_enviar.visibility = View.VISIBLE
                        IB_EnviarTexto.visibility = View.GONE
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        IB_enviar.setOnClickListener {
            val mensaje = ET_mensaje.text.toString()
            if (mensaje.isEmpty() || mensaje.isBlank()) {
                Toast.makeText(applicationContext, "Por favor ingrese un mensaje", Toast.LENGTH_SHORT).show()
            }
        }
        IB_EnviarTexto.setOnClickListener {
            notificar = true
            val mensaje = ET_mensaje.text.toString()
            if(MyApp.isInternetAvailable(this)) {
                enviarMensaje(firebaseUser!!.uid, uid_usuario_seleccionado, mensaje)
                ET_mensaje.setText("")
            }else{
                Toast.makeText(applicationContext,"No hay conexión a internet",Toast.LENGTH_LONG).show()
            }

        }

        //Cuando pulsamos sobre el boton IB_adjuntar, abre la galería
        IB_Adjuntar.setOnClickListener {
            if(MyApp.isInternetAvailable(this)) {
                notificar = true
                abrirGaleria()
            }else{
                Toast.makeText(applicationContext,"No hay conexión a internet",Toast.LENGTH_LONG).show()
            }
        }

        //Obtener el uid, cuando el usuario quiera ver el perfil de su contacto
        toolbar_chat.setOnClickListener {
            val intent = Intent(applicationContext, InfoPerfilChatActivity::class.java)
            intent.putExtra("uid", uid_usuario_seleccionado)
            startActivity(intent)
        }
    }


    //Obtiene el uid del usuario seleccionado.
    private fun obtenerUID() {
        intent = intent
        uid_usuario_seleccionado = intent.getStringExtra("uid_usuario").toString()
        // Log del UID obtenido
        Log.d(TAG, "UID del usuario seleccionado: $uid_usuario_seleccionado")
    }

    /*
     * Cuando el usuario pulsa sobre un contacto, mostrará su información y recuperara los mensajes
     */
    private fun leerInfoUsuarioSeleccionado() {

        lifecycleScope.launch {
            val contactoDao = MyApp.database.contactoDao().getUserById(uid_usuario_seleccionado,firebaseUser!!.uid)
            var imagenReceptor :String?=null
            if(contactoDao!=null){
                 imagenReceptor = contactoDao.imagen
                val telefonoUsuario = contactoDao.telefono
                N_usuario_chat.text = contactoDao.n_usuario


                // Log de la información del usuario seleccionado
                Log.d(TAG, "Información del usuario seleccionado desde Room:")
                Log.d(TAG, "Nombre: ${contactoDao.n_usuario}")
                Log.d(TAG, "Imagen: $imagenReceptor")

                Glide.with(applicationContext).load(imagenReceptor)
                    .placeholder(R.drawable.ic_item_usuario).into(imagen_perfil_chat)
                obtenerImagenUsuarioActual(object : ImagenUsuarioCallback {
                    override fun onImagenObtenida(imagen: String) {
                        RecuperarMensajes(
                            firebaseUser!!.uid,
                            uid_usuario_seleccionado,
                            imagenReceptor,
                            imagen
                        )
                    }
                })
            }

        }
    }


    //Conversión de mensajeEntity a objeto de tipo Chat
    fun convertirMensajeEntityAChat(mensajeEntity: MensajeEntity): Chat {
        val chat = Chat()

        chat.setId_Mensaje(mensajeEntity.id_mensaje)
        chat.setEmisor(mensajeEntity.emisorId)
        chat.setReceptor(mensajeEntity.receptorId)
        chat.setMensaje(mensajeEntity.mensaje)
        chat.setUrl(mensajeEntity.url)
        chat.setIsVisto(mensajeEntity.visto)
        chat.setHora(mensajeEntity.hora)

        return chat
    }


    private fun RecuperarMensajes(
        EmisorUid: String,
        ReceptorUid: String,
        ReceptorImagen: String?,
        ImagenActual: String?
    ) {
        // Limpiar lista de chats
        chatList?.clear()

        if (!MyApp.isInternetAvailable(this)) {
            Log.d(TAG, "Recuperando mensajes desde Room...")
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val mensajesRoom = MyApp.database.mensajeDao().obtenerMensajesDeRoom(EmisorUid, ReceptorUid)
                    val chats = mensajesRoom.map { convertirMensajeEntityAChat(it) }
                    val uniqueChats = chats.distinctBy { it.getId_Mensaje() } // Filtra mensajes duplicados

                    withContext(Dispatchers.Main) {
                        (chatList as ArrayList<Chat>).clear()
                        (chatList as ArrayList<Chat>).addAll(uniqueChats) // Añadimos los chats filtrados al chatList genérico
                        Log.d(TAG, "Mensajes recuperados desde Room: ${ (chatList as ArrayList<Chat>).size}")

                        chatAdapter = AdaptadorMensaje(
                            this@MensajesActivity,
                            chatList as ArrayList<Chat>,
                            ReceptorImagen!!,
                            ImagenActual!!
                        )
                        RV_chats.adapter = chatAdapter
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al recuperar mensajes desde Room: ${e.message}")
                }
            }
        } else {
            val reference = FirebaseDatabase.getInstance().reference.child("chats")

            // Añadir un ChildEventListener para detectar eliminaciones
            reference.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val chat = snapshot.getValue(Chat::class.java)
                    if (chat != null) {
                        if ((chat.getReceptor().equals(EmisorUid) && chat.getEmisor().equals(ReceptorUid))
                            || (chat.getReceptor().equals(ReceptorUid) && chat.getEmisor().equals(EmisorUid))) {
                            (chatList as ArrayList<Chat>).add(chat)

                            val mensajeEntity = MensajeEntity(
                                id_mensaje = chat.getId_Mensaje()!!,
                                emisorId = chat.getEmisor()!!,
                                receptorId = chat.getReceptor()!!,
                                mensaje = chat.getMensaje() ?: "",
                                url = chat.getUrl(),
                                visto = chat.isVisto(),
                                hora = chat.getHora()
                            )
                            insertarMensajeEnRoom(mensajeEntity)
                        }
                    }
                    chatAdapter = AdaptadorMensaje(
                        this@MensajesActivity,
                        chatList as ArrayList<Chat>,
                        ReceptorImagen!!,
                        ImagenActual!!
                    )
                    RV_chats.adapter = chatAdapter
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // Manejar cambios si es necesario
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val chat = snapshot.getValue(Chat::class.java)
                    if (chat != null) {
                        (chatList as ArrayList<Chat>).removeIf { it.getId_Mensaje() == chat.getId_Mensaje() }

                        lifecycleScope.launch(Dispatchers.IO) {
                            MyApp.database.mensajeDao().borrarMensajePorId(chat.getId_Mensaje()!!)
                        }

                        chatAdapter = AdaptadorMensaje(
                            this@MensajesActivity,
                            chatList as ArrayList<Chat>,
                            ReceptorImagen!!,
                            ImagenActual!!
                        )
                        RV_chats.adapter = chatAdapter
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error al escuchar cambios en Firebase: $error")
                }
            })

            reference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    (chatList as ArrayList<Chat>).clear()
                    for (sn in snapshot.children) {
                        val chat = sn.getValue(Chat::class.java)
                        if (chat != null) {
                            if ((chat.getReceptor().equals(EmisorUid) && chat.getEmisor().equals(ReceptorUid))
                                || (chat.getReceptor().equals(ReceptorUid) && chat.getEmisor().equals(EmisorUid))) {
                                (chatList as ArrayList<Chat>).add(chat)

                                val mensajeEntity = MensajeEntity(
                                    id_mensaje = chat.getId_Mensaje()!!,
                                    emisorId = chat.getEmisor()!!,
                                    receptorId = chat.getReceptor()!!,
                                    mensaje = chat.getMensaje() ?: "",
                                    url = chat.getUrl(),
                                    visto = chat.isVisto(),
                                    hora = chat.getHora()
                                )
                                insertarMensajeEnRoom(mensajeEntity)
                            }
                        }
                    }
                    chatAdapter = AdaptadorMensaje(
                        this@MensajesActivity,
                        chatList as ArrayList<Chat>,
                        ReceptorImagen!!,
                        ImagenActual!!
                    )
                    RV_chats.adapter = chatAdapter

                    if (receptorActivo) {
                        marcarMensajesComoLeidos()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error al recuperar mensajes: $error")
                }
            })
        }
    }



    //Obtener imagen propia de perfil
    interface ImagenUsuarioCallback {
        fun onImagenObtenida(imagen: String)
    }


    private fun obtenerImagenUsuarioActual(callback: ImagenUsuarioCallback) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            val userId = firebaseUser.uid
            if (!MyApp.isInternetAvailable(this)) {
                lifecycleScope.launch {
                    val miImagen = MyApp.get().readMyImage().firstOrNull()
                    Log.d("imagen", "Esta es mi imagen: $miImagen")
                    callback.onImagenObtenida(miImagen!!)
                }
            } else {
                //Si hay internet
                if (MyApp.isInternetAvailable(this)) {
                    val databaseReference = FirebaseDatabase.getInstance().reference.child("usuarios").child(userId)
                    databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val usuario = snapshot.getValue(Usuario::class.java)
                                if (usuario != null) {
                                    val imagen = usuario.getImagen()
                                    //Escribir en el almacenamiento del dispositivo la imagen de perfil
                                    lifecycleScope.launch {
                                        imagen.let { MyApp.get().writeImage(it) }
                                    }
                                    callback.onImagenObtenida(imagen)
                                } else {
                                    callback.onImagenObtenida("") // Otra opción: callback.onImagenNoDisponible()
                                }
                            } else {
                                callback.onImagenObtenida("") // Otra opción: callback.onImagenNoDisponible()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Log.e("Glide", "Error al obtener datos de usuario: $error")
                            callback.onImagenObtenida("") // Otra opción: callback.onImagenNoDisponible()
                        }
                    })
                } else {
                    // El usuario no está autenticado
                    Log.e("Glide", "Error: Usuario no autenticado")
                    callback.onImagenObtenida("") // Otra opción: callback.onImagenNoDisponible()
                }
            }
        }
    }


    //Insertar mensaje en ROOM.
    private fun insertarMensajeEnRoom(mensaje: MensajeEntity) {
        lifecycleScope.launch {
            MyApp.database.mensajeDao().insertarMensaje(mensaje)
        }
    }

    private fun enviarMensaje(uid_emisor: String, uid_receptor: String, mensaje: String) {
        val reference = FirebaseDatabase.getInstance().reference
        val mensajeKey = reference.push().key
        val infoMensaje = HashMap<String, Any?>()
        infoMensaje["id_mensaje"] = mensajeKey
        infoMensaje["emisor"] = uid_emisor
        infoMensaje["receptor"] = uid_receptor
        infoMensaje["mensaje"] = mensaje
        infoMensaje["url"] = ""
        infoMensaje["visto"] = false
        infoMensaje["hora"] = timestamp



        CoroutineScope(Dispatchers.Main).launch {
            // Guardar el mensaje en la base de datos
            guardarMensajeEnBD(reference, mensajeKey, infoMensaje, uid_receptor)
            // Actualizar la lista de mensajes
            actualizarListaMensajes(uid_receptor)
            // Obtener el número de teléfono del emisor
            val telefonoEmisor = obtenerTelefonoEmisor(uid_emisor)

            // Obtener el nombre del contacto del receptor
            val nombreContacto = obtenerNombreContacto(uid_receptor, telefonoEmisor)

            // Enviar la notificación
            estadoNotificacionesReceptor(uid_receptor, nombreContacto, mensaje)
        }
    }

    /*
     * Obtener el teléfono del emisor
     */
    private suspend fun obtenerTelefonoEmisor(uid_emisor: String): String {
        return withContext(Dispatchers.IO) {
            val referenciaEmisor =
                FirebaseDatabase.getInstance().reference.child("usuarios").child(uid_emisor)
            val snapshot = referenciaEmisor.get().await()
            val usuarioEmisor = snapshot.getValue(Usuario::class.java)
            usuarioEmisor?.getTelefono() ?: ""
        }
    }

    /*
     * Obtener nombre del contacto de la agenda del dispositivo
     */
    private suspend fun obtenerNombreContacto(
        uid_receptor: String,
        telefonoEmisor: String
    ): String {
        return withContext(Dispatchers.IO) {
            val contactosReference = FirebaseDatabase.getInstance().reference
                .child("usuarios").child(uid_receptor).child("contactos")
            val snapshot = contactosReference.get().await()
            var nombreContacto = telefonoEmisor // Por defecto, devolver el número de teléfono
            for (contactoSnapshot in snapshot.children) {
                val contacto = contactoSnapshot.getValue(Contacto::class.java)
                val telefonoContacto = contacto?.getTelefono()
                if (telefonoContacto != null && telefonoContacto == telefonoEmisor) {
                    nombreContacto = contacto.getNombre() ?: telefonoEmisor
                    break
                }
            }
            nombreContacto
        }
    }

    /*
     * Actualizar la lista de mensajes para saber quien ha hablado con quien
     */
    private suspend fun actualizarListaMensajes(uid_receptor: String) {
        withContext(Dispatchers.IO) {
            val listaMensajesEmisor =
                FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                    .child(firebaseUser!!.uid).child(uid_receptor)
            val listaMensajesReceptor =
                FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                    .child(uid_receptor).child(firebaseUser!!.uid)

            listaMensajesEmisor.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        listaMensajesEmisor.child("uid").setValue(uid_receptor)
                    }
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
            listaMensajesReceptor.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        listaMensajesReceptor.child("uid").setValue(firebaseUser!!.uid)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    private fun guardarMensajeEnBD(
        reference: DatabaseReference,
        mensajeKey: String?,
        infoMensaje: HashMap<String, Any?>,
        uid_receptor: String
    ) {
        //Guarda el mensaje en la base de datos
        reference.child("chats").child(mensajeKey!!).setValue(infoMensaje)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Marcar mensajes como leídos
                    marcarMensajesComoLeidos()
                    val listaMensajesEmisor =
                        FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                            .child(firebaseUser!!.uid).child(uid_receptor)
                    listaMensajesEmisor.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                listaMensajesEmisor.child("uid").setValue(uid_receptor)
                            }

                            val listaMensajesReceptor =
                                FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                                    .child(uid_receptor).child(firebaseUser!!.uid)
                            listaMensajesReceptor.child("uid").setValue(firebaseUser!!.uid)
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }
            }
    }

    private fun estadoNotificacionesReceptor(
        uidReceptor: String,
        nombreContacto: String,
        mensaje: String
    ) {
        val reference =
            FirebaseDatabase.getInstance().reference.child("usuarios").child(uidReceptor)

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usuario = snapshot.getValue(Usuario::class.java)
                val notificacionesActivas = usuario?.getNotificaciones() ?: false

                // Verificar el estado del interruptor de notificaciones del receptor
                if (!notificacionesActivas) {
                    // Si las notificaciones están activas, enviar la notificación
                    Log.d(
                        TAG,
                        "Notificaciones activas para $nombreContacto, enviando notificación..."
                    )
                    enviarNotificacion(uidReceptor, nombreContacto, mensaje)
                } else {
                    // Si las notificaciones están desactivadas, no enviar la notificación
                    Log.d(TAG, "Las notificaciones del receptor $nombreContacto están desactivadas")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    TAG,
                    "Error al leer el estado de las notificaciones del receptor: ${error.message}"
                )
            }
        })
    }


    private fun enviarNotificacion(uidReceptor: String, nUsuario: String, mensaje: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = reference.orderByKey().equalTo(uidReceptor)
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    //Obtiene el token de la base de datos
                    val token: Token? = dataSnapshot.getValue(Token::class.java)
                    Log.d(TAG, "Token obtenido para el UID $uidReceptor: ${token?.getToken()}")

                    //Introducir datos que contendrá la notificación
                    val data = Data(firebaseUser!!.uid, R.drawable.burbuja_de_chat_not, "$nUsuario: $mensaje", "nuevo mensaje", uid_usuario_seleccionado)
                    val sender = Sender(data!!, token!!.getToken().toString())
                    //Envía la notificación
                    apiService!!.sendNotification(sender)
                        .enqueue(object : retrofit2.Callback<MyResponse> {
                            override fun onResponse(
                                call: Call<MyResponse>,
                                response: Response<MyResponse>
                            ) {
                                val responseBody = response.body()
                                if (response.isSuccessful && responseBody != null && responseBody.success == 1) {
                                    // La notificación se envió correctamente
                                    Log.d(TAG, "Notificación enviada correctamente")
                                } else {
                                    // La notificación no se pudo enviar correctamente
                                    Log.e(TAG, "Error al enviar la notificación: ${response.message()}")
                                }
                            }
                            override fun onFailure(call: Call<MyResponse>, t: Throwable) {
                                // Error al enviar la notificación
                                Log.e(TAG, "Error al enviar la notificación: $t")
                            }
                        })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // Error al obtener el token del receptor
                Log.e(TAG, "Error al obtener el token del receptor: ${error.message}")
            }
        })
    }

    private fun abrirGaleria() {
        //Abrir galería
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        GaleriaARL.launch(intent)
    }
    private val GaleriaARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { resultado ->
            //Descargar URL de la imagen
            if (resultado.resultCode == RESULT_OK) {
                val data = resultado.data
                imagenUri = data!!.data
                //Configuración de progressBar
                val cargandoImagen = ProgressDialog(this@MensajesActivity)
                cargandoImagen.setMessage("Por favor espere,la imagen se está enviando")
                cargandoImagen.setCanceledOnTouchOutside(false)
                cargandoImagen.show()
                //Insertar imágenes en el almacenamiento multimedia de firebase
                val carpetaImagenes = FirebaseStorage.getInstance().reference.child("Imagenes de mensajes")
                val reference = FirebaseDatabase.getInstance().reference
                val idMensaje = reference.push().key
                val nombreImagen = carpetaImagenes.child("${idMensaje}.jpg")
                val uploadTask: StorageTask<*>
                uploadTask = nombreImagen.putFile(imagenUri!!)
                uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                        }
                    }
                    return@Continuation nombreImagen.downloadUrl
                    //Añadir datos del mensaje de imagen a la Base de datos.
                }).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        cargandoImagen.dismiss()
                        val downloadUrl = task.result
                        val url = downloadUrl.toString()
                        val infoMensajeImagen = HashMap<String, Any?>()
                        infoMensajeImagen["id_mensaje"] = idMensaje
                        infoMensajeImagen["emisor"] = firebaseUser!!.uid
                        infoMensajeImagen["mensaje"] = "Se ha enviado la imagen"
                        infoMensajeImagen["receptor"] = uid_usuario_seleccionado
                        infoMensajeImagen["url"] = url
                        infoMensajeImagen["visto"] = false
                        infoMensajeImagen["hora"] = timestamp

                        reference.child("chats").child(idMensaje!!).setValue(infoMensajeImagen)
                            .addOnCompleteListener { tarea ->
                                if (tarea.isSuccessful) {
                                    val usuarioReference = FirebaseDatabase.getInstance().reference
                                        .child("usuarios").child(firebaseUser!!.uid)
                                    usuarioReference.addValueEventListener(object :
                                        ValueEventListener {
                                        @SuppressLint("SuspiciousIndentation")
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val usuario = snapshot.getValue(Usuario::class.java)

                                                enviarNotificacion(uid_usuario_seleccionado, usuario!!.getN_Usuario(), "Imagen enviada")

                                            notificar = false
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            TODO("Not yet implemented")
                                        }

                                    })
                                }
                            }

                        //Manejo de si el primer mensaje de la conversación es una imagen
                        reference.child("chats").child(idMensaje).setValue(infoMensajeImagen)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val listaMensajesEmisor =
                                        FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                                            .child(firebaseUser!!.uid)
                                            .child(uid_usuario_seleccionado)
                                    listaMensajesEmisor.addListenerForSingleValueEvent(object :
                                        ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (!snapshot.exists()) {
                                                listaMensajesEmisor.child("uid").setValue(uid_usuario_seleccionado)
                                            }
                                            val listaMensajesReceptor =
                                                FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                                                    .child(uid_usuario_seleccionado)
                                                    .child(firebaseUser!!.uid)
                                            listaMensajesReceptor.child("uid")
                                                .setValue(firebaseUser!!.uid)
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                        }

                                    })
                                }

                            }
                        Toast.makeText(applicationContext, "La imagen se ha enviado con éxito", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "La imagen se ha enviado con éxito")
                    }

                }
            } else {
                Toast.makeText(applicationContext, "Cancelado por el usuario", Toast.LENGTH_SHORT).show()
            }
        }
    )

    //Marcar mensaje como visto en BD
    private fun marcarMensajesComoLeidos() {
        val referenciaMensajes = FirebaseDatabase.getInstance().reference.child("chats")
        referenciaMensajes.orderByChild("receptor").equalTo(firebaseUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (mensajeSnapshot in snapshot.children) {
                        val mensaje = mensajeSnapshot.getValue(Chat::class.java)
                        if (mensaje != null && mensaje.getEmisor() == uid_usuario_seleccionado) {
                            // Marcar el mensaje como leído
                            mensajeSnapshot.ref.child("visto").setValue(true)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    override fun onResume() {
        super.onResume()
        receptorActivo = true
    }

    override fun onPause() {
        super.onPause()
        receptorActivo = false
    }

}