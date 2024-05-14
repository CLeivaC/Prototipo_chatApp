package com.leiva.prototipo_chatapp.ui.Fragments.Chat

import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.leiva.prototipo_chatapp.Modelo.Chat
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat
import com.leiva.prototipo_chatapp.ui.Fragments.Contactos.Adaptador.AdaptadorContactos
import com.leiva.prototipo_chatapp.ui.Fragments.Contactos.Adaptador.FragmentType

class ChatFragment : Fragment(), FragmentType {

    private val READ_CONTACTS_PERMISSION_REQUEST_CODE = 1

    private var adaptadorContactos: AdaptadorContactos? = null
    private var listaContactos: MutableList<Usuario> = mutableListOf()
    private var recyclerView: RecyclerView? = null
    private var firebaseUser = FirebaseAuth.getInstance().currentUser


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        checkContactPermission()

        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)

        // Espera hasta que el Toolbar esté completamente inicializado antes de configurar los márgenes

        initViews()



        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let { token ->
                        updateToken(token)
                    }
                }
            }

        return view
    }

    private fun initViews() {
        recyclerView!!.layoutManager = LinearLayoutManager(context)
        adaptadorContactos = AdaptadorContactos(requireContext(), listaContactos, this)
        recyclerView!!.adapter = adaptadorContactos
    }

    private fun checkContactPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchContactList()
        } else {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                READ_CONTACTS_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun updateToken(token: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("Tokens")
        val token1 = com.leiva.prototipo_chatapp.Notificaciones.Token(token)
        firebaseUser?.uid?.let { reference.child(it).setValue(token1) }
    }

    private fun fetchContactList() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        firebaseUser?.uid?.let {
            val referenciaListaMensajes =
                FirebaseDatabase.getInstance().reference.child("ListaMensajes").child(it)

            referenciaListaMensajes.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tempMap: MutableMap<String, Usuario> = mutableMapOf()

                    snapshot.children.forEach { usuarioSnapshot ->
                        val uidUsuario = usuarioSnapshot.key
                        uidUsuario?.let { uid ->
                            cargarDetallesUsuario(uid) { usuario ->
                                tempMap[usuario.getUid()] = usuario

                                if (tempMap.size == snapshot.childrenCount.toInt()) {
                                    listaContactos.clear()
                                    listaContactos.addAll(tempMap.values)
                                    listaContactos.sortByDescending { it.ultimoMensajeTimestamp }
                                    adaptadorContactos?.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    private fun cargarDetallesUsuario(uidUsuario: String, callback: (Usuario) -> Unit) {

        if (!isAdded) {
            return
        }
        val referenciaUsuario =
            FirebaseDatabase.getInstance().reference.child("usuarios").child(uidUsuario)
        val contentResolver = requireContext().contentResolver

        referenciaUsuario.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val usuario = dataSnapshot.getValue(Usuario::class.java)
                val numeroTelefonoUsuario = usuario?.getTelefono()
                numeroTelefonoUsuario?.let { numero ->
                    usuario.setN_Usuario(
                        UtilidadesChat.obtenerNombreDesdeTelefono(contentResolver, numero)
                    )
                    obtenerUltimoMensaje(uidUsuario) { ultimoMensajeTimestamp ->
                        usuario.ultimoMensajeTimestamp = ultimoMensajeTimestamp
                        callback(usuario)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun obtenerUltimoMensaje(uidUsuario: String, callback: (Long) -> Unit) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        firebaseUser?.let {
            val reference = FirebaseDatabase.getInstance().reference.child("chats")
            reference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var ultimoMensajeTimestamp = 0L

                    snapshot.children.forEach { dataSnapshot ->
                        val chat = dataSnapshot.getValue(Chat::class.java)
                        chat?.let {
                            if ((it.getEmisor() == firebaseUser.uid && it.getReceptor() == uidUsuario) ||
                                (it.getEmisor() == uidUsuario && it.getReceptor() == firebaseUser.uid)
                            ) {
                                val timestamp = it.getHora()
                                if (timestamp > ultimoMensajeTimestamp) {
                                    ultimoMensajeTimestamp = timestamp
                                }
                            }
                        }
                    }
                    callback(ultimoMensajeTimestamp)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }
    }

    override fun isChatFragment(): Boolean {
        return true
    }
}
