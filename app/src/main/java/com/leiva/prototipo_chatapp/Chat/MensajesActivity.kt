package com.leiva.prototipo_chatapp.Chat

import android.app.Activity
import android.app.ProgressDialog
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
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.leiva.prototipo_chatapp.Adaptador.AdaptadorChat
import com.leiva.prototipo_chatapp.Modelo.Chat
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat

class MensajesActivity : AppCompatActivity() {


    private var receptorActivo: Boolean = false

    private lateinit var imagen_perfil_chat: ImageView

    private lateinit var N_usuario_chat: TextView

    private lateinit var IB_Adjuntar: ImageButton

    var uid_usuario_seleccionado: String = ""

    private lateinit var ET_mensaje: EditText
    private lateinit var IB_enviar: ImageButton

    var firebaseUser : FirebaseUser?=null

    private var imagenUri: Uri?=null

    lateinit var RV_chats : RecyclerView
    var chatAdapter: AdaptadorChat?=null
    var chatList: List<Chat>?=null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mensajes)
        initComponents()
        obtenerUID()
        leerInfoUsuarioSeleccionado()
        initListeners()
        abrirConversacion()

    }

    private fun initComponents(){
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


    }

    private fun initListeners(){
        IB_enviar.setOnClickListener {
            val mensaje = ET_mensaje.text.toString()
            if(mensaje.isEmpty()){
                Toast.makeText(applicationContext,"Por favor ingrese un mensaje",Toast.LENGTH_SHORT).show()
            }else{
                enviarMensaje(firebaseUser!!.uid,uid_usuario_seleccionado,mensaje)
                ET_mensaje.setText("")
            }
        }

        IB_Adjuntar.setOnClickListener {
            abrirGaleria()
        }
    }


    private fun obtenerUID(){
        intent = intent
        uid_usuario_seleccionado = intent.getStringExtra("uid_usuario").toString()
    }

    private fun leerInfoUsuarioSeleccionado() {
        val reference = FirebaseDatabase.getInstance().reference.child("usuarios").child(uid_usuario_seleccionado)
        var imagenReceptor:String?=null
        val contentResolver = contentResolver
        reference.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val usuario: Usuario? = snapshot.getValue(Usuario::class.java)
                N_usuario_chat.text = UtilidadesChat.obtenerNombreDesdeTelefono(contentResolver,usuario!!.getTelefono()!!)
                imagenReceptor = usuario.getImagen().toString()
                Glide.with(applicationContext).load(usuario.getImagen()).placeholder(R.drawable.ic_item_usuario).into(imagen_perfil_chat)
                obtenerImagenUsuarioActual(object : ImagenUsuarioCallback {
                    override fun onImagenObtenida(imagen: String) {
                        RecuperarMensajes(firebaseUser!!.uid, uid_usuario_seleccionado, imagenReceptor, imagen)
                    }
                })
            }
            override fun onCancelled(error: DatabaseError) {
                //TODO("Not yet implemented")
            }
        })
    }

    private fun RecuperarMensajes(EmisorUid: String, ReceptorUid: String, ReceptorImagen: String?, ImagenActual: String?) {
        chatList = ArrayList()
        val reference = FirebaseDatabase.getInstance().reference.child("chats")
        reference.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                (chatList as ArrayList<Chat>).clear()
                for(sn in snapshot.children){
                    val chat = sn.getValue(Chat::class.java)
                    if(chat!!.getReceptor().equals(EmisorUid) && chat.getEmisor().equals(ReceptorUid)
                        || chat.getReceptor().equals(ReceptorUid) && chat.getEmisor().equals(EmisorUid)) {
                        (chatList as ArrayList<Chat>).add(chat)
                    }
                }
                chatAdapter = AdaptadorChat(this@MensajesActivity, (chatList as ArrayList<Chat>), ReceptorImagen!!, ImagenActual!!)
                RV_chats.adapter = chatAdapter

                if (receptorActivo) {
                    marcarMensajesComoLeidos()
                }
            }

            override fun onCancelled(error: DatabaseError) {
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
                        }
                    } else {
                        // El usuario no tiene datos en la base de datos
                        callback.onImagenObtenida("")  // Otra opción: callback.onImagenNoDisponible()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejar errores de la base de datos, si es necesario
                    callback.onImagenObtenida("")  // Otra opción: callback.onImagenNoDisponible()
                }
            })
        } else {
            // El usuario no está autenticado
            callback.onImagenObtenida("")  // Otra opción: callback.onImagenNoDisponible()
        }
    }

// Llamada a la función con una devolución de llamada


    private fun enviarMensaje(uid_emisor: String,uid_receptor:String,mensaje:String) {
        val reference = FirebaseDatabase.getInstance().reference
        val mensajeKey = reference.push().key
        val infoMensaje = HashMap<String,Any?>()
        infoMensaje["id_mensaje"] = mensajeKey
        infoMensaje["emisor"] = uid_emisor
        infoMensaje["receptor"] = uid_receptor
        infoMensaje["mensaje"] = mensaje
        infoMensaje["url"] = ""
        infoMensaje["visto"] = false
        reference.child("chats").child(mensajeKey!!).setValue(infoMensaje).addOnCompleteListener {task->
            if(task.isSuccessful){
                marcarMensajesComoLeidos()
                val listaMensajesEmisor = FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                    .child(firebaseUser!!.uid).child(uid_usuario_seleccionado)
                listaMensajesEmisor.addListenerForSingleValueEvent(object :ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(!snapshot.exists()){
                            listaMensajesEmisor.child("uid").setValue(uid_usuario_seleccionado)
                        }

                        val listaMensajesReceptor = FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                            .child(uid_usuario_seleccionado).child(firebaseUser!!.uid)
                        listaMensajesReceptor.child("uid").setValue(firebaseUser!!.uid)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        // Not implemented
                    }
                })
            }
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        GaleriaARL.launch(intent)
    }

    private val GaleriaARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> {resultado->

            //Descargar URL de la imagen
            if(resultado.resultCode == RESULT_OK){
                val data = resultado.data
                imagenUri = data!!.data

                val cargandoImagen = ProgressDialog(this@MensajesActivity)
                cargandoImagen.setMessage("Por favor espere,la imagen se está enviando")
                cargandoImagen.setCanceledOnTouchOutside(false)
                cargandoImagen.show()

                val carpetaImagenes = FirebaseStorage.getInstance().reference.child("Imagenes de mensajes")
                val reference = FirebaseDatabase.getInstance().reference
                val idMensaje = reference.push().key
                val nombreImagen = carpetaImagenes.child("${idMensaje}.jpg")

                val uploadTask: StorageTask<*>
                uploadTask = nombreImagen.putFile(imagenUri!!)
                uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot,Task<Uri>>{task->
                    if(!task.isSuccessful){
                        task.exception?.let {
                            throw it
                        }
                    }
                    return@Continuation nombreImagen.downloadUrl

                    //Añadir datos del mensaje de imagen a la Base de datos.
                }).addOnCompleteListener { task->
                    if(task.isSuccessful){
                        cargandoImagen.dismiss()
                        val downloadUrl = task.result
                        val url = downloadUrl.toString()

                        val infoMensajeImagen = HashMap<String,Any?>()
                        infoMensajeImagen["id_mensaje"] = idMensaje
                        infoMensajeImagen["emisor"] = firebaseUser!!.uid
                        infoMensajeImagen["mensaje"] = "Se ha enviado la imagen"
                        infoMensajeImagen["receptor"] = uid_usuario_seleccionado
                        infoMensajeImagen["url"] = url
                        infoMensajeImagen["visto"] = false

                        reference.child("chats").child(idMensaje!!).setValue(infoMensajeImagen).addOnCompleteListener {task->
                            if(task.isSuccessful){
                                val listaMensajesEmisor = FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                                    .child(firebaseUser!!.uid).child(uid_usuario_seleccionado)

                                listaMensajesEmisor.addListenerForSingleValueEvent(object :ValueEventListener{
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if(!snapshot.exists()){
                                            listaMensajesEmisor.child("uid").setValue(uid_usuario_seleccionado)
                                        }

                                        val listaMensajesReceptor = FirebaseDatabase.getInstance().reference.child("ListaMensajes")
                                            .child(uid_usuario_seleccionado).child(firebaseUser!!.uid)
                                        listaMensajesReceptor.child("uid").setValue(firebaseUser!!.uid)
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        TODO("Not yet implemented")
                                    }

                                })
                            }

                        }
                        Toast.makeText(applicationContext,"La imagen se ha enviado con éxito",Toast.LENGTH_SHORT).show()
                    }

                }
            }
            else{
                Toast.makeText(applicationContext,"Cancelado por el usuario",Toast.LENGTH_SHORT).show()
            }
        }
    )

    private fun marcarMensajesComoLeidos() {
        val referenciaMensajes = FirebaseDatabase.getInstance().reference.child("chats")
        referenciaMensajes.orderByChild("receptor").equalTo(firebaseUser!!.uid).addListenerForSingleValueEvent(object : ValueEventListener {
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
                // Manejar errores de base de datos, si es necesario
            }
        })
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