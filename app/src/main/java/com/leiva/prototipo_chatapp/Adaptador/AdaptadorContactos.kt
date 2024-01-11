package com.leiva.prototipo_chatapp.Adaptador

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.leiva.prototipo_chatapp.Chat.MensajesActivity
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R

class AdaptadorContactos (context: Context, listaUsuariosFiltrada: List<Usuario>): RecyclerView.Adapter<AdaptadorContactos.ViewHolder?>(){

    private val context: Context
    private var listaUsuariosFiltrada: List<Usuario>

    init {
        this.context = context
        this.listaUsuariosFiltrada = listaUsuariosFiltrada
    }

    //private var listaUsuariosFiltrada: List<Usuario> = listaUsuarios
    fun filtrarLista(filtrada: List<Usuario>?) {
        listaUsuariosFiltrada = filtrada ?: emptyList()
        notifyDataSetChanged()
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var nombre_usuario: TextView
        var numero_usuario: TextView
        var imagen_usuario:ImageView

        init {
            nombre_usuario = itemView.findViewById(R.id.item_nombre_usuario)
            numero_usuario = itemView.findViewById(R.id.item_numero_usuario)
            imagen_usuario = itemView.findViewById(R.id.Item_imagen)
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
        holder.nombre_usuario.text = usuario.getN_Usuario()
        holder.numero_usuario.text = usuario.getTelefono()
        Glide.with(context).load(usuario.getImagen()).placeholder(R.drawable.ic_item_usuario).into(holder.imagen_usuario)

        holder.itemView.setOnClickListener {
            val intent = Intent(context,MensajesActivity::class.java)
            //Enviamos el uid del usuario seleccionado
            intent.putExtra("uid_usuario",usuario.getUid())
            Toast.makeText(context,"El usuario seleccionado es: "+usuario.getN_Usuario(),Toast.LENGTH_SHORT).show()
            context.startActivity(intent)
        }
    }
}