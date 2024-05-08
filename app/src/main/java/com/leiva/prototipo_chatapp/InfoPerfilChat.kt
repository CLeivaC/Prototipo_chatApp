package com.leiva.prototipo_chatapp

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat

class InfoPerfilChat : AppCompatActivity() {


    private lateinit var imageViewPerfil: ShapeableImageView

    private lateinit var textViewNombre:TextView
    private lateinit var textViewTelefono:TextView


    var uid_usuario_visitado = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_info_perfil_chat)
        initComponents()
        intent = intent
        uid_usuario_visitado = intent.getStringExtra("uid").toString()
        leerInformacionUsuario()
    }

    private fun initComponents(){
        imageViewPerfil = findViewById(R.id.imageViewPerfil)
        textViewNombre = findViewById(R.id.textViewNombre)
        textViewTelefono = findViewById(R.id.textViewTelefono)
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
                Glide.with(applicationContext).load(usuario.getImagen())
                    .placeholder(R.drawable.ic_item_usuario).into(imageViewPerfil)

            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

}