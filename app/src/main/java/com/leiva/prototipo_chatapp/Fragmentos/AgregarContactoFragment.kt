package com.leiva.prototipo_chatapp.Fragmentos

import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.leiva.prototipo_chatapp.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AgregarContactoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AgregarContactoFragment : Fragment() {

    private lateinit var botonGuardar: Button
    private lateinit var editTextNombre: EditText
    private lateinit var editTextTelefono: EditText

    private val WRITE_CONTACTS_PERMISSION_REQUEST_CODE = 123
    private lateinit var nombre: String
    private lateinit var telefono: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_agregar_contacto, container, false)

        botonGuardar = view.findViewById(R.id.botonGuardar)
        editTextNombre = view.findViewById(R.id.editTextNombre)
        editTextTelefono = view.findViewById(R.id.editTextTelefono)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        botonGuardar.setOnClickListener {
            nombre = editTextNombre.text.toString()
            telefono =editTextTelefono.text.toString()

            if (nombre.isNotEmpty() && telefono.isNotEmpty()) {
                val permission = android.Manifest.permission.WRITE_CONTACTS
                if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                    // Si no tienes el permiso, solicítalo al usuario
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), WRITE_CONTACTS_PERMISSION_REQUEST_CODE)
                } else {
                    // Si ya tienes el permiso, intenta agregar el contacto
                    agregarContactoEnAgenda(nombre, telefono)
                    // Verificar en Firebase y actualizar el RecyclerView
                    (activity as? ContactosFragment)?.verificarContactoEnFirebase(telefono)
                }
            } else {
                Toast.makeText(context, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            WRITE_CONTACTS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // El usuario concedió el permiso, ahora puedes agregar el contacto
                    agregarContactoEnAgenda(nombre, telefono)
                } else {
                    // El usuario no concedió el permiso, muestra un mensaje o toma otra acción
                    Toast.makeText(requireContext(), "Permiso denegado para escribir contactos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun agregarContactoEnAgenda(nombre: String, telefono: String) {
        try {
            val ops = ArrayList<ContentProviderOperation>()

            // Agregar operación para insertar nuevo contacto
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Agregar operación para agregar nombre
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, nombre)
                    .build()
            )

            // Agregar operación para agregar número de teléfono
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, telefono)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )

            // Aplicar las operaciones
            activity?.contentResolver?.applyBatch(ContactsContract.AUTHORITY, ops)

            // Notificar al usuario que el contacto se ha agregado correctamente
            Toast.makeText(context, "Contacto agregado correctamente", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            // Manejar errores al agregar el contacto
            Log.e("AgregarContactoFragment", "Error al agregar contacto: $e")
            Toast.makeText(context, "Error al agregar contacto", Toast.LENGTH_SHORT).show()
        }
    }

}



