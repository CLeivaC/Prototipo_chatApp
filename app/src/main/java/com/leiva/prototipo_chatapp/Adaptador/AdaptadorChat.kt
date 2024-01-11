package com.leiva.prototipo_chatapp.Adaptador

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.leiva.prototipo_chatapp.Modelo.Chat
import com.leiva.prototipo_chatapp.R

class AdaptadorChat(contexto: Context,chatLista: List<Chat>,imagenUrl: String,imagenEmisorUrl:String): RecyclerView.Adapter<AdaptadorChat.ViewHolder?>() {


    private val contexto:Context
    private val chatLista: List<Chat>
    private val imagenUrl: String
    private val imagenEmisorUrl: String
    var firebaseUser: FirebaseUser = FirebaseAuth.getInstance().currentUser!!

    init {
        this.contexto = contexto
        this.chatLista = chatLista
        this.imagenUrl = imagenUrl
        this.imagenEmisorUrl = imagenEmisorUrl



    }

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        /*Vistas de item mensaje izquierdo*/
        var imagen_perfil_mensaje: ImageView?=null


        var TXT_ver_mensaje: TextView?=null
        var imagen_enviada_izquierdo : ImageView?=null
        var TXT_mensaje_visto:TextView?=null


        /*Vistas de item mensaje derecho*/
        var imagen_enviada_derecha:ImageView?=null
        var imagen_perfil_mensaje_derecho: ImageView?=null
        init {
            imagen_perfil_mensaje = itemView.findViewById(R.id.imagen_perfil_mensaje)
            TXT_ver_mensaje = itemView.findViewById(R.id.TXT_ver_mensaje)
            imagen_enviada_izquierdo = itemView.findViewById(R.id.imagen_enviada_izquierdo)
            TXT_mensaje_visto = itemView.findViewById(R.id.TXT_mensaje_visto)
            imagen_enviada_derecha = itemView.findViewById(R.id.imagen_enviada_derecha)
            imagen_perfil_mensaje_derecho = itemView.findViewById(R.id.imagen_perfil_mensaje_derecho)


        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, posicion: Int): ViewHolder {
       return if(posicion ==1){
           val view: View = LayoutInflater.from(contexto).inflate(com.leiva.prototipo_chatapp.R.layout.item_mensaje_derecho,parent,false)
           ViewHolder(view)
       }else{
           val view: View = LayoutInflater.from(contexto).inflate(com.leiva.prototipo_chatapp.R.layout.item_mensaje_izquierdo,parent,false)
           ViewHolder(view)
       }
    }

    override fun getItemCount(): Int {
       return chatLista.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val chat : Chat = chatLista[position]

        try {

            Glide.with(contexto).load(imagenUrl).placeholder(R.drawable.ic_imagen_chat).into(holder.imagen_perfil_mensaje!!)
            Log.d("Leiva", "URL Imagen receptor: $imagenUrl")
            Log.d("Leiva", "URL Imagen Emisor: $imagenEmisorUrl")

        } catch (e: Exception) {
            e.printStackTrace()
            // Maneja la excepción según tus necesidades
        }

        try {

            Glide.with(contexto).load(imagenEmisorUrl).placeholder(R.drawable.ic_imagen_chat).into(holder.imagen_perfil_mensaje_derecho!!)
            Log.d("Leiva", "URL Imagen receptor: $imagenUrl")
            Log.d("Leiva", "URL Imagen Emisor: $imagenEmisorUrl")

        } catch (e: Exception) {
            e.printStackTrace()
            // Maneja la excepción según tus necesidades
        }
     //Si el mensaje contiene una imagen
    if(chat.getMensaje().equals("Se ha enviado la imagen") && !chat.getUrl().equals("")){
        //Condicion para el usuario que envía una imagen como mensaje
        if(chat.getEmisor().equals(firebaseUser!!.uid)){
            holder.TXT_ver_mensaje!!.visibility = View.GONE
            holder.imagen_enviada_derecha!!.visibility = View.VISIBLE
            Glide.with(contexto).load(chat.getUrl()).placeholder(R.drawable.ic_imagen_enviada).into(holder.imagen_enviada_derecha!!)
        }

        //Condicion para el usuario el cual nos envía una imagen como mensaje
        else if(!chat.getEmisor().equals(firebaseUser!!.uid)){
            holder.TXT_ver_mensaje!!.visibility = View.GONE
            holder.imagen_enviada_izquierdo!!.visibility = View.VISIBLE
            Glide.with(contexto).load(chat.getUrl()).placeholder(R.drawable.ic_imagen_enviada).into(holder.imagen_enviada_izquierdo!!)
        }
    }

    //Si el mensaje contiene solo texto
    else {
        holder.TXT_ver_mensaje!!.text = chat.getMensaje()
    }

    }

    override fun getItemViewType(position: Int): Int {
        return if(chatLista[position].getEmisor().equals(firebaseUser!!.uid)){
            1
        }else{
            0
        }
    }

}