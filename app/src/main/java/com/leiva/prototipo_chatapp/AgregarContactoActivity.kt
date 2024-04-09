package com.leiva.prototipo_chatapp

import android.app.Activity
import android.content.ContentProviderOperation
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.leiva.prototipo_chatapp.Fragmentos.ContactosFragment
import com.leiva.prototipo_chatapp.Modelo.Usuario


class AgregarContactoActivity : AppCompatActivity() {

    private lateinit var botonGuardar: Button
    private lateinit var editTextNombre: EditText
    private lateinit var editTextTelefono: EditText

    private val WRITE_CONTACTS_PERMISSION_REQUEST_CODE = 123
    private lateinit var nombre: String
    private lateinit var telefono: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_contacto)
        initComponents()
        initListeners()
    }

    private fun initComponents(){
        botonGuardar = findViewById(R.id.botonGuardar)
        editTextNombre = findViewById(R.id.editTextNombre)
        editTextTelefono = findViewById(R.id.editTextTelefono)
    }

    private fun initListeners(){
        botonGuardar.setOnClickListener {
            nombre = editTextNombre.text.toString()
            telefono =editTextTelefono.text.toString()

            if (nombre.isNotEmpty() && telefono.isNotEmpty()) {
                val permission = android.Manifest.permission.WRITE_CONTACTS
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    // Si no tienes el permiso, solicítalo al usuario
                    ActivityCompat.requestPermissions(this, arrayOf(permission), WRITE_CONTACTS_PERMISSION_REQUEST_CODE)
                } else {
                    // Si ya tienes el permiso, intenta agregar el contacto
                    agregarContactoEnAgenda(nombre, telefono)


                }
            } else {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
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
            this.contentResolver?.applyBatch(ContactsContract.AUTHORITY, ops)

            // Notificar al usuario que el contacto se ha agregado correctamente
            Toast.makeText(this, "Contacto agregado correctamente", Toast.LENGTH_SHORT).show()

            // Informar al fragmento ContactosFragment que se ha agregado un contacto
            setResult(Activity.RESULT_OK)

            // Finalizar la actividad
            finish()

        } catch (e: Exception) {
            // Manejar errores al agregar el contacto
            Log.e("AgregarContactoFragment", "Error al agregar contacto: $e")
            Toast.makeText(this, "Error al agregar contacto", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, "Permiso denegado para escribir contactos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

