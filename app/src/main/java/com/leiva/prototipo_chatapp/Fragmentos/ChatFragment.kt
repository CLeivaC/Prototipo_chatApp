package com.leiva.prototipo_chatapp.Fragmentos

import android.media.session.MediaSession.Token
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.leiva.prototipo_chatapp.Adaptador.AdaptadorChat
import com.leiva.prototipo_chatapp.Adaptador.AdaptadorContactos
import com.leiva.prototipo_chatapp.Modelo.Chat
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat


class ChatFragment : Fragment() {

    // Adaptador para la lista de contactos
    private var adaptadorContactos: AdaptadorContactos? = null

    // Lista de contactos con los que has tenido una conversación
    private var listaContactos: MutableList<Usuario> = mutableListOf()

    // RecyclerView para mostrar la lista de contactos
    private var recyclerView: RecyclerView? = null

    var firebaseUser = FirebaseAuth.getInstance().currentUser
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        // Inicializar el RecyclerView y su diseño
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView!!.layoutManager = LinearLayoutManager(context)

        // Obtener la lista de contactos con los que has tenido una conversación
        obtenerListaContactos()

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { tarea->
                if (tarea.isSuccessful){
                    if (tarea.result !=null && !TextUtils.isEmpty(tarea.result)){
                        val token : String = tarea.result!!
                        ActualizarToken(token)
                    }
                }

            }

        return view
    }

    private fun ActualizarToken(token: String) {

        val reference = FirebaseDatabase.getInstance().reference.child("Tokens")
        val token1 = com.leiva.prototipo_chatapp.Notificaciones.Token(token!!)
        reference.child(firebaseUser!!.uid).setValue(token1)
    }

    private fun obtenerListaContactos() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val referenciaListaMensajes = FirebaseDatabase.getInstance().reference.child("ListaMensajes").child(firebaseUser!!.uid)

        referenciaListaMensajes.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (usuarioSnapshot in snapshot.children) {
                    if(isAdded) {
                        val uidUsuario = usuarioSnapshot.key
                        if (uidUsuario != null) {
                            // Obtener detalles del usuario desde la base de datos
                            cargarDetallesUsuario(uidUsuario)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores de base de datos
            }
        })
    }

    private fun cargarDetallesUsuario(uidUsuario: String) {
        val referenciaUsuario = FirebaseDatabase.getInstance().reference.child("usuarios").child(uidUsuario)
        val contentResolver = requireContext().contentResolver

        referenciaUsuario.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val usuario = dataSnapshot.getValue(Usuario::class.java)
                val numeroTelefonoUsuario = usuario?.getTelefono()
                if (usuario != null) {
                    val nombreContacto = UtilidadesChat.obtenerNombreDesdeTelefono(contentResolver,numeroTelefonoUsuario!!)
                    usuario?.setN_Usuario(nombreContacto)

                    // Agregar usuario a la lista de contactos
                    listaContactos.add(usuario)
                    // Actualizar el adaptador del RecyclerView
                    adaptadorContactos?.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores de base de datos
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar y configurar el adaptador de contactos
        adaptadorContactos = AdaptadorContactos(requireContext(), listaContactos)

        // Configurar el RecyclerView
        recyclerView!!.adapter = adaptadorContactos
    }
}