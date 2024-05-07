package com.leiva.prototipo_chatapp.Chat

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.leiva.prototipo_chatapp.Adaptador.AdaptadorChat
import com.leiva.prototipo_chatapp.Fragmentos.PerfilFragment
import com.leiva.prototipo_chatapp.InfoPerfilChat
import com.leiva.prototipo_chatapp.Modelo.Chat
import com.leiva.prototipo_chatapp.Modelo.Contacto
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.MyApp
import com.leiva.prototipo_chatapp.Notificaciones.APIService
import com.leiva.prototipo_chatapp.Notificaciones.Client
import com.leiva.prototipo_chatapp.Notificaciones.Data
import com.leiva.prototipo_chatapp.Notificaciones.MyResponse
import com.leiva.prototipo_chatapp.Notificaciones.Sender
import com.leiva.prototipo_chatapp.Notificaciones.Token
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Response
import javax.security.auth.callback.Callback

class MensajesActivity : AppCompatActivity() {

    private lateinit var toolbar_chat : androidx.appcompat.widget.Toolbar

    private var receptorActivo: Boolean = false

    private lateinit var imagen_perfil_chat: ImageView

    private lateinit var N_usuario_chat: TextView

    private lateinit var IB_Adjuntar: ImageButton

    var uid_usuario_seleccionado: String = ""

    private lateinit var ET_mensaje: EditText
    private lateinit var IB_enviar: ImageButton

    var firebaseUser: FirebaseUser? = null

    private var imagenUri: Uri? = null

    lateinit var RV_chats: RecyclerView
    var chatAdapter: AdaptadorChat? = null
    var chatList: List<Chat>? = null

    var notificar = false
    var apiService: APIService? = null

    val timestamp = ServerValue.TIMESTAMP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mensajes)
        initComponents()
        obtenerUID()
        leerInfoUsuarioSeleccionado()
        initListeners()
        abrirConversacion()


    }

    private fun initComponents() {

        toolbar_chat = findViewById(R.id.toolbar_chat)
        imagen_perfil_chat = findViewById(R.id.imagen_perfil_chat)
        N_usuario_chat = findViewById(R.id.N_usuario_chat)

        ET_mensaje = findViewById(R.id.ET_mensaje)
        IB_enviar = findViewById(R.id.IB_Enviar)
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
        IB_enviar.setOnClickListener {
            notificar = true
            val mensaje = ET_mensaje.text.toString()
            if (mensaje.isEmpty() || mensaje.isBlank()) {
                Toast.makeText(
                    applicationContext,
                    "Por favor ingrese un mensaje",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                enviarMensaje(firebaseUser!!.uid, uid_usuario_seleccionado, mensaje)
                ET_mensaje.setText("")
            }
        }

        IB_Adjuntar.setOnClickListener {
            notificar = true
            abrirGaleria()
        }

        toolbar_chat.setOnClickListener {
            val intent = Intent(applicationContext,InfoPerfilChat::class.java)
            intent.putExtra("uid",uid_usuario_seleccionado)
            startActivity(intent)
        }
    }


    private fun obtenerUID() {
        intent = intent
        uid_usuario_seleccionado = intent.getStringExtra("uid_usuario").toString()

        // Log del UID obtenido
        Log.d(TAG, "UID del usuario seleccionado: $uid_usuario_seleccionado")
    }

    private fun leerInfoUsuarioSeleccionado() {
        val reference = FirebaseDatabase.getInstance().reference.child("usuarios")
            .child(uid_usuario_seleccionado)
        var imagenReceptor: String? = null
        val contentResolver = contentResolver
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usuario: Usuario? = snapshot.getValue(Usuario::class.java)
                val telefonoUsuario = usuario!!.getTelefono()
                val imagenUsuario = usuario.getImagen()

                // Log de la información del usuario seleccionado
                Log.d(TAG, "Información del usuario seleccionado:")
                Log.d(TAG, "Nombre: ${UtilidadesChat.obtenerNombreDesdeTelefono(contentResolver, telefonoUsuario!!)}")
                Log.d(TAG, "Imagen: $imagenUsuario")

                N_usuario_chat.text = UtilidadesChat.obtenerNombreDesdeTelefono(
                    contentResolver,
                    telefonoUsuario
                )
                imagenReceptor = imagenUsuario.toString()
                Glide.with(applicationContext).load(imagenUsuario)
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

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al leer la información del usuario seleccionado: $error")
                //TODO("Not yet implemented")
            }
        })
    }


    private fun RecuperarMensajes(
        EmisorUid: String,
        ReceptorUid: String,
        ReceptorImagen: String?,
        ImagenActual: String?
    ) {
        chatList = ArrayList()
        val reference = FirebaseDatabase.getInstance().reference.child("chats")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                (chatList as ArrayList<Chat>).clear()
                for (sn in snapshot.children) {
                    val chat = sn.getValue(Chat::class.java)
                    if (chat!!.getReceptor().equals(EmisorUid) && chat.getEmisor()
                            .equals(ReceptorUid)
                        || chat.getReceptor().equals(ReceptorUid) && chat.getEmisor()
                            .equals(EmisorUid)
                    ) {
                        (chatList as ArrayList<Chat>).add(chat)
                    }
                }
                chatAdapter = AdaptadorChat(
                    this@MensajesActivity,
                    (chatList as ArrayList<Chat>),
                    ReceptorImagen!!,
                    ImagenActual!!
                )
                RV_chats.adapter = chatAdapter

                // Log de los mensajes recuperados
                for (chat in chatList as ArrayList<Chat>) {
                    Log.d(TAG, "Mensaje: ${chat.getMensaje()}")
                }

                if (receptorActivo) {
                    marcarMensajesComoLeidos()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al recuperar mensajes: $error")
                // Manejar errores de base de datos, si es necesario
            }
        })
    }

    interface ImagenUsuarioCallback {
        fun onImagenObtenida(imagen: String)
    }

    private fun obtenerImagenUsuarioActual(callback: ImagenUsuarioCallback) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        if (firebaseUser != null) {
            val userId = firebaseUser.uid
            val databaseReference = FirebaseDatabase.getInstance().reference.child("usuarios").child(userId)

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val usuario = snapshot.getValue(Usuario::class.java)

                        if (usuario != null) {
                            val imagen = usuario.getImagen().toString()
                            Log.d("Glide", "Cargando imagen derecha: $imagen")
                            callback.onImagenObtenida(imagen!!)
                        } else {
                            Log.e("Glide", "Error: Usuario es nulo")
                            callback.onImagenObtenida("") // Otra opción: callback.onImagenNoDisponible()
                        }
                    } else {
                        Log.e("Glide", "Error: No existe el snapshot")
                        callback.onImagenObtenida("") // Otra opción: callback.onImagenNoDisponible()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejar errores de la base de datos, si es necesario
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

// Llamada a la función con una devolución de llamada



    private fun enviarMensaje(uid_emisor: String, uid_receptor: String, mensaje: String) {
        val reference = FirebaseDatabase.getInstance().reference
        val mensajeKey = reference.push().key
        val infoMensaje = HashMap<String, Any?>()
        infoMensaje["id_mensaje"] = mensajeKey
        infoMensaje["emisor"] = uid_emisor
        infoMensaje["receptor"] = uid_receptor
        infoMensaje["mensaje"] = mensaje // Agregar el mensaje a la información del mensaje
        infoMensaje["url"] = ""
        infoMensaje["visto"] = false
        infoMensaje["hora"] = timestamp

        var telefonoEmisor = ""

        val referenciaEmisor = FirebaseDatabase.getInstance().reference.child("usuarios").child(uid_emisor)
        referenciaEmisor.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usuarioEmisor = snapshot.getValue(Usuario::class.java)
                telefonoEmisor = usuarioEmisor!!.getTelefono()
                Log.d(TAG, "Número de teléfono del emisor: $telefonoEmisor")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al obtener datos del emisor: $error")
            }
        })

        // Verificar el número de teléfono del emisor en la lista de contactos del receptor
        val contactosReference = FirebaseDatabase.getInstance().reference
            .child("usuarios").child(uid_receptor).child("contactos")
        contactosReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var nombreContacto = ""

                // Iterar sobre los contactos del receptor
                for (contactoSnapshot in snapshot.children) {
                    val contacto = contactoSnapshot.getValue(Contacto::class.java)
                    val telefonoContacto = contacto?.getTelefono()

                    // Verificar si el número de teléfono del emisor está en los contactos del receptor
                    if (telefonoContacto != null && telefonoContacto==telefonoEmisor) {
                        nombreContacto = contacto.getNombre()
                        Log.d(TAG, "Nombre encontrado en la lista de contactos del receptor: $nombreContacto")
                        break // Terminar el bucle si se encuentra el contacto
                    }
                }

                // Enviar la notificación con el nombre encontrado (o nombre vacío si no se encontró)
                Log.d(TAG, "Enviando notificación con nombre: $nombreContacto y mensaje: $mensaje")
                //enviarNotificacion(uid_receptor, nombreContacto, mensaje)
                estadoNotificacionesReceptor(uid_receptor,nombreContacto,mensaje)

                // Guardar el mensaje en la base de datos
                reference.child("chats").child(mensajeKey!!).setValue(infoMensaje)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
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
                                    // Not implemented
                                }
                            })
                        }
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al obtener datos de los contactos del usuario receptor: $error")
                // Enviar notificación con nombre vacío en caso de error
                enviarNotificacion(uid_receptor, "", mensaje)

                // Guardar el mensaje en la base de datos
                reference.child("chats").child(mensajeKey!!).setValue(infoMensaje)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
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
                                    // Not implemented
                                }
                            })
                        }
                    }
            }
        })
    }

    private fun estadoNotificacionesReceptor(uidReceptor: String, nombreContacto: String, mensaje: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("usuarios").child(uidReceptor)

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usuario = snapshot.getValue(Usuario::class.java)
                val notificacionesActivas = usuario?.getNotificaciones() ?: false

                // Verificar el estado del interruptor de notificaciones del receptor
                if (!notificacionesActivas) {
                    // Si las notificaciones están activas, enviar la notificación
                    Log.d(TAG, "Notificaciones activas para $nombreContacto, enviando notificación...")
                    enviarNotificacion(uidReceptor, nombreContacto, mensaje)
                } else {
                    // Si las notificaciones están desactivadas, no enviar la notificación
                    Log.d(TAG, "Las notificaciones del receptor $nombreContacto están desactivadas")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al leer el estado de las notificaciones del receptor: ${error.message}")
            }
        })
    }


    private fun enviarNotificacion(uidReceptor: String, nUsuario: String, mensaje: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("Tokens")
        val query = reference.orderByKey().equalTo(uidReceptor)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (dataSnapshot in snapshot.children) {
                    val token: Token? = dataSnapshot.getValue(Token::class.java)
                    val data = Data(
                        firebaseUser!!.uid,
                        R.mipmap.ic_launcher,
                        "$nUsuario: $mensaje",
                        "nuevo mensaje",
                        uid_usuario_seleccionado

                    )

                    val sender = Sender(data!!, token!!.getToken().toString())
                    apiService!!.sendNotification(sender)
                        .enqueue(object : retrofit2.Callback<MyResponse> {
                            override fun onResponse(
                                call: Call<MyResponse>,
                                response: Response<MyResponse>
                            ) {
                                if (response.code() == 200) {
                                    if (response.body()!!.success !== 1) {
                                        Toast.makeText(
                                            applicationContext,
                                            "Algo ha salido mal",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                            override fun onFailure(call: Call<MyResponse>, t: Throwable) {
                                TODO("Not yet implemented")
                            }

                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun abrirGaleria() {
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

                val cargandoImagen = ProgressDialog(this@MensajesActivity)
                cargandoImagen.setMessage("Por favor espere,la imagen se está enviando")
                cargandoImagen.setCanceledOnTouchOutside(false)
                cargandoImagen.show()

                val carpetaImagenes =
                    FirebaseStorage.getInstance().reference.child("Imagenes de mensajes")
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
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val usuario = snapshot.getValue(Usuario::class.java)
                                            if (notificar) {
                                                enviarNotificacion(
                                                    uid_usuario_seleccionado,
                                                    usuario!!.getN_Usuario(),
                                                    "Se ha enviado la imagen"
                                                )
                                            }
                                            notificar = false
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            TODO("Not yet implemented")
                                        }

                                    })
                                }
                            }

                        reference.child("chats").child(idMensaje!!).setValue(infoMensajeImagen)
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
                                                listaMensajesEmisor.child("uid")
                                                    .setValue(uid_usuario_seleccionado)
                                            }

                                            val listaMensajesReceptor =
                                                FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                                                    .child(uid_usuario_seleccionado)
                                                    .child(firebaseUser!!.uid)
                                            listaMensajesReceptor.child("uid")
                                                .setValue(firebaseUser!!.uid)
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            TODO("Not yet implemented")
                                        }

                                    })
                                }

                            }
                        Toast.makeText(
                            applicationContext,
                            "La imagen se ha enviado con éxito",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "La imagen se ha enviado con éxito")
                    }

                }
            } else {
                Toast.makeText(applicationContext, "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    )



    private fun marcarMensajesComoLeidos() {
        lifecycleScope.launch {
            val referenciaMensajes = FirebaseDatabase.getInstance().reference.child("chats")
            referenciaMensajes.orderByChild("receptor").equalTo(firebaseUser!!.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (mensajeSnapshot in snapshot.children) {
                            val mensaje = mensajeSnapshot.getValue(Chat::class.java)
                            if (mensaje != null && mensaje.getEmisor() == uid_usuario_seleccionado) {
                                val emisorId = mensaje.getEmisor()
                                val receptorId = mensaje.getReceptor()

                                // Verificar "oculto" del emisor
                                val emisorRef = FirebaseDatabase.getInstance().reference
                                    .child("usuarios").child(emisorId!!)
                                emisorRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(emisorSnapshot: DataSnapshot) {
                                        val emisorUsuario = emisorSnapshot.getValue(Usuario::class.java)
                                        val emisorOculto = emisorUsuario?.getOculto() ?: false
                                        Log.d(TAG, "Oculto del emisor ($emisorId): $emisorOculto")

                                        // Verificar "oculto" del receptor
                                        val receptorRef = FirebaseDatabase.getInstance().reference
                                            .child("usuarios").child(receptorId!!)
                                        receptorRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(receptorSnapshot: DataSnapshot) {
                                                val receptorUsuario = receptorSnapshot.getValue(Usuario::class.java)
                                                val receptorOculto = receptorUsuario?.getOculto() ?: false
                                                Log.d(TAG, "Oculto del receptor ($receptorId): $receptorOculto")

                                                // Verificar si alguno de los dos está en true
                                                if (emisorOculto || receptorOculto) {
                                                    // Al menos uno está en true, no marcar como leído
                                                    Log.d(TAG, "Al menos uno de los usuarios está oculto. No marcar como leído.")
                                                } else {
                                                    // Ambos están en false, marcar como leído
                                                    mensajeSnapshot.ref.child("visto").setValue(true)
                                                    Log.d(TAG, "Mensaje marcado como leído: ${mensaje.getId_Mensaje()}")
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                Log.e(TAG, "Error al obtener oculto del receptor ($receptorId): ${error.message}")
                                            }
                                        })
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e(TAG, "Error al obtener oculto del emisor ($emisorId): ${error.message}")
                                    }
                                })
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error al obtener mensajes: ${error.message}")
                    }
                })
        }
    }



    private fun abrirConversacion() {
        // Después de abrir la conversación, llamar a la función para marcar mensajes como leídos
        marcarMensajesComoLeidos()

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