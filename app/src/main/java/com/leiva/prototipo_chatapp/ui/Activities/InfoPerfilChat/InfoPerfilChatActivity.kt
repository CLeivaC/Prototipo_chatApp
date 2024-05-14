package com.leiva.prototipo_chatapp.ui.Activities.InfoPerfilChat

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat
import com.leiva.prototipo_chatapp.ui.Activities.InfoPerfilChat.Adaptador.AdaptadorImagenes

class InfoPerfilChatActivity : AppCompatActivity() {

    private lateinit var recyclerViewImagenesEnviadas: RecyclerView

    private lateinit var backgroundImage:ImageView
    private lateinit var imageViewPerfil: ShapeableImageView

    private lateinit var textViewNombre:TextView
    private lateinit var textViewTelefono:TextView

    private lateinit var textViewFraseUsuario:TextView

    private val firebaseUser = FirebaseAuth.getInstance().currentUser

    private lateinit var btn_llamar:Button

    var uid_usuario_visitado = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_info_perfil_chat)
        initComponents()
        intent = intent
        uid_usuario_visitado = intent.getStringExtra("uid").toString()
        leerInformacionUsuario()
        cargarImagenesDeConversacion()
        initListener()
    }

    private fun initComponents(){
        backgroundImage = findViewById(R.id.backgroundImage)
        imageViewPerfil = findViewById(R.id.imageViewPerfil)
        textViewNombre = findViewById(R.id.textViewNombre)
        textViewTelefono = findViewById(R.id.textViewTelefono)
        textViewFraseUsuario = findViewById(R.id.textViewFraseUsuario)

        btn_llamar = findViewById(R.id.btn_llamar)

        recyclerViewImagenesEnviadas = findViewById(R.id.recyclerViewImagenesEnviadas)
    }

    private fun initListener(){
        btn_llamar.setOnClickListener {
            RealizarLlamada()
        }
    }

    private fun RealizarLlamada() {
        val numeroUsuario = textViewTelefono.text.toString()
        if(numeroUsuario.isEmpty()){
            Toast.makeText(applicationContext,"El usuario no cuenta con un número telefónico",Toast.LENGTH_SHORT)
        }else{
            val intent = Intent(Intent.ACTION_CALL)
            intent.setData(Uri.parse("tel:$numeroUsuario"))
            startActivity(intent)
        }
    }

    private fun leerInformacionUsuario(){
        val reference = FirebaseDatabase.getInstance().reference
            .child("usuarios")
            .child(uid_usuario_visitado)

        reference.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
               val usuario: Usuario? = snapshot.getValue(Usuario::class.java)

                textViewNombre.text = UtilidadesChat.obtenerNombreDesdeTelefono(contentResolver,usuario!!.getTelefono())
                textViewTelefono.text = usuario.getTelefono()
                textViewFraseUsuario.text = usuario.getInfoAdicional()
                Glide.with(applicationContext).load(usuario.getFondoPerfilUrl())
                    .placeholder(R.drawable.ic_item_usuario).into(backgroundImage)

                Glide.with(applicationContext).load(usuario.getImagen())
                    .placeholder(R.drawable.ic_item_usuario).into(imageViewPerfil)

            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun cargarImagenesDeConversacion() {
        val mi_uid = firebaseUser!!.uid
        val referenciaChats = FirebaseDatabase.getInstance().reference.child("chats")
        val listaImagenes = mutableListOf<String>()

        referenciaChats.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (chatSnapshot in snapshot.children) {
                    val mensaje = chatSnapshot.child("mensaje").getValue(String::class.java)
                    val emisor = chatSnapshot.child("emisor").getValue(String::class.java)
                    val receptor = chatSnapshot.child("receptor").getValue(String::class.java)
                    val imagenUrl = chatSnapshot.child("url").getValue(String::class.java)

                    // Verifica si la imagen fue enviada dentro de la conversación entre los dos usuarios específicos
                    if (mensaje == "Se ha enviado la imagen" &&
                        ((emisor == uid_usuario_visitado && receptor == mi_uid) || (emisor == mi_uid && receptor == uid_usuario_visitado))
                    ) {
                        imagenUrl?.let {
                            listaImagenes.add(imagenUrl)
                        }
                    }
                }

                // Configura el adaptador del RecyclerView con las URLs de las imágenes obtenidas
                val layoutManager = LinearLayoutManager(this@InfoPerfilChatActivity, LinearLayoutManager.HORIZONTAL, false)
                recyclerViewImagenesEnviadas.layoutManager = layoutManager
                recyclerViewImagenesEnviadas.isNestedScrollingEnabled = false
                recyclerViewImagenesEnviadas.adapter = AdaptadorImagenes(this@InfoPerfilChatActivity,listaImagenes)
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar el error, si es necesario
            }
        })
    }



}