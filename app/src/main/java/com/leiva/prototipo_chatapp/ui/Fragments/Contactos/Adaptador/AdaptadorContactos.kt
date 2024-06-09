package com.leiva.prototipo_chatapp.ui.Fragments.Contactos.Adaptador

import android.content.Context
import android.content.Context.*
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.leiva.prototipo_chatapp.Data.database.entities.ContactoEntity
import com.leiva.prototipo_chatapp.ui.Activities.Mensajes.MensajesActivity
import com.leiva.prototipo_chatapp.Modelo.Chat
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.MyApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Handler

interface FragmentType {
    fun isChatFragment(): Boolean
}

class AdaptadorContactos (context: Context, listaUsuariosFiltrada: List<ContactoEntity>,private val fragmentType: FragmentType)
    : RecyclerView.Adapter<AdaptadorContactos.ViewHolder?>(){

    private val context: Context
    private var listaUsuariosFiltrada: List<ContactoEntity>
    var ultimoMensaje:String = ""
    init {
        this.context = context
        this.listaUsuariosFiltrada = listaUsuariosFiltrada
    }
    fun actualizarLista(nuevaLista: List<ContactoEntity>) {
        listaUsuariosFiltrada = nuevaLista
        notifyDataSetChanged()
    }
    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var nombre_usuario: TextView
        var imagen_usuario:ImageView
        var imagen_online:ImageView
        var imagen_offline:ImageView
        var TXT_ultimoMensaje:TextView
        var line: View // Agregar referencia a la línea
        var item_numero_mensajes:TextView

        init {
            //Inicializar componentes
            nombre_usuario = itemView.findViewById(R.id.item_nombre_usuario)
            imagen_usuario = itemView.findViewById(R.id.Item_imagen)
            imagen_online = itemView.findViewById(R.id.imagen_online)
            imagen_offline= itemView.findViewById(R.id.imagen_offline)
            TXT_ultimoMensaje =itemView.findViewById(R.id.TXT_ultimoMensaje)
            line = itemView.findViewById(R.id.line)
            item_numero_mensajes = itemView.findViewById(R.id.item_numero_mensajes)


        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //Inflar el Item
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_usuario,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        //Devolver el tamaño de la lista
        return  listaUsuariosFiltrada.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usuario: ContactoEntity = listaUsuariosFiltrada[position]


        val uidUsuarioActual = obtenerUidUsuarioActual()

        holder.nombre_usuario.text = usuario.n_usuario
        Glide.with(context).load(usuario.imagen).placeholder(R.drawable.ic_item_usuario).into(holder.imagen_usuario)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, MensajesActivity::class.java)
            //Enviamos el uid del usuario seleccionado
            intent.putExtra("uid_usuario",usuario.uid)
            Toast.makeText(context,"El usuario seleccionado es: "+usuario.n_usuario,Toast.LENGTH_SHORT).show()
            context.startActivity(intent)
        }

        val isChatFragment = fragmentType.isChatFragment()

        // Si estamos en el ChatFragment, mostramos el último mensaje
        if (isChatFragment) {
            if(!MyApp.isInternetAvailable(context)){
                holder.imagen_offline.visibility = View.VISIBLE
            }else{
               actualizarEstadoUsuario(usuario,holder)
            }
            val isConnected = MyApp.isInternetAvailable(context)
            //Si hay internet, cogemos el mensaje de firebase,en caso contrario de ROOM.
            if (isConnected) {
                ObtenerUltimoMensaje(usuario.uid, holder.TXT_ultimoMensaje)
            } else {
                obtenerUltimoMensajeLocal(usuario.uid, holder.TXT_ultimoMensaje)
            }
            obtenerNumeroMensajesNoLeidos(uidUsuarioActual,usuario.uid) { numeroMensajesNoLeidos ->
                // Actualizar el valor del contador de mensajes no leídos en el adaptador
                holder.item_numero_mensajes.text = if (numeroMensajesNoLeidos > 0) {numeroMensajesNoLeidos.toString()}  else ""
                holder.item_numero_mensajes.visibility = if (numeroMensajesNoLeidos>0) View.VISIBLE else View.GONE

                // Aquí configuramos el listener para el campo "oculto" del usuario actual en Firebase
                val databaseReference = FirebaseDatabase.getInstance().reference.child("usuarios").child(usuario.uid)
                val valueEventListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val oculto = snapshot.child("oculto").getValue(Boolean::class.java) ?: false

                        usuario.oculto = oculto
                        actualizarEstadoUsuario(usuario, holder)

                        // Actualizar el estado oculto en Room
                        CoroutineScope(Dispatchers.IO).launch {
                            MyApp.database.contactoDao().actualizarEstadoOculto(usuario.uid, oculto)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                }
                databaseReference.addValueEventListener(valueEventListener)

                // Guardamos la referencia al listener en el tag del holder para poder eliminarlo más tarde si es necesario
                holder.itemView.tag = valueEventListener
            }
        } else {
            // Si estamos en el ContactosFragment, ocultamos el campo de último mensaje
            holder.item_numero_mensajes.visibility = View.GONE
            holder.TXT_ultimoMensaje.visibility = View.GONE

        }



        // Controlar la visibilidad de la línea
        if (position > 0) {
            holder.line.visibility = View.VISIBLE
        } else {
            holder.line.visibility = View.GONE
        }

    }


    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        // Llamar a la implementación de la función en la clase base
        super.onDetachedFromRecyclerView(recyclerView)

        // Iterar a través de cada elemento hijo (ViewHolder) del RecyclerView
        for (holder in recyclerView.children) {
            // Obtener el ValueEventListener asociado al ViewHolder
            val valueEventListener = holder.tag as? ValueEventListener
            // Verificar si el ValueEventListener no es nulo
            valueEventListener?.let {
                // Obtener una referencia a la base de datos de Firebase
                val databaseReference = FirebaseDatabase.getInstance().reference
                // Eliminar el ValueEventListener de la referencia de la base de datos
                databaseReference.removeEventListener(it)
            }
        }
    }

    private fun actualizarEstadoUsuario(usuario:ContactoEntity,holder: ViewHolder){
        //Actualizar el estado del usuario (online||offline)
        if(usuario.oculto){
            holder.imagen_online.visibility = View.GONE
            holder.imagen_offline.visibility = View.VISIBLE
        }else{
            holder.imagen_online.visibility = View.VISIBLE
            holder.imagen_offline.visibility = View.GONE
        }
    }

    private fun obtenerNumeroMensajesNoLeidos(uidUsuarioActual: String, uidUsuarioItem: String, callback: (Int) -> Unit) {
        val ref = FirebaseDatabase.getInstance().reference.child("chats")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var contMensajesNoLeidos = 0
                for (dataSnapshot in snapshot.children) {
                    val chat = dataSnapshot.getValue(Chat::class.java)
                    if (chat != null) {
                        val emisor = chat.getEmisor()
                        val receptor = chat.getReceptor()
                        // Verificar si el usuario actual es el emisor o el receptor
                        if ((emisor == uidUsuarioActual && receptor == uidUsuarioItem) || (emisor == uidUsuarioItem && receptor == uidUsuarioActual)) {
                            // Verificar si el chat no ha sido visto y el usuario actual no es el emisor
                            if (!chat.isVisto() && emisor != uidUsuarioActual) {
                                contMensajesNoLeidos++
                            }
                        }
                    }
                }
                // Llamar al callback con el número de mensajes no leídos
                callback(contMensajesNoLeidos)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun obtenerUidUsuarioActual(): String {
        // Obtener la instancia de Firebase Auth
        val firebaseAuth = FirebaseAuth.getInstance()

        // Verificar si el usuario está autenticado
        val usuarioActual = firebaseAuth.currentUser
        if (usuarioActual != null) {
            // El usuario está autenticado, devolver su UID
            return usuarioActual.uid
        } else {
            // El usuario no está autenticado
            return ""
        }
    }


    private fun obtenerUltimoMensajeLocal(ChatUsuarioUID: String?, txtUltimomensaje: TextView) {
        // Verificar si el UID del usuario de chat es nulo
        if (ChatUsuarioUID == null) return

        // Obtener el usuario actualmente autenticado
        val currentUser = FirebaseAuth.getInstance().currentUser
        // Obtener el ID del usuario actual
        val currentUserId = currentUser?.uid ?: return

        // Lanzar una nueva corrutina en el hilo de E/S (IO)
        CoroutineScope(Dispatchers.IO).launch {
            // Obtener el último mensaje de la base de datos local
            val ultimoMensajeEntity = MyApp.database.mensajeDao().obtenerUltimoMensaje(currentUserId, ChatUsuarioUID)
            // Cambiar al hilo principal (UI) para actualizar la vista de texto
            withContext(Dispatchers.Main) {

                // Actualizar el texto de la vista de texto con el último mensaje obtenido
                //txtUltimomensaje.text = ultimoMensajeEntity?.mensaje ?: "¡Empieza la conversación!"
                when (ultimoMensajeEntity?.mensaje) {
                    "Se ha enviado la imagen" -> txtUltimomensaje.text = "Imagen enviada"
                    else -> txtUltimomensaje.text = ultimoMensajeEntity?.mensaje ?: "¡Empieza la conversación!"
                }
            }
        }
    }

    private fun ObtenerUltimoMensaje(ChatUsuarioUID: String?, txtUltimomensaje: TextView) {
        // Mensaje predeterminado si no hay conversación previa
        ultimoMensaje = "¡Empieza la conversación!"

        // Obtener el usuario actualmente autenticado
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        // Referencia a la base de datos de Firebase donde se almacenan los chats
        val reference = FirebaseDatabase.getInstance().reference.child("chats")

        // Agregar un ValueEventListener para escuchar cambios en los datos de los chats
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Iterar a través de los chats almacenados en la base de datos
                for (dataSnapshot in snapshot.children) {
                    // Obtener el chat actual del snapshot
                    val chat: Chat? = dataSnapshot.getValue(Chat::class.java)
                    // Verificar si el chat y el usuario actual no son nulos
                    if (firebaseUser != null && chat != null) {
                        // Verificar si el chat es entre el usuario actual y el usuario específico (por su UID)
                        if ((chat.getReceptor() == firebaseUser.uid && chat.getEmisor() == ChatUsuarioUID) ||
                            (chat.getReceptor() == ChatUsuarioUID && chat.getEmisor() == firebaseUser.uid)
                        ) {
                            // Actualizar el último mensaje con el mensaje del chat actual
                            ultimoMensaje = chat.getMensaje() ?: ""
                        }
                    }
                }

                // Actualizar el texto de la vista de texto con el último mensaje obtenido
                when (ultimoMensaje) {
                    "Se ha enviado la imagen" -> txtUltimomensaje.text = "Imagen enviada"
                    else -> txtUltimomensaje.text = ultimoMensaje
                }

                // Restablecer el valor predeterminado del último mensaje
                ultimoMensaje = "¡Empieza la conversación!"
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}