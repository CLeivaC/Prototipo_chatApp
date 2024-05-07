package com.leiva.prototipo_chatapp.Adaptador

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.leiva.prototipo_chatapp.Chat.MensajesActivity
import com.leiva.prototipo_chatapp.Modelo.Chat
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import org.w3c.dom.Text

class AdaptadorContactos (context: Context, listaUsuariosFiltrada: List<Usuario>): RecyclerView.Adapter<AdaptadorContactos.ViewHolder?>(){

    private val context: Context
    private var listaUsuariosFiltrada: List<Usuario>
    var ultimoMensaje:String = ""
    var ref = FirebaseDatabase.getInstance().reference.child("chats")
    //var firebaseUser = FirebaseAuth.getInstance().currentUser

    init {
        this.context = context
        this.listaUsuariosFiltrada = listaUsuariosFiltrada
    }

    fun filtrarLista(filtrada: List<Usuario>?) {
        listaUsuariosFiltrada = filtrada ?: emptyList()
        notifyDataSetChanged()
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var nombre_usuario: TextView
        //var numero_usuario: TextView
        var imagen_usuario:ImageView
        var imagen_online:ImageView
        var imagen_offline:ImageView
        var TXT_ultimoMensaje:TextView
        var line: View // Agregar referencia a la línea
        var item_numero_mensajes:TextView



        init {
            nombre_usuario = itemView.findViewById(R.id.item_nombre_usuario)
            //  numero_usuario = itemView.findViewById(R.id.item_numero_usuario)
            imagen_usuario = itemView.findViewById(R.id.Item_imagen)
            imagen_online = itemView.findViewById(R.id.imagen_online)
            imagen_offline= itemView.findViewById(R.id.imagen_offline)
            TXT_ultimoMensaje =itemView.findViewById(R.id.TXT_ultimoMensaje)
            line = itemView.findViewById(R.id.line)
            item_numero_mensajes = itemView.findViewById(R.id.item_numero_mensajes)


        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.item_usuario,parent,false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return  listaUsuariosFiltrada.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val usuario: Usuario = listaUsuariosFiltrada[position]


        val uidUsuarioActual = obtenerUidUsuarioActual()

        // Llamar a la función para obtener el número de mensajes no leídos para este usuario
        obtenerNumeroMensajesNoLeidos(uidUsuarioActual,usuario.getUid()) { numeroMensajesNoLeidos ->
            // Actualizar el valor del contador de mensajes no leídos en el adaptador
            holder.item_numero_mensajes.text = if (numeroMensajesNoLeidos > 0) {numeroMensajesNoLeidos.toString()}  else ""
            holder.item_numero_mensajes.visibility = if (numeroMensajesNoLeidos>0) View.VISIBLE else View.GONE
        }

        holder.nombre_usuario.text = usuario.getN_Usuario()
        //holder.numero_usuario.text = usuario.getTelefono()
        Glide.with(context).load(usuario.getImagen()).placeholder(R.drawable.ic_item_usuario).into(holder.imagen_usuario)

        holder.itemView.setOnClickListener {
            val intent = Intent(context,MensajesActivity::class.java)
            //Enviamos el uid del usuario seleccionado
            intent.putExtra("uid_usuario",usuario.getUid())
            Toast.makeText(context,"El usuario seleccionado es: "+usuario.getN_Usuario(),Toast.LENGTH_SHORT).show()
            context.startActivity(intent)
        }

        ObtenerUltimoMensje(usuario.getUid(),holder.TXT_ultimoMensaje)

        if(usuario.getOculto() == false){
            holder.imagen_online.visibility=View.VISIBLE
            holder.imagen_offline.visibility = View.GONE
        }else{
            holder.imagen_online.visibility= View.GONE
            holder.imagen_offline.visibility = View.VISIBLE
        }

        // Controlar la visibilidad de la línea
        if (position > 0) {
            holder.line.visibility = View.VISIBLE
        } else {
            holder.line.visibility = View.GONE
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
                // Manejar errores de base de datos si es necesario
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
            // El usuario no está autenticado, devolver una cadena vacía o manejar el caso según sea necesario
            return ""
        }
    }

    private fun ObtenerUltimoMensje(ChatUsuarioUID: String?, txtUltimomensaje: TextView) {
        ultimoMensaje="¡Empieza la conversación!"

        val firebaseUser= FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("chats")
        reference.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                for(dataSnapshot in snapshot.children){
                    val chat: Chat?=dataSnapshot.getValue(Chat::class.java)
                    if(firebaseUser!=null && chat!=null){
                        if(chat.getReceptor() == firebaseUser!!.uid &&
                            chat.getEmisor() ==ChatUsuarioUID ||
                            chat.getReceptor()==ChatUsuarioUID &&
                            chat.getEmisor() == firebaseUser!!.uid){
                            ultimoMensaje = chat.getMensaje()!!
                        }
                    }
                }

                when(ultimoMensaje){
                    "Se ha enviado la imagen" ->txtUltimomensaje.text= "Imagen enviada"
                    else-> txtUltimomensaje.text =ultimoMensaje
                }
                ultimoMensaje="¡Empieza la conversación!"
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }
}