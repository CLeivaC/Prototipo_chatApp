package com.leiva.prototipo_chatapp.Fragmentos

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.leiva.prototipo_chatapp.Login
import com.leiva.prototipo_chatapp.R




class PerfilFragment : Fragment() {

    private lateinit var nombreTextView: TextView
    private lateinit var telefonoTextView: TextView
    private lateinit var apellidoTextView: TextView
    private lateinit var spinnerEstado: Spinner
    private lateinit var databaseReference: DatabaseReference
    private lateinit var imagenPerfil: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var firebaseAuth : FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        nombreTextView = view.findViewById(R.id.editTextNombre)
        telefonoTextView = view.findViewById(R.id.editTextTelefono)
        apellidoTextView = view.findViewById(R.id.editTextApellido)
        spinnerEstado = view.findViewById(R.id.spinnerEstado)
        imagenPerfil = view.findViewById(R.id.P_imagen)
        val btnGuardar = view.findViewById<Button>(R.id.P_guardar)
        val btnCerrarSesion = view.findViewById<Button>(R.id.btnCerrarSesion)

        val adapter = ArrayAdapter.createFromResource(requireContext(),
            R.array.estados, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEstado.adapter = adapter

        spinnerEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                val estadoSeleccionado = parentView?.getItemAtPosition(position).toString()
                val textView = parentView?.getChildAt(0) as? TextView
                when (estadoSeleccionado) {
                    "En línea" -> textView?.setTextColor(Color.GREEN)
                    "Ausente" -> textView?.setTextColor(Color.RED)
                    "Desconectado" -> textView?.setTextColor(Color.GRAY)
                }
            }

            override fun onNothingSelected(parentView: AdapterView<*>?) {
                // No hacer nada si no se selecciona ningún estado
            }
        }

        // Obtener referencia a la base de datos
        databaseReference = FirebaseDatabase.getInstance().reference.child("usuarios")

        // Obtener datos del usuario actual
        obtenerDatosUsuario()

        btnGuardar.setOnClickListener {
            guardarDatosUsuario()

        }

        // Agregar OnClickListener al ImageView para seleccionar imagen de la galería
        imagenPerfil.setOnClickListener {
            abrirGaleria()
        }

        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            // Redirige a la pantalla de inicio de sesión (login)
            val intent = Intent(requireContext(), Login::class.java)
            startActivity(intent)
            requireActivity().finish() // Finaliza la actividad actual
        }

        return view
    }

    private fun obtenerDatosUsuario() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid
        if (userId != null) {
            databaseReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        try {
                            val nombre = dataSnapshot.child("nombre").getValue(String::class.java)
                            val apellido = dataSnapshot.child("apellido").getValue(String::class.java)
                            val telefono = dataSnapshot.child("telefono").getValue(String::class.java)
                            val estado = dataSnapshot.child("estado").getValue(String::class.java)
                            val imagenUrl = dataSnapshot.child("imagen").getValue(String::class.java)

                            nombreTextView.text = nombre
                            apellidoTextView.text = apellido
                            telefonoTextView.text = telefono

                            // Seleccionar el estado en el Spinner
                            val estadosArray = resources.getStringArray(R.array.estados)
                            val estadoIndex = estadosArray.indexOf(estado)
                            spinnerEstado.setSelection(estadoIndex)

                            // Cargar la imagen de perfil usando Glide
                            view?.let {
                                Glide.with(requireContext())
                                    .load(imagenUrl) // La URL de la imagen de perfil del usuario
                                    .placeholder(R.drawable.ic_item_usuario) // Imagen de placeholder mientras se carga la imagen
                                    .error(R.drawable.ic_juegos) // Imagen de error si no se puede cargar la imagen
                                    .into(it.findViewById(R.id.P_imagen))
                            } // ImageView donde se mostrará la imagen

                        } catch (e: Exception) {
                            Log.e(TAG, "Error al obtener datos de Firebase: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Error en la consulta a Firebase: ${databaseError.message}")
                }
            })
        }
    }

    private fun guardarDatosUsuario() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid
        val nombre = nombreTextView.text.toString()
        val apellido = apellidoTextView.text.toString()
        val telefono = telefonoTextView.text.toString()
        val estado = spinnerEstado.selectedItem.toString()

        userId?.let { uid ->
            val usuario = HashMap<String, Any>()
            usuario["nombre"] = nombre
            usuario["apellido"] = apellido
            usuario["telefono"] = telefono
            usuario["estado"] = estado

            try {
                // Verificar si la referencia de la base de datos es nula
                if (databaseReference == null) {
                    Log.e(TAG, "La referencia de la base de datos es nula")
                    return
                }

                databaseReference.child(uid).updateChildren(usuario)
                    .addOnSuccessListener {
                        Log.d(TAG, "Datos actualizados exitosamente en Firebase")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al actualizar datos en Firebase: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al actualizar datos en Firebase: ${e.message}")
            }
        } ?: run {
            Log.e(TAG, "UID de usuario nulo")
        }
    }

    // Método para abrir la galería
    private fun abrirGaleria() {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), PICK_IMAGE_REQUEST)
        }

        // Método para manejar el resultado de la selección de imagen de la galería
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val uri = data.data

            // Carga la imagen seleccionada en el ImageView usando Glide
            Glide.with(requireContext()).load(uri).into(imagenPerfil)

            // Guarda la URL de la imagen en la base de datos
            guardarUrlImagenEnBaseDeDatos(uri!!)
        }
    }

    // Agrega este método
    private fun guardarUrlImagenEnBaseDeDatos(uri: Uri) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid
        val rutaImagen = "Perfil_usuario/${firebaseUser?.uid}" // Corrección: usamos el ID de usuario para la ruta
        val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)

        referenceStorage.putFile(uri)
            .addOnSuccessListener { tarea ->
                // Si la carga de la imagen es exitosa, obtenemos la URL de la imagen
                tarea.storage.downloadUrl.addOnCompleteListener { uriTask ->
                    if (uriTask.isSuccessful) {
                        val urlImagen = uriTask.result.toString()
                        // Actualizamos la URL de la imagen en la base de datos
                        ActualizarImagenBD(urlImagen)
                    } else {
                        Log.e(TAG, "Error al obtener la URL de la imagen: ${uriTask.exception?.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar imagen en Firebase Storage: ${e.message}")
            }
    }

    private fun ActualizarImagenBD(urlImagen: String) {
        val hashMap: HashMap<String, Any> = HashMap()
        if (urlImagen.isNotEmpty()) { // Verifica si la URL de la imagen no está vacía
            hashMap["imagen"] = urlImagen
        }
        val reference = FirebaseDatabase.getInstance().getReference("usuarios")
        reference.child(FirebaseAuth.getInstance().currentUser!!.uid).updateChildren(hashMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Su imagen ha sido actualizada", Toast.LENGTH_SHORT).show()
            }
    }



}