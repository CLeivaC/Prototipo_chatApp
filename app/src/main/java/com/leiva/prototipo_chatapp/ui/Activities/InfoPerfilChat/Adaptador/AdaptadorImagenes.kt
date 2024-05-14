package com.leiva.prototipo_chatapp.ui.Activities.InfoPerfilChat.Adaptador

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.imageview.ShapeableImageView
import com.leiva.prototipo_chatapp.R


class AdaptadorImagenes(contexto: Context, private val imagenes: List<String>) : RecyclerView.Adapter<AdaptadorImagenes.ViewHolder>() {


    private val contexto: Context


    init {
        this.contexto = contexto
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_imagen_historial, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imagenUrl = imagenes[position]
        Glide.with(holder.itemView.context).load(imagenUrl).into(holder.imageViewImagenEnviada)
    }

    override fun getItemCount(): Int {
        return imagenes.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val imageViewImagenEnviada: ImageView = itemView.findViewById(R.id.imageViewImagenEnviada)

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val imagenUrl = imagenes[position]
                VisualizarImagen(imagenUrl)
            }
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
}