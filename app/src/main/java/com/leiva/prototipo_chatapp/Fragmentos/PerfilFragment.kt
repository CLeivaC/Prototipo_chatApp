package com.leiva.prototipo_chatapp.Fragmentos

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.leiva.prototipo_chatapp.Login
import com.leiva.prototipo_chatapp.MainActivity
import com.leiva.prototipo_chatapp.MyApp
import com.leiva.prototipo_chatapp.R
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
class PerfilFragment : Fragment() {

    private lateinit var nombreTextView: TextView
    private lateinit var telefonoTextView: TextView
    private lateinit var apellidoTextView: TextView
    private lateinit var editTextInfoAdicional: EditText
    private lateinit var databaseReference: DatabaseReference
    private lateinit var imagenPerfil: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var fondoImagenUri: ImageView
    private var seEstaCambiandoImagenPerfil = false
    private var seEstaCambiandoFondoPerfil = false
    private lateinit var switchActivarOculto : SwitchCompat
    private lateinit var switchClaroOscuro : SwitchCompat
    private lateinit var btnCerrarSesion : Button
    private lateinit var switchNotificaciones : SwitchCompat

    private var selectedFragmentId: Int = R.id.PerfilF
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)
        initializeViews(view)

        // Obtener datos del usuario
        obtenerDatosUsuario()

        // Configurar los listeners de los botones y switches
        setupButtonListeners()

        // Configurar la funcionalidad para cambiar el tema
        cambiarTema()

        return view
    }

    private fun initializeViews(view: View) {
        // Inicializar vistas
        nombreTextView = view.findViewById(R.id.editTextNombre)
        telefonoTextView = view.findViewById(R.id.editTextTelefono)
        apellidoTextView = view.findViewById(R.id.editTextApellido)
        editTextInfoAdicional = view.findViewById(R.id.editTextInfoAdicional)
        imagenPerfil = view.findViewById(R.id.P_imagen)
        fondoImagenUri = view.findViewById(R.id.fondo_perfil_image)
        btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion)
        switchActivarOculto = view.findViewById(R.id.switchActivarOculto)
        switchClaroOscuro = view.findViewById(R.id.switchClaroOscuro)
        databaseReference = FirebaseDatabase.getInstance().reference.child("usuarios")
        switchNotificaciones = view.findViewById(R.id.switchNotificaciones)
    }

    private fun setupButtonListeners() {

        // Agregar OnClickListener al ImageView para seleccionar imagen de la galería
        imagenPerfil.setOnClickListener {
            seEstaCambiandoImagenPerfil = true
            abrirGaleria()
        }

        fondoImagenUri.setOnClickListener() {
            seEstaCambiandoFondoPerfil = true
            abrirGaleria()
        }

        switchActivarOculto.setOnCheckedChangeListener { _, isChecked ->
            guardarEstadoSwitchEnBaseDeDatos(isChecked)
        }

        switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            guardarEstadoNotificacionesEnBaseDeDatos(isChecked)
        }

        btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }
        nombreTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDatosUsuario()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        apellidoTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDatosUsuario()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        telefonoTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDatosUsuario()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        editTextInfoAdicional.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                guardarDatosUsuario()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Llama a la función cargarEstadoSwitch() cuando se crea la vista del fragmento
        cargarEstadoSwitch()
        cargarEstadoSwitchClaroOscuro()
        cargarEstadoSwitchNotificaciones()


    }

    private fun obtenerDatosUsuario() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid
        if (userId != null) {
            databaseReference.child(userId).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        try {
                            val nombre = dataSnapshot.child("nombre").getValue(String::class.java)
                            val apellido = dataSnapshot.child("apellido").getValue(String::class.java)
                            val telefono = dataSnapshot.child("telefono").getValue(String::class.java)

                            val infoAdicional = dataSnapshot.child("infoAdicional").getValue(String::class.java)
                            val imagenUrl = dataSnapshot.child("imagen").getValue(String::class.java)
                            val fondoPerfilUrl = dataSnapshot.child("fondoPerfilUrl").getValue(String::class.java)
                            val switchActivar = dataSnapshot.child("oculto").getValue(Boolean::class.java)

                            nombreTextView.text = nombre
                            apellidoTextView.text = apellido
                            telefonoTextView.text = telefono
                            // Verificar si el valor de switchActivar es nulo antes de usarlo
                            switchActivar?.let { activo ->
                                // Si no es nulo, asignar el valor al Switch
                                switchActivarOculto.isChecked = activo
                            } ?: run {
                                // Si es nulo, establecer el valor predeterminado del Switch (por ejemplo, false)
                                switchActivarOculto.isChecked = false
                            }

                            // Mostrar la información adicional
                            editTextInfoAdicional.setText(infoAdicional)

                            // Cargar la imagen de perfil usando Glide
                            view?.let {
                                Glide.with(requireContext())
                                    .load(imagenUrl) // La URL de la imagen de perfil del usuario
                                    .placeholder(R.drawable.ic_item_usuario) // Imagen de placeholder mientras se carga la imagen
                                    .error(R.drawable.ic_juegos) // Imagen de error si no se puede cargar la imagen
                                    .into(it.findViewById(R.id.P_imagen))
                            } // ImageView donde se mostrará la imagen

                            // Cargar el fondo de perfil usando Glide
                            view?.let {
                                Glide.with(requireContext())
                                    .load(fondoPerfilUrl) // La URL del fondo de perfil del usuario
                                    .placeholder(R.drawable.ic_item_usuario) // Imagen de placeholder mientras se carga la imagen
                                    .error(R.drawable.ic_juegos) // Imagen de error si no se puede cargar la imagen
                                    .into(it.findViewById(R.id.fondo_perfil_image))
                            } // ImageView donde se mostrará el fondo de perfil

                            // Actualizar los datos del usuario en Firebase
                            guardarDatosUsuario()

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
        Log.d(TAG, "Datos del usuario obtenidos correctamente")
    }



    private fun guardarDatosUsuario() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid
        val nombre = nombreTextView.text.toString()
        val apellido = apellidoTextView.text.toString()
        val telefono = telefonoTextView.text.toString()
        val infoAdicional = editTextInfoAdicional.text.toString()

        userId?.let { uid ->
            val usuario = HashMap<String, Any>()
            usuario["nombre"] = nombre
            usuario["apellido"] = apellido
            usuario["telefono"] = telefono
            usuario["infoAdicional"] = infoAdicional

            try {
                // Verificar si la referencia de la base de datos es nula
                if (databaseReference == null) {
                    Log.e(TAG, "La referencia de la base de datos es nula")
                    return
                }

                databaseReference.child(uid).updateChildren(usuario)
                    .addOnSuccessListener {
                        Log.d(TAG, "Datos actualizados exitosamente en Firebase")
                        // Aquí no necesitas actualizar la URL del fondo de perfil ya que no estás cambiando la imagen de fondo
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
        Log.d(TAG, "Datos del usuario guardados correctamente")
    }


    // Método para abrir la galería
    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(
            Intent.createChooser(intent, "Selecciona una imagen"),
            PICK_IMAGE_REQUEST
        )
        Log.d(TAG, "Galería abierta para seleccionar una imagen")
    }


    // Método para manejar el resultado de la selección de imagen de la galería

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        try {
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                val uri = data.data

                // Verificar si se está cambiando la imagen de perfil
                if (seEstaCambiandoImagenPerfil) {
                    // Carga la imagen seleccionada en el ImageView de imagenPerfil
                    Glide.with(requireContext()).load(uri).into(imagenPerfil)
                    // Guarda la URL de la imagen de perfil en la base de datos
                    guardarUrlImagenEnBaseDeDatos(uri!!).invokeOnCompletion {
                        if (it == null) {
                            Log.d(TAG, "URL de la imagen de perfil guardada correctamente")
                        } else {
                            Log.e(TAG, "Error al guardar la URL de la imagen de perfil: ${it.message}")
                        }
                    }
                }

                // Verificar si se está cambiando el fondo de perfil
                if (seEstaCambiandoFondoPerfil) {
                    // Carga la imagen seleccionada en el ImageView de fondoImagenUri
                    Glide.with(requireContext()).load(uri).into(fondoImagenUri)
                    // Guarda la URL del fondo de perfil en la base de datos
                    guardarUrlFondoPerfilEnBaseDeDatos(uri!!).invokeOnCompletion {
                        if (it == null) {
                            Log.d(TAG, "URL del fondo de perfil guardada correctamente")
                        } else {
                            Log.e(TAG, "Error al guardar la URL del fondo de perfil: ${it.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onActivityResult: ${e.message}")
        }
    }




    // Agrega este método
    private fun guardarUrlImagenEnBaseDeDatos(uri: Uri): Deferred<String?> = CoroutineScope(Dispatchers.IO).async {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid
        val rutaImagen = "Perfil_usuario/${firebaseUser?.uid}" // Ruta por defecto para la imagen de perfil

        val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)

        try {
            val tarea = referenceStorage.putFile(uri).await()
            val urlImagen = tarea.storage.downloadUrl.await().toString()
            ActualizarImagenBD(urlImagen)
            Log.d(TAG, "URL de la imagen de perfil guardada en Firebase Storage")
            return@async urlImagen
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar imagen en Firebase Storage: ${e.message}")
            return@async null
        }
    }


    private fun guardarUrlFondoPerfilEnBaseDeDatos(uri: Uri): Deferred<String?> = CoroutineScope(Dispatchers.IO).async {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid
        val rutaImagen = "Fondos_perfil/${firebaseUser?.uid}" // Ruta para el fondo de perfil

        val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)

        try {
            val tarea = referenceStorage.putFile(uri).await()
            val urlFondoPerfil = tarea.storage.downloadUrl.await().toString()
            ActualizarFondoPerfilBD(urlFondoPerfil)
            Log.d(TAG, "URL del fondo de perfil guardada en Firebase Storage")
            return@async urlFondoPerfil
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar fondo de perfil en Firebase Storage: ${e.message}")
            return@async null
        }
    }


    private suspend fun ActualizarFondoPerfilBD(urlFondoPerfil: String) {
        val hashMap: HashMap<String, Any> = HashMap()
        if (urlFondoPerfil.isNotEmpty()) {
            hashMap["fondoPerfilUrl"] = urlFondoPerfil
        }
        val reference = FirebaseDatabase.getInstance().getReference("usuarios")
        try {
            reference.child(FirebaseAuth.getInstance().currentUser!!.uid).updateChildren(hashMap).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "El fondo de perfil ha sido actualizado", Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG, "Fondo perfil actualizado correctamente en Firebase Database")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar fondo de perfil en Firebase Database: ${e.message}")
            // Aquí puedes agregar un Toast o manejar la excepción de alguna otra manera
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Error al actualizar fondo de perfil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun ActualizarImagenBD(urlImagen: String) {
        val hashMap: HashMap<String, Any> = HashMap()
        if (urlImagen.isNotEmpty()) { // Verifica si la URL de la imagen no está vacía
            hashMap["imagen"] = urlImagen
        }
        val reference = FirebaseDatabase.getInstance().getReference("usuarios")
        try {
            reference.child(FirebaseAuth.getInstance().currentUser!!.uid).updateChildren(hashMap).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Su imagen ha sido actualizada", Toast.LENGTH_SHORT).show()
            }
            Log.d(TAG, "Imagen actualizada correctamente en Firebase Database")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar imagen en Firebase Database: ${e.message}")
            // Manejo de excepción, podrías mostrar un Toast o manejarlo de otra manera
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Error al actualizar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun guardarEstadoSwitchEnBaseDeDatos(estado: Boolean) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid

        userId?.let { uid ->
            val usuario = HashMap<String, Any>()
            usuario["oculto"] = estado

            try {
                // Verificar si la referencia de la base de datos es nula
                if (databaseReference == null) {
                    Log.e(TAG, "La referencia de la base de datos es nula")
                    return
                }

                databaseReference.child(uid).updateChildren(usuario)
                    .addOnSuccessListener {
                        Log.d(TAG, "Estado del switch guardado exitosamente en Firebase")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al guardar estado del switch en Firebase: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al guardar estado del switch en Firebase: ${e.message}")
            }
        } ?: run {
            Log.e(TAG, "UID de usuario nulo")
        }
    }


    private fun guardarEstadoNotificacionesEnBaseDeDatos(notificacion: Boolean) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid

        userId?.let { uid ->
            val usuario = HashMap<String, Any>()
            usuario["notificaciones"] = notificacion  // Aquí usamos "notificaciones" como clave para el estado del interruptor

            val reference = FirebaseDatabase.getInstance().reference.child("usuarios").child(uid)
            try {
                reference.updateChildren(usuario)
                    .addOnSuccessListener {
                        Log.d(TAG, "Estado del interruptor de notificaciones guardado exitosamente en Firebase")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al guardar estado del interruptor de notificaciones en Firebase: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error inesperado al guardar notificacion del interruptor de notificaciones en Firebase: ${e.message}")
            }
        } ?: run {
            Log.e(TAG, "UID de usuario nulo")
        }
    }

    override fun onPause() {
        super.onPause()
        guardarEstadoSwitchEnDataStore(switchActivarOculto.isChecked)
        guardarEstadoSwitchClaroOscuroEnDataStore(switchClaroOscuro.isChecked)
        guardarEstadoSwitchNotificacionesEnDataStore(switchNotificaciones.isChecked)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            (requireActivity().application as MyApp).readSwitchState().collect { isChecked ->
                switchActivarOculto.isChecked = isChecked
            }
        }
    }

    private fun cargarEstadoSwitch() {
        lifecycleScope.launch {
            (requireActivity().application as MyApp).readSwitchState().collect { isChecked ->
                switchActivarOculto.isChecked = isChecked
            }
        }
    }

    private fun guardarEstadoSwitchEnDataStore(isChecked: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            (requireActivity().application as MyApp).writeSwitchState(isChecked)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selected_fragment_id", selectedFragmentId)
    }


    // Cargar el estado del switch claro/oscuro desde DataStore
    private fun cargarEstadoSwitchClaroOscuro() {
        lifecycleScope.launch {
            (requireActivity().application as MyApp).readSwitchClaroOscuroState().collect { isChecked ->
                switchClaroOscuro.isChecked = isChecked
            }
        }
    }

    // Guardar el estado del switch claro/oscuro en DataStore
    private fun guardarEstadoSwitchClaroOscuroEnDataStore(isChecked: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            (requireActivity().application as MyApp).writeSwitchClaroOscuroState(isChecked)
        }
    }

    private fun cargarEstadoSwitchNotificaciones() {
        lifecycleScope.launch {
            (requireActivity().application as MyApp).readSwitchNotificacionesState().collect { isChecked ->
                switchNotificaciones.isChecked = isChecked
            }
        }
    }

    // Guardar el estado del switch claro/oscuro en DataStore
    private fun guardarEstadoSwitchNotificacionesEnDataStore(isChecked: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            (requireActivity().application as MyApp).writeSwitchNotificacionesState(isChecked)
        }
    }



    private fun cambiarTema() {
        switchClaroOscuro.setOnCheckedChangeListener { _, isChecked ->
            try {
                if (isChecked) {
                    // Cambiar a modo oscuro
                    (activity as? MainActivity)?.setDayNight(0)
                    telefonoTextView.setTextColor(resources.getColor(R.color.white))
                    Log.d(TAG, "Tema cambiado a oscuro")
                } else {
                    // Cambiar a modo claro
                    (activity as? MainActivity)?.setDayNight(1)
                    Log.d(TAG, "Tema cambiado a claro")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cambiar el tema: ${e.message}")
                // Aquí podrías mostrar un mensaje de error al usuario si lo deseas
            }
        }
    }


    private fun cerrarSesion() {
        try {
            // Cerrar sesión y redirigir a la pantalla de inicio de sesión
            FirebaseAuth.getInstance().signOut()
            Log.d(TAG, "Sesión cerrada correctamente")
            Toast.makeText(
                requireContext(),
                "Sesión cerrada correctamente",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(requireContext(), Login::class.java)
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar sesión: ${e.message}")
            // Aquí podrías mostrar un mensaje de error al usuario si lo deseas
        }
    }



}