package com.leiva.prototipo_chatapp.ui.Activities.RecovPassword

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hbb20.CountryCodePicker
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.ui.Activities.Login.VerificationState
import com.leiva.prototipo_chatapp.ui.Activities.Login.LoginActivity
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit

class RecuperarContrasena : AppCompatActivity() {

    private lateinit var etTelefono: EditText
    private lateinit var etCodigo: EditText
    private lateinit var spinnerPreguntas: Spinner
    private lateinit var etRespuesta: EditText
    private lateinit var btnEnviarCodigo: MaterialButton
    private lateinit var btnVerificarRespuesta: MaterialButton
    private lateinit var selector_codigo_pais: CountryCodePicker

    private lateinit var auth: FirebaseAuth
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar_contrasena)

        initComponents()
        initListeners()
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Este método se llama cuando se completa la verificación automáticamente
                // No necesitamos hacer nada aquí en este caso
            }

            override fun onVerificationFailed(exception: FirebaseException) {
                // Manejar la falla en la verificación del número de teléfono
                Toast.makeText(this@RecuperarContrasena, "Error en la verificación: ${exception.message}", Toast.LENGTH_SHORT).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                // Guardar el ID de verificación y el token para reenviar el código si es necesario
                storedVerificationId = verificationId
                resendToken = token
                Log.d("RecuperarContrasena", "ID de verificación almacenado: $storedVerificationId")

                // Guardar el ID de verificación en VerificationState
                VerificationState.setStoredVerificationId(storedVerificationId)
            }
        }
    }



    private fun initComponents() {
        etTelefono = findViewById(R.id.R_et_telefono)
        selector_codigo_pais = findViewById<CountryCodePicker>(R.id.selector_codigo_pais)
        etCodigo = findViewById(R.id.R_et_codigo)
        spinnerPreguntas = findViewById(R.id.R_spinner_preguntas)
        etRespuesta = findViewById(R.id.R_et_respuesta)
        btnEnviarCodigo = findViewById(R.id.R_Btn_enviar_sms)
        btnVerificarRespuesta = findViewById(R.id.confirmar)

        auth = FirebaseAuth.getInstance()

        // Configurar el adaptador para el Spinner de preguntas
        val adapter = ArrayAdapter.createFromResource(this, R.array.pregunta, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPreguntas.adapter = adapter
    }


    private fun initListeners (){
        btnEnviarCodigo.setOnClickListener {
            enviarCodigoSMS()
        }

        btnVerificarRespuesta.setOnClickListener {
            verificarRespuesta() // Llama al método verificarRespuesta cuando se presiona el botón de confirmación
        }

    }
    private fun enviarCodigoSMS() {
        try {
            var number = etTelefono.text.toString()
            number = "${selector_codigo_pais.selectedCountryCodeWithPlus}$number" // Aquí se agrega la línea que deseas

            Log.d("RecuperarContrasena", "Número de teléfono a verificar: $number")

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } catch (e: Exception) {
            Log.e("RecuperarContrasena", "Error al enviar el código SMS: ${e.message}")
            Toast.makeText(this, "Error al enviar el código SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }




    private fun verificarRespuesta() {
        try {
            var numero = etTelefono.text.toString()
            val phoneNumber = "${selector_codigo_pais.selectedCountryCodeWithPlus}$numero"
            Log.d("RecuperarContrasena", "Número de teléfono $phoneNumber")
            val respuestaIngresada = etRespuesta.text.toString()
            val preguntaSeleccionada = spinnerPreguntas.selectedItem.toString()


            val usuariosRef = FirebaseDatabase.getInstance().reference.child("usuarios")
            usuariosRef.orderByChild("telefono").equalTo(phoneNumber).addListenerForSingleValueEvent(object :
                ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        var respuestaCorrecta = false
                        for (usuarioSnapshot in dataSnapshot.children) {
                            val usuario = usuarioSnapshot.getValue(Usuario::class.java)
                            if (usuario != null && usuario.getPregunta() == preguntaSeleccionada && usuario.getRespuesta() == respuestaIngresada) {
                                respuestaCorrecta = true
                                break
                            }
                        }
                        if (respuestaCorrecta) {
                            mostrarDialogoCambiarContrasena()

                        } else {
                            Toast.makeText(
                                this@RecuperarContrasena,
                                "Pregunta o respuesta incorrecta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@RecuperarContrasena,
                            "No se encontró ningún usuario con este número de teléfono",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                private fun mostrarDialogoCambiarContrasena() {
                    try {
                        val dialogView = layoutInflater.inflate(R.layout.dialog_cambiar_contrasena, null)
                        val editTextNewPassword = dialogView.findViewById<EditText>(R.id.editTextNewPassword)
                        val btnConfirmar = dialogView.findViewById<Button>(R.id.btnConfirmar)

                        val alertDialog = AlertDialog.Builder(this@RecuperarContrasena)
                            .setView(dialogView)
                            .create()

                        btnConfirmar.setOnClickListener {
                            // Obtener el nuevo contraseña
                            val newPassword = hashPassword(editTextNewPassword.text.toString())

                            // Obtener el número de teléfono del usuario actual
                            var numero = etTelefono.text.toString()
                            val phoneNumber = "${selector_codigo_pais.selectedCountryCodeWithPlus}$numero"
                            Log.d("RecuperarContrasena", "Número de teléfono del usuario actual: $phoneNumber")

                            // Verificar si el número de teléfono existe
                            if (phoneNumber != null) {
                                // Actualizar la contraseña en la base de datos
                                val usuariosRef = FirebaseDatabase.getInstance().reference.child("usuarios")
                                usuariosRef.orderByChild("telefono").equalTo(phoneNumber).addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            for (usuarioSnapshot in dataSnapshot.children) {
                                                val usuarioId = usuarioSnapshot.key
                                                usuarioSnapshot.ref.child("password").setValue(newPassword)
                                                    .addOnCompleteListener { dbTask ->
                                                        if (dbTask.isSuccessful) {
                                                            Toast.makeText(this@RecuperarContrasena, "Contraseña cambiada exitosamente", Toast.LENGTH_SHORT).show()
                                                            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@RecuperarContrasena)
                                                            sharedPreferences.edit().putString("password", newPassword).apply()

                                                            // Cerrar y volver a abrir la aplicación
                                                            val intent = Intent(applicationContext, LoginActivity::class.java)
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            startActivity(intent)
                                                            finishAffinity() // Cierra todas las actividades de la aplicación

                                                            alertDialog.dismiss()
                                                        } else {
                                                            Toast.makeText(this@RecuperarContrasena, "Error al cambiar la contraseña en la base de datos", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                break // Solo necesitamos actualizar la contraseña para un usuario
                                            }
                                        } else {
                                            Toast.makeText(this@RecuperarContrasena, "No se encontró ningún usuario con este número de teléfono", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        Toast.makeText(this@RecuperarContrasena, "Error al recuperar los datos del usuario", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            } else {
                                Toast.makeText(this@RecuperarContrasena, "No se puede obtener el número de teléfono del usuario actual", Toast.LENGTH_SHORT).show()
                            }
                        }

                        alertDialog.show()
                    } catch (e: Exception) {
                        Log.e("RecuperarContrasena", "Error al mostrar el diálogo: ${e.message}")
                        Toast.makeText(this@RecuperarContrasena, "Error al mostrar el diálogo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@RecuperarContrasena, "Error al recuperar los datos del usuario", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        } catch (e: Exception) {
            Log.e("RecuperarContrasena", "Error al verificar respuesta: ${e.message}")
            Toast.makeText(this, "Error al verificar respuesta: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun hashPassword(password:String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(bytes)
    }


}
