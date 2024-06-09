package com.leiva.prototipo_chatapp.ui.Fragments.Chat

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.messaging.FirebaseMessaging
import com.leiva.prototipo_chatapp.Data.database.entities.ContactoEntity
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.ui.Fragments.Contactos.Adaptador.AdaptadorContactos
import com.leiva.prototipo_chatapp.ui.Fragments.Contactos.Adaptador.FragmentType
import com.leiva.prototipo_chatapp.ui.Fragments.Chat.viewModel.ChatViewModel

class ChatFragment : Fragment(), FragmentType {

    private lateinit var viewModel: ChatViewModel
    private lateinit var adaptadorContactos: AdaptadorContactos
    private lateinit var recyclerView: RecyclerView
    private val READ_CONTACTS_PERMISSION_REQUEST_CODE = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)

        initRecyclerView()



        return view
    }

    // Función para inicializar el RecyclerView
    private fun initRecyclerView() {
        // Configurar el LinearLayoutManager para el RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        // Inicializar el adaptador de contactos con una lista vacía y el listener de clics
        adaptadorContactos = AdaptadorContactos(requireContext(), mutableListOf(), this)
        // Asignar el adaptador al RecyclerView
        recyclerView.adapter = adaptadorContactos
    }

    // Función para verificar los permisos de acceso a los contactos
    private fun checkContactPermission() {
        // Verificar si se tienen permisos de lectura de contactos
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Si se tienen permisos, cargar los contactos desde el ViewModel
            viewModel.onViewCreated(requireContext().contentResolver)
        } else {
            // Si no se tienen permisos, solicitarlos al usuario
            requestPermissions(
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                READ_CONTACTS_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Función llamada cuando se obtiene una respuesta a la solicitud de permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Verificar si se obtuvieron los permisos de lectura de contactos
        if (requestCode == READ_CONTACTS_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Si se obtuvieron los permisos, cargar los contactos desde el ViewModel
            viewModel.onViewCreated(requireContext().contentResolver)
        }
    }

    // Función para indicar si este fragmento es el fragmento de chat
    override fun isChatFragment(): Boolean {
        return true
    }

    // Función llamada cuando la vista del fragmento ha sido creada
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Verificar los permisos de acceso a los contactos
        checkContactPermission()
        // Observar los cambios en la lista de contactos en el ViewModel
        observeViewModel()
        // Obtener el token de Firebase Messaging y actualizarlo en el ViewModel
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result?.let { token ->
                        viewModel.updateToken(token)
                    }
                }
            }
    }

    // Función para observar los cambios en la lista de contactos en el ViewModel
    private fun observeViewModel() {
        viewModel.contactos.observe(viewLifecycleOwner, Observer { contactos ->
            // Actualizar la lista de contactos en el adaptador
            adaptadorContactos.actualizarLista(contactos)
        })
    }

}

