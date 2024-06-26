package com.leiva.prototipo_chatapp.ui.Activities.Mensajes.Adaptador

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.leiva.prototipo_chatapp.Modelo.Chat
import com.leiva.prototipo_chatapp.R
import java.text.SimpleDateFormat
import java.util.Locale

class AdaptadorMensaje(
    contexto: Context,
    chatLista: List<Chat>,
    imagenUrl: String,
    imagenEmisorUrl: String
) : RecyclerView.Adapter<AdaptadorMensaje.ViewHolder?>() {


    private val contexto: Context
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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /*Vistas de item mensaje izquierdo*/
        var imagen_perfil_mensaje: ImageView? = null
        var textViewHoraMensaje: TextView? = null
        var textViewHoraImagen: TextView? = null

        var TXT_ver_mensaje: TextView? = null
        var imagen_enviada_izquierdo: ImageView? = null
        var estadoMensaje: ImageView? = null
        var estadoMensajeImagen: ImageView? = null


        /*Vistas de item mensaje derecho*/
        var imagen_enviada_derecha: ImageView? = null
        var imagen_perfil_mensaje_derecho: ImageView? = null

        var cardView_imagen_enviada_derecha: CardView? = null
        var cardView_imagen_enviada_izquierda: CardView? = null

        init {
            imagen_perfil_mensaje = itemView.findViewById(R.id.imagen_perfil_mensaje)
            TXT_ver_mensaje = itemView.findViewById(R.id.TXT_ver_mensaje)
            imagen_enviada_izquierdo = itemView.findViewById(R.id.imagen_enviada_izquierdo)
            imagen_enviada_derecha = itemView.findViewById(R.id.imagen_enviada_derecha)
            imagen_perfil_mensaje_derecho =
                itemView.findViewById(R.id.imagen_perfil_mensaje_derecho)
            estadoMensaje = itemView.findViewById(R.id.estadoMensaje)
            estadoMensajeImagen = itemView.findViewById(R.id.estadoMensajeImagen)
            cardView_imagen_enviada_derecha =
                itemView.findViewById(R.id.cardView_imagen_enviada_derecha)
            cardView_imagen_enviada_izquierda =
                itemView.findViewById(R.id.cardView_imagen_enviada_izquierda)
            textViewHoraMensaje = itemView.findViewById(R.id.textViewHoraMensaje)
            textViewHoraImagen = itemView.findViewById(R.id.textViewHoraImagen)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, posicion: Int): ViewHolder {
        return if (posicion == 1) {
            val view: View = LayoutInflater.from(contexto)
                .inflate(R.layout.item_mensaje_derecho, parent, false)
            ViewHolder(view)
        } else {
            val view: View = LayoutInflater.from(contexto)
                .inflate(R.layout.item_mensaje_izquierdo, parent, false)
            ViewHolder(view)
        }
    }

    override fun getItemCount(): Int {
        return chatLista.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val chat: Chat = chatLista[position]

        val horaMensaje = SimpleDateFormat("HH:mm", Locale.getDefault()).format(chat.getHora())

        holder.textViewHoraMensaje!!.text = horaMensaje

        try {

            Glide.with(contexto).load(imagenUrl).placeholder(R.drawable.ic_imagen_chat)
                .into(holder.imagen_perfil_mensaje!!)
            Log.d("Leiva", "URL Imagen receptor: $imagenUrl")
            Log.d("Leiva", "URL Imagen Emisor: $imagenEmisorUrl")

        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {

            Glide.with(contexto).load(imagenEmisorUrl).placeholder(R.drawable.ic_imagen_chat)
                .into(holder.imagen_perfil_mensaje_derecho!!)
            Log.d("Leiva", "URL Imagen receptor: $imagenUrl")
            Log.d("Leiva", "URL Imagen Emisor: $imagenEmisorUrl")

        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            //Si el mensaje contiene una imagen
            if (chat.getMensaje().equals("Se ha enviado la imagen")) {
                //Condicion para el usuario que envía una imagen como mensaje
                if (chat.getEmisor().equals(firebaseUser!!.uid)) {
                    holder.TXT_ver_mensaje!!.visibility = View.GONE
                    //holder.estadoMensaje!!.visibility = View.GONE
                    holder.cardView_imagen_enviada_derecha!!.visibility = View.VISIBLE
                    holder.imagen_enviada_derecha!!.visibility = View.VISIBLE


                    holder.textViewHoraImagen!!.visibility = View.VISIBLE
                    holder.textViewHoraMensaje!!.visibility = View.GONE
                    holder.textViewHoraImagen!!.text = horaMensaje

                    holder.estadoMensaje!!.visibility = View.GONE
                    holder.estadoMensajeImagen!!.visibility = View.VISIBLE

                    Glide.with(contexto).load(chat.getUrl())
                        .placeholder(R.drawable.ic_imagen_enviada)
                        .into(holder.imagen_enviada_derecha!!)
                    Log.d("Prueba", chat.getUrl().toString())

                    holder.imagen_enviada_derecha!!.setOnClickListener {
                        val opciones = arrayOf<CharSequence>(
                            "Ver imagen completa",
                            "Eliminar imagen",
                            "Cancelar"
                        )
                        val builder: AlertDialog.Builder =
                            AlertDialog.Builder(holder.itemView.context)
                        builder.setTitle("¿Qué desea realizar?")
                        builder.setItems(
                            opciones,
                            DialogInterface.OnClickListener { dialogInterface, i ->
                                if (i == 0) {
                                    VisualizarImagen(chat.getUrl())
                                }

                                if (i == 1) {
                                    val mensajeId =
                                        chatLista[position].getId_Mensaje() // Obtener el ID del mensaje
                                    EliminarMensaje(
                                        mensajeId!!,
                                        holder
                                    ) // Pasar el ID del mensaje a la función EliminarMensaje
                                }
                            })
                        builder.show()
                    }
                }

                //Condicion para el usuario el cual nos envía una imagen como mensaje
                else if (!chat.getEmisor().equals(firebaseUser!!.uid)) {
                    holder.TXT_ver_mensaje!!.visibility = View.GONE
                    //holder.estadoMensaje!!.visibility = View.GONE
                    holder.cardView_imagen_enviada_izquierda!!.visibility = View.VISIBLE
                    holder.imagen_enviada_izquierdo!!.visibility = View.VISIBLE
                    Glide.with(contexto).load(chat.getUrl())
                        .placeholder(R.drawable.ic_imagen_enviada)
                        .into(holder.imagen_enviada_izquierdo!!)


                    holder.textViewHoraImagen!!.visibility = View.VISIBLE
                    holder.textViewHoraMensaje!!.visibility = View.GONE
                    holder.textViewHoraImagen!!.text = horaMensaje

                    holder.imagen_enviada_izquierdo!!.setOnClickListener {
                        val opciones = arrayOf<CharSequence>("Ver imagen completa", "Cancelar")
                        val builder: AlertDialog.Builder =
                            AlertDialog.Builder(holder.itemView.context)
                        builder.setTitle("¿Qué desea realizar?")
                        builder.setItems(
                            opciones,
                            DialogInterface.OnClickListener { dialogInterface, i ->
                                if (i == 0) {
                                    VisualizarImagen(chat.getUrl())
                                }
                            })
                        builder.show()
                    }
                }
            }

            //Si el mensaje contiene solo texto
            else {
                holder.TXT_ver_mensaje!!.text = chat.getMensaje()
                if (firebaseUser.uid == chat.getEmisor()) {
                    holder.TXT_ver_mensaje!!.setOnClickListener {
                        val opciones = arrayOf<CharSequence>("Eliminar mensaje", "Cancelar")
                        val builder: AlertDialog.Builder =
                            AlertDialog.Builder(holder.itemView.context)
                        builder.setTitle("¿Qué desea realizar?")
                        builder.setItems(
                            opciones,
                            DialogInterface.OnClickListener { dialogInterface, i ->
                                if (i == 0) {
                                    val mensajeId = chatLista[position].getId_Mensaje()
                                    Log.d("mensajeid", mensajeId.toString())
                                    if (mensajeId != null) {
                                        EliminarMensaje(mensajeId, holder)
                                    }
                                }
                            })
                        builder.show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        // Cambiar la imagen del indicador de visto a ic_checkazul si el mensaje ha sido visto por el receptor
        if (chat.isVisto()) {
            holder.estadoMensaje?.setImageResource(R.drawable.ic_checkazul)
            holder.estadoMensajeImagen?.setImageResource(R.drawable.ic_checkazul)
        } else {
            holder.estadoMensaje?.setImageResource(R.drawable.ic_checkgris)
            holder.estadoMensajeImagen?.setImageResource(R.drawable.ic_checkgris)
        }


    }

    override fun getItemViewType(position: Int): Int {
        return if (chatLista[position].getEmisor().equals(firebaseUser!!.uid)) {
            1
        } else {
            0
        }
    }


    private fun VisualizarImagen(imagen: String?) {
        val dialog = Dialog(contexto)
        dialog.setContentView(R.layout.visualizer_imagen_completa)

        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = lp

        val Img_visualizar: PhotoView = dialog.findViewById(R.id.Img_visualizar)
        val Btn_cerrar_v: ImageButton = dialog.findViewById(R.id.Btn_cerrar_v)

        Glide.with(contexto).load(imagen).placeholder(R.drawable.ic_imagen_enviada)
            .into(Img_visualizar)

        Btn_cerrar_v.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
    }

    private fun EliminarMensaje(mensajeId: String, holder: ViewHolder) {
        val reference = FirebaseDatabase.getInstance().reference.child("chats")
            .child(mensajeId)
            .removeValue()
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    Toast.makeText(holder.itemView.context, "Mensaje eliminado", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(
                        holder.itemView.context,
                        "No se ha eliminado el mensaje, inténtelo de nuevo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


}