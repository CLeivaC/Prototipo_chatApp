package com.leiva.prototipo_chatapp.Fragmentos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.leiva.prototipo_chatapp.Adaptador.AdaptadorContactos
import com.leiva.prototipo_chatapp.AgregarContactoActivity
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat
import java.util.Locale

class ContactosFragment : Fragment() {

    // Código de solicitud para el permiso READ_CONTACTS
    private val READ_CONTACTS_PERMISSION_REQUEST_CODE = 1

    // Adaptador para la lista de usuarios
    private var usuarioAdaptador: AdaptadorContactos? = null

    // Lista de usuarios
    private var usuarioLista: List<Usuario>? = null

    // RecyclerView para mostrar la lista de usuarios
    private var rvUsuarios: RecyclerView? = null

    // Elementos de la interfaz de usuario
    private lateinit var buscadorEditText: EditText
    private lateinit var botonAgregar: Button

    // Código de solicitud para la actividad AgregarContactoActivity
    private val AGREGAR_CONTACTO_REQUEST_CODE = 123

    // Método llamado al crear la vista del fragmento
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el diseño de la interfaz de usuario desde el archivo XML
        val view: View = inflater.inflate(R.layout.fragment_contactos, container, false)

        // Inicializar el RecyclerView y su diseño
        rvUsuarios = view.findViewById(R.id.RV_usuarios)
        rvUsuarios!!.setHasFixedSize(true)
        rvUsuarios!!.layoutManager = LinearLayoutManager(context)

        // Inicializar la lista de usuarios
        usuarioLista = ArrayList()

        // Solicitar permiso READ_CONTACTS
        solicitarPermisoContactos()

        // Inicializar los elementos de la interfaz de usuario
        buscadorEditText = view.findViewById(R.id.buscador)
        botonAgregar = view.findViewById(R.id.botonAgregar)

        // Agregar un listener al campo de búsqueda para filtrar la lista de contactos
        buscadorEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                // Filtrar la lista de contactos en función del texto de búsqueda
                val searchText = charSequence.toString().toLowerCase(Locale.getDefault())
                val filteredList = usuarioLista?.filter {
                    it.getN_Usuario()!!.toLowerCase(Locale.getDefault()).contains(searchText) ||
                            it.getTelefono()!!.toLowerCase(Locale.getDefault()).contains(searchText)
                }
                // Actualizar el RecyclerView con la lista filtrada
                usuarioAdaptador?.filtrarLista(filteredList)
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        // Agregar un listener al botón de agregar para abrir AgregarContactoFragment
        botonAgregar.setOnClickListener {
            abrirAgregarContactoFragment()
        }

        // Devolver la vista creada
        return view
    }

    // Método para abrir la actividad AgregarContactoActivity
    private fun abrirAgregarContactoFragment() {
        val intent = Intent(requireActivity(), AgregarContactoActivity::class.java)
        startActivityForResult(intent, AGREGAR_CONTACTO_REQUEST_CODE)
    }

    // Manejar el resultado de AgregarContactoActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Verificar si el resultado proviene de AgregarContactoActivity
        if (isAdded) {
            // Verificar si el resultado proviene de AgregarContactoActivity
            if (requestCode == AGREGAR_CONTACTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                // Se ha agregado un contacto, actualiza la lista de contactos
                obtenerYVerificarContactos()
            }
        }
    }

    // Método para obtener los contactos del dispositivo
    private fun getDeviceContacts(context: Context): List<String> {
        val contactsList = mutableListOf<String>()
        val contentResolver = context.contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        // Procesar el cursor para obtener los números de teléfono
        cursor?.use {
            val phoneNumberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                // Verificar si la columna del número de teléfono existe en el cursor
                if (phoneNumberIndex != -1) {
                    val phoneNumber = it.getString(phoneNumberIndex)
                    contactsList.add(phoneNumber)
                }
            }
        }

        return contactsList
    }

    // Solicitar permisos READ_CONTACTS
    private fun solicitarPermisoContactos() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si no se tiene el permiso, solicitarlo al usuario
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                READ_CONTACTS_PERMISSION_REQUEST_CODE
            )
        } else {
            // Si ya se tiene el permiso, obtener los contactos
            obtenerYVerificarContactos()
        }
    }

    // Obtener y verificar contactos cuando se concede el permiso
    private fun obtenerYVerificarContactos() {
        val contactos = getDeviceContacts(requireContext())

        // Obtener tu propio número de teléfono
        obtenerMiNumero { miNumero ->
            // Normalizar los contactos (eliminar caracteres no deseados)
            val contactosNormalizados = contactos.map { it.replace(Regex("[^+\\d]"), "") }

            // Imprimir la lista de contactos en el registro de Android
            if (contactos.isNotEmpty()) {
                for (contacto in contactosNormalizados) {
                    Log.d("ContactosFragment", "Número de teléfono en la lista de contactos: $contacto")
                }
            }

            // Excluir tu propio número de teléfono de la lista
            val contactosExcluyendoMiNumero = contactosNormalizados.filter { it != miNumero }

            // Verificar los contactos en la base de datos de Firebase
            verificarContactosEnFirebase(contactosExcluyendoMiNumero)
        }
    }

    // Método para manejar la respuesta de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Verificar si la solicitud de permisos es para READ_CONTACTS
        when (requestCode) {
            READ_CONTACTS_PERMISSION_REQUEST_CODE -> {
                // Verificar si el usuario concedió el permiso
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // El usuario concedió el permiso, obtener y verificar los contactos
                    obtenerYVerificarContactos()
                } else {
                    // El usuario no concedió el permiso, mostrar un mensaje de advertencia
                    Toast.makeText(
                        requireContext(),
                        "No se concedió el permiso para acceder a los contactos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Verificar contactos en la base de datos de Firebase
    private fun verificarContactosEnFirebase(contactos: List<String>) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("usuarios")
        val contentResolver = requireContext().contentResolver
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Lista para almacenar usuarios que coinciden con contactos
                val usuariosCoincidentes = mutableListOf<Usuario>()

                for (sh in snapshot.children) {
                    val usuario: Usuario? = sh.getValue(Usuario::class.java)
                    val numeroTelefonoUsuario = usuario?.getTelefono()

                    if (numeroTelefonoUsuario != null && contactos.contains(numeroTelefonoUsuario)) {
                        // El número de teléfono está en la base de datos
                        // Asigna el nombre del contacto en la agenda al usuario
                        val nombreContacto = UtilidadesChat.obtenerNombreDesdeTelefono(contentResolver,numeroTelefonoUsuario)
                        usuario?.setN_Usuario(nombreContacto)
                        usuariosCoincidentes.add(usuario)
                        Log.d("ContactosFragment", "Usuarios cargados: ${usuariosCoincidentes?.size}")
                    }
                }

                // Imprimir la lista de contactos en el registro de Android
                if (contactos.isNotEmpty()) {
                    for (contacto in contactos) {
                        Log.d(
                            "ContactosFragment",
                            "Número de teléfono en la lista de contactos: $contacto"
                        )
                    }
                }

                // Limpiar la lista de usuarios
                (usuarioLista as ArrayList<Usuario>).clear()

                // Agregar usuarios coincidentes a la lista principal
                (usuarioLista as ArrayList<Usuario>).addAll(usuariosCoincidentes)

                Log.d("ContactosFragment", "Usuarios cargados: ${usuarioLista?.size}")

                // Actualizar el RecyclerView con la lista de usuarios
                activity?.runOnUiThread {
                    // Operaciones que afectan la interfaz de usuario, como la asignación del adaptador.
                    usuarioAdaptador = AdaptadorContactos(context!!, usuarioLista!!)
                    rvUsuarios!!.adapter = usuarioAdaptador
                    usuarioAdaptador?.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores de base de datos
                Log.e("ContactosFragment", "Error al obtener datos de la base de datos: $error")
            }
        })
    }

    // Verificar contacto en Firebase por número de teléfono
   /* private fun verificarContactoEnFirebase(telefono: String) {
        val reference = FirebaseDatabase.getInstance().reference.child("usuarios")

        reference.orderByChild("telefono").equalTo(telefono)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val usuariosCoincidentes = mutableListOf<Usuario>()

                    for (sh in snapshot.children) {
                        val usuario: Usuario? = sh.getValue(Usuario::class.java)
                        val numeroTelefonoUsuario = usuario?.getTelefono()

                        if (numeroTelefonoUsuario != null && numeroTelefonoUsuario == telefono) {
                            usuariosCoincidentes.add(usuario!!)
                        }
                    }

                    // Limpiar la lista de usuarios
                    (usuarioLista as ArrayList<Usuario>).clear()

                    // Agregar usuarios coincidentes a la lista principal
                    (usuarioLista as ArrayList<Usuario>).addAll(usuariosCoincidentes)

                    // Actualizar el RecyclerView con la lista de usuarios
                    activity?.runOnUiThread {
                        usuarioAdaptador?.filtrarLista(usuarioLista)
                        usuarioAdaptador?.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(
                        "ContactosFragment",
                        "Error al verificar el teléfono en la base de datos: $error"
                    )
                }
            })
    }*/

    // Obtener tu propio número de teléfono desde Firebase
    private fun obtenerMiNumero(callback: (String?) -> Unit) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("usuarios")
            .child(firebaseUser?.uid.orEmpty())

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var miNumero: String? = null
                if (snapshot.exists()) {
                    val usuario: Usuario? = snapshot.getValue(Usuario::class.java)
                    miNumero = usuario?.getTelefono()
                }
                callback(miNumero)
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar errores de base de datos al obtener tu propio número
                Log.e("ContactosFragment", "Error al obtener mi número de teléfono: $error")
                callback(null)
            }
        })
    }
}