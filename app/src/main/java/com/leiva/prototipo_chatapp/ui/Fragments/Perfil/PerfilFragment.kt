package com.leiva.prototipo_chatapp.ui.Fragments.Perfil

import android.app.Activity

import android.app.AlertDialog
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.leiva.prototipo_chatapp.R
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.IOException
import android.Manifest
import android.app.ProgressDialog
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat.getSystemService
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.leiva.prototipo_chatapp.Data.database.AppDatabase
import com.leiva.prototipo_chatapp.Data.database.entities.UsuarioData
import com.leiva.prototipo_chatapp.Utilidades.MyApp
import com.leiva.prototipo_chatapp.ui.Activities.Login.LoginActivity
import com.leiva.prototipo_chatapp.ui.MainActivity

class PerfilFragment : Fragment() {
    fun Fragment.requireContextOrNull() = context
    private lateinit var nombreTextView: TextView
    private lateinit var telefonoTextView: TextView
    private lateinit var apellidoTextView: TextView
    private lateinit var editTextInfoAdicional: EditText
    private lateinit var databaseReference: DatabaseReference
    private lateinit var imagenPerfil: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var fondoImagenUri: ImageView
    private var tipoImagen = false
    private lateinit var switchActivarOculto : SwitchCompat
    private lateinit var switchClaroOscuro : SwitchCompat
    private lateinit var btnCerrarSesion : Button
    private lateinit var switchNotificaciones : SwitchCompat
    val CAMARA_REQUEST_CODE =  200
    private val REQUEST_PERMISSION_CODE = 1001
    private var nombreActual = ""
    private var apellidoActual = ""
    private var telefonoActual = ""
    private var infoAdicionalActual = ""
    private val handler = Handler(Looper.getMainLooper())
    private val delayMillis = 5000L // 5 segundos
    private var textChangeRunnable: Runnable? = null
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
            tipoImagen = true
            mostrarDialogo()
        }

        fondoImagenUri.setOnClickListener() {
            tipoImagen = false
            mostrarDialogo()
        }

        switchActivarOculto.setOnCheckedChangeListener { _, isChecked ->
            // No guardar en la base de datos
            guardarEstadoSwitchEnDataStore(isChecked)
        }

        switchNotificaciones.setOnCheckedChangeListener { _, isChecked ->
            // No guardar en la base de datos
            guardarEstadoSwitchNotificacionesEnDataStore(isChecked)
        }

        btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }

        setupTextWatchers()
    }



    private fun setupTextWatchers() {
        nombreTextView.addTextChangedListener(getTextWatcher())
        apellidoTextView.addTextChangedListener(getTextWatcher())
        telefonoTextView.addTextChangedListener(getTextWatcher())
        editTextInfoAdicional.addTextChangedListener(getTextWatcher())
    }

    private fun getTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Cancela el Runnable pendiente si existe
                textChangeRunnable?.let { handler.removeCallbacks(it) }

                // Crea un nuevo Runnable que se ejecutará después del retraso especificado
                textChangeRunnable = Runnable {
                    guardarDatosUsuario()
                }

                // Programa la ejecución del Runnable después del retraso
                handler.postDelayed(textChangeRunnable!!, delayMillis)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cargarEstadoSwitch()
        cargarEstadoSwitchClaroOscuro()
        cargarEstadoSwitchNotificaciones()

    }

    private fun obtenerDatosUsuario() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid

        if (userId != null) {
            if (isNetworkAvailable()) {
                obtenerDatosDesdeFirebase()
            } else {
                cargarDatosDesdeRoom()
            }
        } else {
            cargarDatosDesdeRoom()
        }
    }

    private fun Fragment.isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


    private fun obtenerDatosDesdeFirebase() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid
        if (userId != null) {
            databaseReference.child(userId).addValueEventListener(object :
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

                            // Crear un objeto UsuarioData con los datos obtenidos de Firebase
                            val usuario = UsuarioData(
                                id = userId,
                                nombre = nombre ?: "",
                                apellido = apellido ?: "",
                                telefono = telefono ?: "",
                                infoAdicional = infoAdicional ?: "",
                                imagen = imagenUrl ?: "",
                                fondoPerfilUrl = fondoPerfilUrl ?: ""
                            )

                            // Insertar o actualizar el usuario en la base de datos local
                            insertarUsuarioEnRoom(usuario)

                            // Actualizar la interfaz de usuario con los datos de Firebase
                            with(requireView()) {
                                nombreTextView.text = nombre
                                apellidoTextView.text = apellido
                                telefonoTextView.text = telefono
                                editTextInfoAdicional.setText(infoAdicional)

                                // Cargar la imagen de perfil usando Glide
                                Glide.with(requireContext())
                                    .load(imagenUrl)
                                    .placeholder(R.drawable.ic_item_usuario)
                                    .error(R.drawable.ic_juegos)
                                    .into(findViewById(R.id.P_imagen))

                                // Cargar el fondo de perfil usando Glide
                                Glide.with(requireContext())
                                    .load(fondoPerfilUrl)
                                    .placeholder(R.drawable.ic_item_usuario)
                                    .error(R.drawable.ic_juegos)
                                    .into(findViewById(R.id.fondo_perfil_image))
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Error al obtener datos de Firebase: ${e.message}")
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Error en la consulta a Firebase: ${databaseError.message}")
                    // Si hay un error en la consulta a Firebase, cargar desde la base de datos local
                    cargarDatosDesdeRoom()
                }
            })
        } else {
            // Si no hay un usuario autenticado, cargar desde la base de datos local
            cargarDatosDesdeRoom()
        }
        Log.d(TAG, "Datos del usuario obtenidos correctamente")
    }


    // Función para verificar si hay cambios en los datos del usuario
    private fun insertarUsuarioEnRoom(usuario: UsuarioData) {
        lifecycleScope.launch(Dispatchers.IO) {
            val context = requireContextOrNull() ?: return@launch
            val dao = AppDatabase.getInstance(context).usuarioDao()

            // Verificar si el usuario ya existe en la base de datos local
            val usuarioExistente = dao.obtenerUsuario(usuario.id)

            if (usuarioExistente != null) {
                // Si el usuario ya existe, actualiza los datos
                val usuarioActualizado = usuarioExistente.copy(
                    nombre = usuario.nombre,
                    apellido = usuario.apellido,
                    telefono = usuario.telefono,
                    infoAdicional = usuario.infoAdicional,
                    imagen = usuario.imagen,
                    fondoPerfilUrl = usuario.fondoPerfilUrl
                )
                dao.actualizarUsuario(usuarioActualizado)
            } else {
                // Si el usuario no existe, inserta un nuevo usuario
                dao.insertarUsuario(usuario)
            }


            // Cargar los datos desde Room nuevamente después de la inserción o actualización
            cargarDatosDesdeRoom()
        }
    }
    // Función para cargar los datos de todos los usuarios desde Room
    private fun cargarDatosUsuarioUI(usuario: UsuarioData) {
        with(requireView()) {
            findViewById<TextView>(R.id.editTextNombre).text = usuario.nombre
            findViewById<TextView>(R.id.editTextApellido).text = usuario.apellido
            findViewById<TextView>(R.id.editTextTelefono).text = usuario.telefono
            findViewById<EditText>(R.id.editTextInfoAdicional).setText(usuario.infoAdicional)

            // Cargar la imagen de perfil usando Glide
            Glide.with(requireContext())
                .load(usuario.imagen) // URL de la imagen de perfil del usuario
                .placeholder(R.drawable.ic_item_usuario) // Imagen de placeholder mientras se carga la imagen
                .error(R.drawable.ic_juegos) // Imagen de error si no se puede cargar la imagen
                .into(findViewById(R.id.P_imagen))

            // Cargar el fondo de perfil usando Glide
            Glide.with(requireContext())
                .load(usuario.fondoPerfilUrl) // URL del fondo de perfil del usuario
                .placeholder(R.drawable.ic_item_usuario) // Imagen de placeholder mientras se carga la imagen
                .error(R.drawable.ic_juegos) // Imagen de error si no se puede cargar la imagen
                .into(findViewById(R.id.fondo_perfil_image))
        }
    }
    private fun cargarDatosDesdeRoom() {
        GlobalScope.launch(Dispatchers.IO) {
            val usuarios = AppDatabase.getInstance(requireContext()).usuarioDao().obtenerTodosUsuarios()

            withContext(Dispatchers.Main) {
                if (usuarios.isNotEmpty()) {
                    // Limpiar vistas antes de agregar nuevos datos
                    // (esto es opcional dependiendo de cómo quieras mostrar los usuarios)
                    nombreTextView.text = ""
                    apellidoTextView.text = ""
                    telefonoTextView.text = ""
                    editTextInfoAdicional.setText("")
                    requireView().findViewById<ImageView>(R.id.P_imagen).setImageResource(R.drawable.ic_item_usuario)
                    requireView().findViewById<ImageView>(R.id.fondo_perfil_image).setImageResource(R.drawable.ic_item_usuario)

                    // Mostrar los datos del usuario activo
                    val usuarioActivoId = obtenerIdUsuarioActivo() // Obtener el ID del usuario activo
                    val usuarioActivo = usuarios.find { it.id == usuarioActivoId }
                    usuarioActivo?.let { cargarDatosUsuarioUI(it) }
                } else {
                    Log.d(TAG, "No hay datos de usuario en la base de datos local")
                }
            }
        }
    }

    private fun guardarDatosUsuario() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid
        val nombre = nombreTextView.text.toString()
        val apellido = apellidoTextView.text.toString()
        val telefono = telefonoTextView.text.toString()
        val infoAdicional = editTextInfoAdicional.text.toString()

        userId?.let { uid ->
            // Verificar si ha habido cambios en los campos
            if (nombre != nombreActual || apellido != apellidoActual || telefono != telefonoActual || infoAdicional != infoAdicionalActual) {
                // Crear un mapa para almacenar los datos actualizados
                val usuarioActualizado = hashMapOf<String, Any>()

                // Verificar cada campo y agregarlo al mapa solo si ha habido cambios
                if (nombre != nombreActual) {
                    usuarioActualizado["nombre"] = nombre
                }
                if (apellido != apellidoActual) {
                    usuarioActualizado["apellido"] = apellido
                }
                if (telefono != telefonoActual) {
                    usuarioActualizado["telefono"] = telefono
                }
                if (infoAdicional != infoAdicionalActual) {
                    usuarioActualizado["infoAdicional"] = infoAdicional
                }

                try {
                    // Verificar si la referencia de la base de datos es nula
                    if (databaseReference == null) {
                        Log.e(TAG, "La referencia de la base de datos es nula")
                        return
                    }

                    // Actualizar los datos en Firebase solo si ha habido cambios
                    if (usuarioActualizado.isNotEmpty()) {
                        databaseReference.child(uid).updateChildren(usuarioActualizado)
                            .addOnSuccessListener {
                                Log.d(TAG, "Datos actualizados exitosamente en Firebase")
                                // Actualizar los valores actuales
                                nombreActual = nombre
                                apellidoActual = apellido
                                telefonoActual = telefono
                                infoAdicionalActual = infoAdicional
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error al actualizar datos en Firebase: ${e.message}")
                            }
                    } else {
                        Log.d(TAG, "No hay cambios para actualizar en Firebase")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error inesperado al actualizar datos en Firebase: ${e.message}")
                }
            } else {
                Log.d(TAG, "No hay cambios para actualizar en Firebase")
            }
        } ?: run {
            Log.e(TAG, "UID de usuario nulo")
        }
        Log.d(TAG, "Datos del usuario guardados correctamente")
    }

    private fun abrirCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // El permiso está concedido, abre la cámara
            val camaraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(camaraIntent, CAMARA_REQUEST_CODE)
        } else {
            // El permiso no está concedido, solicita permiso
            solicitarPermisoCamara()
        }
    }

    private fun solicitarPermisoCamara() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMARA_REQUEST_CODE)

    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(
            Intent.createChooser(intent, "Selecciona una imagen"),
            PICK_IMAGE_REQUEST
        )
        Log.d(TAG, "Galería abierta para seleccionar una imagen")
    }


    // Método para manejar el resultado de la solicitud de permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso concedido, abre la galería
                    abrirGaleria()
                } else {
                    // Permiso denegado, muestra un mensaje o toma otra acción
                    Toast.makeText(requireContext(), "Permiso de galería denegado", Toast.LENGTH_SHORT).show()
                }
            }
            CAMARA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permiso concedido, abre la cámara
                    abrirCamara()
                } else {
                    // Permiso denegado, muestra un mensaje o toma otra acción
                    Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun verificarYSolicitarPermisoCamara() {
        try {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Si ya se tiene permiso, abrir la cámara
                abrirCamara()
            } else {
                // Si no se tiene permiso, solicitarlo
                solicitarPermisoCamara()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al verificar y solicitar permiso de la cámara: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Error al verificar y solicitar permiso de la cámara",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarDialogo() {
        val opciones = arrayOf("Abrir Galería", "Cámara")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Seleccionar opción")
            .setItems(opciones) { dialog, which ->
                when (which) {
                    0 -> abrirGaleria()
                    1 -> verificarYSolicitarPermisoCamara()
                }
            }
        val dialog = builder.create()
        dialog.show()
    }

    // Método para manejar el resultado de la selección de imagen de la galería
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        try {
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                // Obtiene la URI de la imagen seleccionada desde la galería
                val uri = data.data
                if (uri != null) {
                    cargarImagenYSubirla(uri)
                }
            } else if (requestCode == CAMARA_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
                // Guarda la imagen de la cámara en la galería
                val imageBitmap = data.extras?.get("data") as Bitmap
                val uri = guardarImagenEnGaleria(requireContext(), imageBitmap)
                uri?.let {
                    cargarImagenYSubirla(uri)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onActivityResult: ${e.message}")
        }
    }
    private fun cargarImagenYSubirla(uri: Uri) {
        // Crea y muestra un ProgressDialog
        val progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Cargando imagen...")
        progressDialog.setCancelable(false)
        progressDialog.show()



        // Carga la imagen seleccionada en la ImageView correspondiente y muestra un indicador de carga
        if (tipoImagen) {
            Glide.with(requireContext())
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_perfil_mensaje) // Placeholder mientras se carga la imagen
                .error(R.drawable.ic_perfil_mensaje) // Imagen de error si la carga falla
                .into(imagenPerfil)
        }
        if (!tipoImagen) {
            Glide.with(requireContext())
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_perfil_mensaje) // Placeholder mientras se carga la imagen
                .error(R.drawable.ic_perfil_mensaje) // Imagen de error si la carga falla
                .into(fondoImagenUri)
        }

        // Guarda la URL de la imagen en la base de datos en segundo plano
        if (tipoImagen) {
            guardarUrlImagenEnBaseDeDatos(uri).invokeOnCompletion {
                if (it == null) {
                    Log.d(TAG, "URL de la imagen de perfil guardada correctamente")
                } else {
                    Log.e(TAG, "Error al guardar la URL de la imagen de perfil: ${it.message}")
                }
                // Cierra el ProgressDialog una vez que la carga está completa o falla
                progressDialog.dismiss()
            }
        }
        if (!tipoImagen) {
            guardarUrlFondoPerfilEnBaseDeDatos(uri).invokeOnCompletion {
                if (it == null) {
                    Log.d(TAG, "URL del fondo de perfil guardada correctamente")
                } else {
                    Log.e(TAG, "Error al guardar la URL del fondo de perfil: ${it.message}")
                }
                // Cierra el ProgressDialog una vez que la carga está completa o falla
                progressDialog.dismiss()
            }
        }
    }



    private fun guardarImagenEnGaleria(context: Context, bitmap: Bitmap): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                }
                return uri
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
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

            // Actualizar el estado del switch en la base de datos
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val userId = firebaseUser?.uid
            userId?.let { uid ->
                val userStatusMap = mapOf("oculto" to isChecked)
                FirebaseDatabase.getInstance().reference.child("usuarios").child(uid)
                    .updateChildren(userStatusMap)
                    .addOnSuccessListener {
                        // Operación de actualización exitosa
                    }
                    .addOnFailureListener { e ->
                        // Error al actualizar la base de datos
                        Log.e("MainActivity", "Error al actualizar el estado del usuario: $e")
                    }
            } ?: run {
                Log.e("MainActivity", "UID de usuario nulo")
            }
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
                    MainActivity.setDayNight(0)
                    telefonoTextView.setTextColor(resources.getColor(R.color.white))
                    Log.d(TAG, "Tema cambiado a oscuro")
                } else {
                    // Cambiar a modo claro
                    MainActivity.setDayNight(1)
                    Log.d(TAG, "Tema cambiado a claro")
                }
                // No guardar en la base de datos
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
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar sesión: ${e.message}")
            // Aquí podrías mostrar un mensaje de error al usuario si lo deseas
        }
    }

    override fun onPause() {
        super.onPause()
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Detiene todos los Runnable pendientes
    }


    private fun obtenerIdUsuarioActivo(): String? {
        return Firebase.auth.currentUser?.uid
    }
}