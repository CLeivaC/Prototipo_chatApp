package com.leiva.prototipo_chatapp.ui.Fragments.Contactos

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.leiva.prototipo_chatapp.ui.Fragments.Contactos.Adaptador.AdaptadorContactos
import com.leiva.prototipo_chatapp.ui.Activities.AgregarContacto.AgregarContactoActivity
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.UtilidadesChat
import com.leiva.prototipo_chatapp.ui.Fragments.Contactos.Adaptador.FragmentType
import java.util.Locale

class ContactosFragment : Fragment(),FragmentType {

    private val READ_CONTACTS_PERMISSION_REQUEST_CODE = 1
    private var usuarioAdaptador: AdaptadorContactos? = null
    private var usuarioLista: List<Usuario>? = null
    private var rvUsuarios: RecyclerView? = null
    private lateinit var buscadorEditText: EditText
    private lateinit var botonAgregar: ImageButton
    private val AGREGAR_CONTACTO_REQUEST_CODE = 123

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_contactos, container, false)
        // Obtener la Toolbar del MainActivity
        val toolbar = requireActivity().findViewById<MaterialToolbar>(R.id.toolbarMain)

        val fragmentContainer = view.findViewById<RelativeLayout>(R.id.fragment_container)
        val layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.setMargins(0, toolbar.height, 0, 0)
        fragmentContainer.layoutParams = layoutParams

        initComponents(view)
        solicitarPermisoContactos()
        return view
    }

    private fun initComponents(view: View) {
        rvUsuarios = view.findViewById(R.id.RV_usuarios)
        rvUsuarios!!.setHasFixedSize(true)
        rvUsuarios!!.layoutManager = LinearLayoutManager(context)
        usuarioLista = ArrayList()
        buscadorEditText = view.findViewById(R.id.buscador)
        botonAgregar = view.findViewById(R.id.botonAgregar)

        buscadorEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = charSequence.toString().toLowerCase(Locale.getDefault())
                val filteredList = usuarioLista?.filter {
                    it.getN_Usuario()!!.toLowerCase(Locale.getDefault()).contains(searchText) ||
                            it.getTelefono()!!.toLowerCase(Locale.getDefault()).contains(searchText)
                }
                usuarioAdaptador?.filtrarLista(filteredList)
            }
            override fun afterTextChanged(editable: Editable?) {}
        })

        botonAgregar.setOnClickListener {
            abrirAgregarContactoFragment()
        }
    }

    private fun abrirAgregarContactoFragment() {
        val intent = Intent(requireActivity(), AgregarContactoActivity::class.java)
        startActivityForResult(intent, AGREGAR_CONTACTO_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (isAdded) {
            if (requestCode == AGREGAR_CONTACTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                obtenerYVerificarContactos()
            }
        }
    }

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
        cursor?.use {
            val phoneNumberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                if (phoneNumberIndex != -1) {
                    val phoneNumber = it.getString(phoneNumberIndex)
                    Log.d(TAG, "Número de teléfono obtenido: $phoneNumber")
                    contactsList.add(phoneNumber)
                }
            }
        }
        Log.d(TAG, "Lista de contactos obtenida: $contactsList")
        return contactsList
    }

    private fun solicitarPermisoContactos() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                READ_CONTACTS_PERMISSION_REQUEST_CODE
            )
        } else {
            obtenerYVerificarContactos()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_CONTACTS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permiso concedido para acceder a los contactos")
                    obtenerYVerificarContactos()
                } else {
                    Log.d(TAG, "Permiso denegado para acceder a los contactos")
                    Toast.makeText(
                        requireContext(),
                        "No se concedió el permiso para acceder a los contactos",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun obtenerYVerificarContactos() {
        val contactos = getDeviceContacts(requireContext())
        obtenerMiNumero { miNumero ->
            val contactosNormalizados = contactos.map { it.replace(Regex("[^+\\d]"), "") }
            val contactosExcluyendoMiNumero = contactosNormalizados.filter { it != miNumero }
            Log.d(TAG, "Contactos obtenidos: $contactosExcluyendoMiNumero")
            verificarContactosEnFirebase(contactosExcluyendoMiNumero)
        }
    }

    // Declarar una lista para almacenar los teléfonos de los contactos ya agregados
    private val contactosAgregados = HashSet<String>()

    private fun verificarContactosEnFirebase(contactos: List<String>) {
        if (!isAdded) {
            return
        }

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val reference = FirebaseDatabase.getInstance().reference.child("usuarios")
        val contentResolver = requireContext().contentResolver

        // Obtener el UID del usuario actual
        val uidUsuario = firebaseUser?.uid

        val context = requireContext() // Almacenar una referencia al contexto de manera segura

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usuariosCoincidentes = mutableListOf<Usuario>()
                for (sh in snapshot.children) {
                    val usuario: Usuario? = sh.getValue(Usuario::class.java)
                    val numeroTelefonoUsuario = usuario?.getTelefono()
                    if (numeroTelefonoUsuario != null && contactos.contains(numeroTelefonoUsuario)) {
                        val nombreContacto = UtilidadesChat.obtenerNombreDesdeTelefono(contentResolver, numeroTelefonoUsuario)
                        usuario?.setN_Usuario(nombreContacto)
                        // Verificar si el contacto ya ha sido agregado
                        if (!contactosAgregados.contains(numeroTelefonoUsuario)) {
                            // Agregar el contacto a Firebase solo si no ha sido agregado antes
                            if (uidUsuario != null) {
                                agregarContactoAUsuarioEnFirebase(uidUsuario, nombreContacto, numeroTelefonoUsuario)
                                Log.d(TAG, "Contacto agregado a Firebase: $nombreContacto - $numeroTelefonoUsuario")
                            }
                            // Agregar el contacto a la lista de contactos ya agregados
                            contactosAgregados.add(numeroTelefonoUsuario)
                        }
                        usuariosCoincidentes.add(usuario)
                    }
                }
                (usuarioLista as ArrayList<Usuario>).clear()
                (usuarioLista as ArrayList<Usuario>).addAll(usuariosCoincidentes)
                activity?.runOnUiThread {
                    if (isAdded) {
                        usuarioAdaptador = AdaptadorContactos(context, usuarioLista!!, this@ContactosFragment)
                        rvUsuarios!!.adapter = usuarioAdaptador
                        usuarioAdaptador?.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al obtener datos de la base de datos: $error")
            }
        })
    }


    private fun agregarContactoAUsuarioEnFirebase(uidUsuario: String, nombreContacto: String, telefonoContacto: String) {
        val reference = FirebaseDatabase.getInstance().reference
        val contactosReference = reference.child("usuarios").child(uidUsuario).child("contactos")
        val query = contactosReference.orderByChild("telefono").equalTo(telefonoContacto)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // El contacto no existe, agregarlo a Firebase
                    val nuevoContactoKey = contactosReference.push().key
                    if (nuevoContactoKey != null) {
                        val nuevoContacto = HashMap<String, Any>()
                        nuevoContacto["nombre"] = nombreContacto
                        nuevoContacto["telefono"] = telefonoContacto
                        contactosReference.child(nuevoContactoKey).setValue(nuevoContacto)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Contacto agregado correctamente a Firebase
                                    Log.d(TAG, "Contacto agregado correctamente a Firebase")
                                } else {
                                    // Error al agregar el contacto a Firebase
                                    Log.e(TAG, "Error al agregar contacto a Firebase: ${task.exception}")
                                }
                            }
                    }
                } else {
                    // El contacto ya existe en Firebase, no es necesario agregarlo
                    Log.d(TAG, "El contacto ya existe en Firebase")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al consultar la lista de contactos del usuario: $error")
            }
        })
    }
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
                Log.d(TAG, "Mi número de teléfono obtenido: $miNumero")
                callback(miNumero)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al obtener mi número de teléfono: $error")
                callback(null)
            }
        })
    }

    override fun isChatFragment(): Boolean {
        return false
    }
}
