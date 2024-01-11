package com.leiva.prototipo_chatapp.Fragmentos

import android.Manifest.permission.READ_CONTACTS
import android.content.Context
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
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ContactosFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
/*class ContactosFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private var usuarioAdaptador: AdaptadorContactos?=null
    private var usuarioLista: List<Usuario>?=null
    private var rvUsuarios: RecyclerView?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.fragment_contactos, container, false)
        rvUsuarios = view.findViewById(R.id.RV_usuarios)
        rvUsuarios!!.setHasFixedSize(true)
        rvUsuarios!!.layoutManager = LinearLayoutManager(context)

        usuarioLista = ArrayList()
        obtenerUsuariosBD()
        return view
    }

    private fun obtenerUsuariosBD() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser!!.uid
        val reference = FirebaseDatabase.getInstance().reference.child("usuarios").orderByChild("n_usuario")
        reference.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                (usuarioLista as ArrayList<Usuario>).clear()
                for(sh in snapshot.children){
                    val usuario : Usuario?=sh.getValue(Usuario::class.java)
                    if(!(usuario!!.getUid()).equals(firebaseUser)){
                        (usuarioLista as ArrayList<Usuario>).add(usuario)
                    }
                }
                usuarioAdaptador = AdaptadorContactos(context!!,usuarioLista!!)
                rvUsuarios!!.adapter = usuarioAdaptador
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ContactosFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ContactosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}*/
class ContactosFragment : Fragment() {

private val READ_CONTACTS_PERMISSION_REQUEST_CODE = 1

    private var usuarioAdaptador: AdaptadorContactos?=null
    private var usuarioLista: List<Usuario>?=null
    private var rvUsuarios: RecyclerView?=null

    private lateinit var buscadorEditText: EditText
    private lateinit var botonAgregar:Button

    override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
): View? {
    val view: View = inflater.inflate(R.layout.fragment_contactos, container, false)
    rvUsuarios = view.findViewById(R.id.RV_usuarios)
    rvUsuarios!!.setHasFixedSize(true)
    rvUsuarios!!.layoutManager = LinearLayoutManager(context)

    usuarioLista = ArrayList()

    // Solicitar permiso READ_CONTACTS
    solicitarPermisoContactos()

        buscadorEditText = view.findViewById(R.id.buscador)
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

        // Obtener referencia al botón
        botonAgregar = view.findViewById(R.id.botonAgregar)

        // Agregar listener al botón para abrir AgregarContactoFragment
        botonAgregar.setOnClickListener {
            abrirAgregarContactoFragment()
        }


    return view
}
    private fun abrirAgregarContactoFragment() {
        val agregarContactoFragment = AgregarContactoFragment()

        // Reemplazar el fragment actual por AgregarContactoFragment
        val transaction = requireActivity().supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, agregarContactoFragment)
        transaction.addToBackStack(null)
        transaction.commit()
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

    val contactosNormalizados = contactos.map { it.replace(Regex("[^+\\d]"), "") }
    // Ahora puedes verificar los contactos en la base de datos de Firebase

    if (contactos.isNotEmpty()) {
        // Imprimir el contenido de la lista de contactos en el registro de Android
        for (contacto in contactosNormalizados) {
            Log.d(
                "ContactosFragment",
                "Número de teléfono en la lista de contactos: $contactosNormalizados"
            )
        }
    }
    verificarContactosEnFirebase(contactosNormalizados)
}

// Método para manejar la respuesta de la solicitud de permisos
override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    when (requestCode) {
        READ_CONTACTS_PERMISSION_REQUEST_CODE -> {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // El usuario concedió el permiso, obtener y verificar los contactos
                obtenerYVerificarContactos()
            } else {
                // El usuario no concedió el permiso, puedes informar al usuario
                // sobre la importancia del permiso o tomar otras acciones
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

    reference.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // Lista para almacenar usuarios que coinciden con contactos
            val usuariosCoincidentes = mutableListOf<Usuario>()

            for (sh in snapshot.children) {
                val usuario: Usuario? = sh.getValue(Usuario::class.java)
                Log.d("ContactosFragment", "Usuario recuperado: ${usuario!!.getN_Usuario()}")
                val numeroTelefonoUsuario = usuario?.getTelefono()

                if (numeroTelefonoUsuario != null && contactos.contains(numeroTelefonoUsuario)) {
                    // El número de teléfono está en la base de datos
                    // Agregar el usuario a la lista
                    usuariosCoincidentes.add(usuario)
                    Log.d("ContactosFragment", "Usuarios cargados: ${usuariosCoincidentes?.size}")
                }
            }
            if (contactos.isNotEmpty()) {
                // Imprimir el contenido de la lista de contactos en el registro de Android
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

            // Crear el adaptador y configurar el RecyclerView
           // usuarioAdaptador = AdaptadorContactos(requireContext(), usuarioLista!!)
            //rvUsuarios!!.adapter = usuarioAdaptador

            activity?.runOnUiThread {
                // Operaciones que afectan la interfaz de usuario, como la asignación del adaptador
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
fun verificarContactoEnFirebase(telefono: String) {
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

                (usuarioLista as ArrayList<Usuario>).clear()
                (usuarioLista as ArrayList<Usuario>).addAll(usuariosCoincidentes)

                activity?.runOnUiThread {
                    usuarioAdaptador?.filtrarLista(usuarioLista)
                    usuarioAdaptador?.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ContactosFragment", "Error al verificar el teléfono en la base de datos: $error")
            }
        })
}

// ... (resto del código)
}