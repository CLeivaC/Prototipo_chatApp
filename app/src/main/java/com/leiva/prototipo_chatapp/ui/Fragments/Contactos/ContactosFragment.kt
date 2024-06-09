package com.leiva.prototipo_chatapp.ui.Fragments.Contactos

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.leiva.prototipo_chatapp.Data.database.entities.ContactoEntity
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.ui.Activities.AgregarContacto.AgregarContactoActivity
import com.leiva.prototipo_chatapp.ui.Fragments.Contactos.Adaptador.AdaptadorContactos
import com.leiva.prototipo_chatapp.ui.Fragments.Contactos.Adaptador.FragmentType
import com.leiva.prototipo_chatapp.ui.Fragments.Contactos.viewModel.ContactosViewModel
import java.util.Locale

class ContactosFragment : Fragment(), FragmentType {

    private lateinit var contactosViewModel: ContactosViewModel
    private lateinit var usuarioAdaptador: AdaptadorContactos
    private lateinit var rvUsuarios: RecyclerView
    private lateinit var buscadorEditText: EditText
    private lateinit var botonAgregar: ImageButton
    private val AGREGAR_CONTACTO_REQUEST_CODE = 123

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_contactos, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contactosViewModel = ViewModelProvider(this)[ContactosViewModel::class.java]
        initComponents(view)
        contactosViewModel.cargarContactosDesdeRoom(requireContext())
        observeViewModel()
        solicitarPermisoContactos()
    }

    private fun initComponents(view: View) {
        //Inicializar compontentes
        rvUsuarios = view.findViewById(R.id.RV_usuarios)
        rvUsuarios.setHasFixedSize(true)
        rvUsuarios.layoutManager = LinearLayoutManager(context)

        usuarioAdaptador = AdaptadorContactos(requireContext(),ArrayList(),this)

        rvUsuarios.adapter = usuarioAdaptador

        buscadorEditText = view.findViewById(R.id.buscador)
        //Permitir filtrar contactos por nombre
        buscadorEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = charSequence.toString().toLowerCase(Locale.getDefault())
                contactosViewModel.filtrarUsuarios(searchText)
            }
            override fun afterTextChanged(editable: Editable?) {}
        })

        botonAgregar = view.findViewById(R.id.botonAgregar)
        botonAgregar.setOnClickListener {
            abrirAgregarContactoFragment()
        }
    }
    /*
     * Observa los cambios del viewModel para que se actualice la vista
     */
    private fun observeViewModel() {
        contactosViewModel.usuariosFiltradosLiveData.observe(viewLifecycleOwner) { usuarios ->
            Log.d(TAG, "LiveData actualizado con la lista filtrada: $usuarios")
            usuarioAdaptador.actualizarLista(usuarios)
            usuarioAdaptador.notifyDataSetChanged()
        }

        // Observa el cambio en la lista original de usuarios
        contactosViewModel.usuariosOriginalesLiveData.observe(viewLifecycleOwner) { usuariosOriginales ->
            // Restablece la lista filtrada cuando cambia la lista original
            contactosViewModel.filtrarUsuarios(buscadorEditText.text.toString())
            usuarioAdaptador.actualizarLista(usuariosOriginales)
        }
    }
    /*
     * Abrir activity de "agregar contacto"
     */
    private fun abrirAgregarContactoFragment() {
        val intent = Intent(requireActivity(), AgregarContactoActivity::class.java)
        startActivityForResult(intent, AGREGAR_CONTACTO_REQUEST_CODE)
    }

    /*
     * Cuando el permiso está concedido, procede a obtener y verificar contactos
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (isAdded) {
            if (requestCode == AGREGAR_CONTACTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                contactosViewModel.obtenerYVerificarContactos(requireContext())
            }
        }
    }

    /*
     * Solicitud de permiso de contactos
     */
    private fun solicitarPermisoContactos() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                contactosViewModel.READ_CONTACTS_PERMISSION_REQUEST_CODE

            )
            contactosViewModel.obtenerYVerificarContactos(requireContext())
        } else {
            contactosViewModel.obtenerYVerificarContactos(requireContext())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            contactosViewModel.READ_CONTACTS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permiso concedido para acceder a los contactos")
                    // Solo solicitar carga de contactos si no hay datos disponibles en Room
                    if (contactosViewModel.usuariosOriginalesLiveData.value.isNullOrEmpty()) {
                        contactosViewModel.obtenerYVerificarContactos(requireContext())
                    }
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

    override fun isChatFragment(): Boolean {
        return false
    }

}

