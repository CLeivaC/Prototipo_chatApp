package com.leiva.prototipo_chatapp.ui.Activities.InfoPerfilChat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.leiva.prototipo_chatapp.Data.database.AppDatabase
import com.leiva.prototipo_chatapp.Data.database.dao.ContactoDao
import com.leiva.prototipo_chatapp.Data.database.entities.ContactoEntity
import com.leiva.prototipo_chatapp.Data.database.entities.ImagenEntity
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.MyApp
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat
import com.leiva.prototipo_chatapp.ui.Activities.InfoPerfilChat.Adaptador.AdaptadorImagenes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InfoPerfilChatActivity : AppCompatActivity() {

    private lateinit var recyclerViewImagenesEnviadas: RecyclerView
    private lateinit var backgroundImage: ImageView
    private lateinit var imageViewPerfil: ShapeableImageView
    private lateinit var textViewNombre: TextView
    private lateinit var textViewTelefono: TextView
    private lateinit var textViewFraseUsuario: TextView

    private val firebaseUser = FirebaseAuth.getInstance().currentUser
    private lateinit var btn_llamar: Button

    var uid_usuario_visitado = ""
    private val REQUEST_CALL_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_info_perfil_chat)
        initComponents()
        intent = intent
        //Obtener "uid" del usuario visitado
        uid_usuario_visitado = intent.getStringExtra("uid").toString()
        cargarImagenesDeConversacion()
        leerInformacionUsuario()
        initListener()
    }

    private fun initComponents() {
        backgroundImage = findViewById(R.id.backgroundImage)
        imageViewPerfil = findViewById(R.id.imageViewPerfil)
        textViewNombre = findViewById(R.id.textViewNombre)
        textViewTelefono = findViewById(R.id.textViewTelefono)
        textViewFraseUsuario = findViewById(R.id.textViewFraseUsuario)
        btn_llamar = findViewById(R.id.btn_llamar)
        recyclerViewImagenesEnviadas = findViewById(R.id.recyclerViewImagenesEnviadas)
    }

    private fun initListener() {
        //Al hacer click, pedir al usuario permiso de "teléfono".
        btn_llamar.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CALL_PHONE),
                    REQUEST_CALL_PERMISSION
                )
            } else {
                realizarLlamada()
            }
        }
    }

    /*
     * Si ya tiene el permiso concedido,procede a realizar una llamada telefónica, de lo contrario, le saldrá al usuario un Toast.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                realizarLlamada()
            } else {
                Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Realizar llamada telefónica
    private fun realizarLlamada() {
        val numeroUsuario = textViewTelefono.text.toString()
        if (numeroUsuario.isEmpty()) {
            Toast.makeText(
                applicationContext,
                "El usuario no cuenta con un número telefónico",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$numeroUsuario")
            startActivity(intent)
        }
    }

    //Leer información del usuario a través de room para evitar llamadas de red.
    private fun leerInformacionUsuario() {
        lifecycleScope.launch {
            val contactoDao = MyApp.database.contactoDao()
            var contacto = contactoDao.getUserById(uid_usuario_visitado,firebaseUser!!.uid)


            if (contacto != null) {
                mostrarInformacionUsuario(contacto)
            }

            if (MyApp.isInternetAvailable(this@InfoPerfilChatActivity)) {
                actualizarInformacionUsuarioDesdeFirebase(contactoDao, contacto)
            }
        }
    }

    //Sincronización de ROOM con firebase en caso de cambios
    private fun actualizarInformacionUsuarioDesdeFirebase(
        contactoDao: ContactoDao,
        contacto: ContactoEntity?
    ) {
        val reference = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid_usuario_visitado)

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usuarioFirebase: Usuario? = snapshot.getValue(Usuario::class.java)
                if (usuarioFirebase != null) {
                    val contactoFirebase = ContactoEntity(
                        uid = usuarioFirebase.getUid(),
                        n_usuario = usuarioFirebase.getN_Usuario(),
                        telefono = usuarioFirebase.getTelefono(),
                        imagen = usuarioFirebase.getImagen(),
                        infoAdicional = usuarioFirebase.getInfoAdicional(),
                        imagenFondoPerfil = usuarioFirebase.getFondoPerfilUrl(),
                        ultimoMensajeTimestamp = usuarioFirebase.ultimoMensajeTimestamp,
                        oculto = usuarioFirebase.getOculto()!!,
                        ownerUid = firebaseUser!!.uid,
                        haTenidoConversacion = null
                    )

                    if (contacto == null || !sonContactosIguales(contacto, contactoFirebase)) {
                        lifecycleScope.launch {
                            contactoDao.actualizarInfoAdicional(
                                usuarioFirebase.getInfoAdicional(),
                                uid_usuario_visitado
                            )
                        }
                        mostrarInformacionUsuario(contactoFirebase)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    //Muestra la información del usuario en la vista
    private fun mostrarInformacionUsuario(usuario: ContactoEntity) {
        textViewNombre.text =
            UtilidadesChat.obtenerNombreDesdeTelefono(contentResolver, usuario.telefono!!)
        textViewTelefono.text = usuario.telefono
        textViewFraseUsuario.text = usuario.infoAdicional
        Glide.with(applicationContext).load(usuario.imagen)
            .placeholder(R.drawable.ic_item_usuario).into(imageViewPerfil)
        Glide.with(applicationContext).load(usuario.imagenFondoPerfil)
            .placeholder(R.drawable.ic_item_usuario).into(backgroundImage)
    }

    //Comprobación de si son 2 contactos iguales
    private fun sonContactosIguales(
        contacto1: ContactoEntity?,
        contacto2: ContactoEntity
    ): Boolean {
        return contacto1!!.n_usuario == contacto2.n_usuario &&
                contacto1.telefono == contacto2.telefono &&
                contacto1.imagen == contacto2.imagen &&
                contacto1.infoAdicional == contacto2.infoAdicional &&
                contacto1.imagenFondoPerfil == contacto2.imagenFondoPerfil
    }

    //Cargar imágenes en el recyclerView de cada conversación.
    private fun cargarImagenesDeConversacion() {
        val mi_uid = firebaseUser!!.uid
        val referenciaChats = FirebaseDatabase.getInstance().reference.child("chats")

        if (MyApp.isInternetAvailable(this@InfoPerfilChatActivity)) {
            referenciaChats.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    lifecycleScope.launch {
                        val imagenDao = MyApp.database.imagenDao()

                        // Limpia la tabla de imágenes en Room solo si hay datos nuevos de Firebase
                        if (snapshot.exists()) {
                            imagenDao.limpiarImagenes()
                        }
                        var listaImagenes: List<String> = emptyList()

                        for (chatSnapshot in snapshot.children) {
                            val mensaje = chatSnapshot.child("mensaje").getValue(String::class.java)
                            val emisor = chatSnapshot.child("emisor").getValue(String::class.java)
                            val receptor = chatSnapshot.child("receptor").getValue(String::class.java)
                            val imagenUrl = chatSnapshot.child("url").getValue(String::class.java)
                            val idMensaje = chatSnapshot.child("id_mensaje").getValue(String::class.java)

                            // Verifica si la imagen fue enviada dentro de la conversación entre los dos usuarios específicos
                            if (mensaje == "Se ha enviado la imagen" &&
                                ((emisor == uid_usuario_visitado && receptor == mi_uid) || (emisor == mi_uid && receptor == uid_usuario_visitado))
                            ) {
                                imagenUrl?.let {
                                    // Inserta la URL de la imagen en Room
                                    val imagenEntity = ImagenEntity(id_mensaje = idMensaje!!, url = it)
                                    withContext(Dispatchers.IO) {
                                        imagenDao.insertarImagen(imagenEntity)
                                    }
                                }
                            }
                        }
                        // Recupera las imágenes desde Room
                        listaImagenes = withContext(Dispatchers.IO) {
                            imagenDao.obtenerImagenesConversacion(mi_uid, uid_usuario_visitado).map { it.url }
                        }
                        // Si no hay imágenes nuevas de Firebase, carga las imágenes desde Room
                        if (listaImagenes.isEmpty()) {
                            listaImagenes = withContext(Dispatchers.IO) {
                                imagenDao.obtenerImagenesConversacion(mi_uid,uid_usuario_visitado).map { it.url }
                            }
                        }

                        // Configura el RecyclerView en el hilo principal
                        withContext(Dispatchers.Main) {
                            val layoutManager = LinearLayoutManager(this@InfoPerfilChatActivity, LinearLayoutManager.HORIZONTAL, false)
                            recyclerViewImagenesEnviadas.layoutManager = layoutManager
                            recyclerViewImagenesEnviadas.isNestedScrollingEnabled = false
                            recyclerViewImagenesEnviadas.adapter = AdaptadorImagenes(this@InfoPerfilChatActivity, listaImagenes)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Manejar el error, si es necesario
                }
            })
        } else {
            // Si no hay conexión a Internet, cargar imágenes desde Room
            lifecycleScope.launch {
                val imagenDao = MyApp.database.imagenDao()

                // Recupera las imágenes desde Room
                val listaImagenes = withContext(Dispatchers.IO) {
                    imagenDao.obtenerImagenesConversacion(mi_uid,uid_usuario_visitado).map { it.url }
                }

                // Configura el RecyclerView en el hilo principal
                withContext(Dispatchers.Main) {
                    val layoutManager = LinearLayoutManager(this@InfoPerfilChatActivity, LinearLayoutManager.HORIZONTAL, false)
                    recyclerViewImagenesEnviadas.layoutManager = layoutManager
                    recyclerViewImagenesEnviadas.isNestedScrollingEnabled = false
                    recyclerViewImagenesEnviadas.adapter = AdaptadorImagenes(this@InfoPerfilChatActivity, listaImagenes)
                }
            }
        }
    }




}
