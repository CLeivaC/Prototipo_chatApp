package com.leiva.prototipo_chatapp.ui.Activities.AgregarContacto

import android.Manifest
import android.app.Activity
import android.content.ContentProviderOperation
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hbb20.CountryCodePicker
import com.leiva.prototipo_chatapp.R


class AgregarContactoActivity : AppCompatActivity() {

    private lateinit var botonGuardar: Button
    private lateinit var editTextNombre: EditText
    private lateinit var editTextTelefono: EditText

    private val WRITE_CONTACTS_PERMISSION_REQUEST_CODE = 123
    private lateinit var nombre: String
    private lateinit var telefono: String
    private lateinit var countryCodePicker:CountryCodePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_contacto)
        initComponents()
        checkAndRequestPermissions()
        initListeners()
    }

    private fun initComponents() {
        botonGuardar = findViewById(R.id.botonGuardar)
        editTextNombre = findViewById(R.id.editTextNombre)
        editTextTelefono = findViewById(R.id.editTextTelefono)
        countryCodePicker = findViewById(R.id.countryCodePicker)
        // Registrar el EditText del teléfono con el CountryCodePicker
        countryCodePicker.registerCarrierNumberEditText(editTextTelefono)
    }

    private fun checkAndRequestPermissions() {
        val permission = Manifest.permission.WRITE_CONTACTS
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), WRITE_CONTACTS_PERMISSION_REQUEST_CODE)
        }
    }

    private fun initListeners() {
        botonGuardar.setOnClickListener {
            nombre = editTextNombre.text.toString()
            telefono = editTextTelefono.text.toString()

            if (nombre.isNotEmpty() && telefono.isNotEmpty()) {
                val permission = android.Manifest.permission.WRITE_CONTACTS
                if (ContextCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Si no tienes el permiso, solicítalo al usuario
                    ActivityCompat.requestPermissions(this, arrayOf(permission),
                        WRITE_CONTACTS_PERMISSION_REQUEST_CODE
                    )
                } else {
                    // Limpiar el número de teléfono para eliminar espacios u otros caracteres no numéricos
                    val cleanedPhoneNumber = telefono.replace("[^0-9]".toRegex(), "")

                    // Validar que el número de teléfono tenga 9 dígitos si el código de país es España
                    if (countryCodePicker.selectedCountryNameCode =="ES" && cleanedPhoneNumber.length != 9) {
                        Toast.makeText(this, "El número de teléfono debe tener 9 dígitos", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener // Salir de la función sin continuar
                    }
                    // Verificar si el número de teléfono contiene el prefijo internacional "+"
                    else {
                        val fullPhoneNumber = countryCodePicker.fullNumberWithPlus

                        // Si ya tienes el permiso y el número de teléfono tiene el prefijo "+", intenta agregar el contacto
                        agregarContactoEnAgenda(nombre, fullPhoneNumber)
                    }
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
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )
            // Agregar operación para agregar nombre
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, nombre)
                    .build()
            )
            // Agregar operación para agregar número de teléfono
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
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
            Toast.makeText(this, "Contacto agregado correctamente",
                Toast.LENGTH_SHORT).show()
            // Informar al fragmento ContactosFragment que se ha agregado un contacto
            setResult(Activity.RESULT_OK)
            // Finalizar la actividad
            finish()
        } catch (e: Exception) {
            // Manejar errores al agregar el contacto
            Log.e("AgregarContactoFragment", "Error al agregar contacto: $e")
            Toast.makeText(this, "Error al agregar contacto",
                Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            WRITE_CONTACTS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // El usuario concedió el permiso, ahora puedes agregar el contacto
                } else {
                    // El usuario no concedió el permiso, muestra un mensaje o toma otra acción
                    Toast.makeText(this, "Permiso denegado para escribir contactos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

