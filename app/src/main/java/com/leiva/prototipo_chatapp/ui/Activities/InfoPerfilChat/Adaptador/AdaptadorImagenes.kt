package com.leiva.prototipo_chatapp.ui.Activities.InfoPerfilChat.Adaptador

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.leiva.prototipo_chatapp.Data.database.entities.ImagenEntity
import com.leiva.prototipo_chatapp.R


// Adaptador para mostrar una lista de imágenes en un RecyclerView
class AdaptadorImagenes(contexto: Context, private val imagenes: List<String>) : RecyclerView.Adapter<AdaptadorImagenes.ViewHolder>() {

    // Contexto de la actividad o fragmento que contiene el RecyclerView
    private val contexto: Context

    // Inicialización del contexto
    init {
        this.contexto = contexto
    }

    // Método que se llama cuando se necesita crear un nuevo ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflar el diseño del elemento de imagen
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_imagen_historial, parent, false)
        return ViewHolder(view)
    }

    // Método que se llama para asociar datos a un ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Obtener la URL de la imagen en la posición dada
        val imagenUrl = imagenes[position]
        // Cargar la imagen en el ImageView usando Glide
        Glide.with(holder.itemView.context).load(imagenUrl).into(holder.imageViewImagenEnviada)
    }

    // Método que devuelve el número total de elementos en la lista de imágenes
    override fun getItemCount(): Int {
        return imagenes.size
    }

    // Clase interna que representa un ViewHolder para cada elemento de imagen
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        // Vista del ImageView en el elemento de imagen
        val imageViewImagenEnviada: ImageView = itemView.findViewById(R.id.imageViewImagenEnviada)

        // Inicialización del ViewHolder
        init {
            // Configurar un OnClickListener en el elemento de imagen
            itemView.setOnClickListener(this)
        }

        // Método que se llama cuando se hace clic en un elemento de imagen
        override fun onClick(view: View?) {
            // Obtener la posición del elemento clicado
            val position = adapterPosition
            // Verificar si la posición es válida
            if (position != RecyclerView.NO_POSITION) {
                // Obtener la URL de la imagen en la posición dada
                val imagenUrl = imagenes[position]
                // Visualizar la imagen completa en un diálogo
                VisualizarImagen(imagenUrl)
            }
        }
    }

    // Método privado para visualizar una imagen completa en un diálogo
    private fun VisualizarImagen(imagen: String?) {
        // Crear un diálogo personalizado
        val dialog = Dialog(contexto)
        dialog.setContentView(R.layout.visualizer_imagen_completa)

        // Configurar el tamaño del diálogo para que ocupe toda la pantalla
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(dialog.window?.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = lp

        // Referencias a las vistas del diálogo
        val Img_visualizar: PhotoView = dialog.findViewById(R.id.Img_visualizar)
        val Btn_cerrar_v: ImageButton = dialog.findViewById(R.id.Btn_cerrar_v)

        // Cargar la imagen en el ImageView usando Glide
        Glide.with(contexto).load(imagen).placeholder(R.drawable.ic_imagen_enviada)
            .into(Img_visualizar)

        // Configurar un OnClickListener en el botón para cerrar el diálogo
        Btn_cerrar_v.setOnClickListener {
            dialog.dismiss()
        }

        // Mostrar el diálogo y evitar que se cierre al tocar fuera de él
        dialog.show()
        dialog.setCanceledOnTouchOutside(false)
    }
}