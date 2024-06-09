package com.leiva.prototipo_chatapp.ui.Activities.Registro

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hbb20.CountryCodePicker
import com.leiva.prototipo_chatapp.Modelo.Usuario
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.ui.MainActivity
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Registro : AppCompatActivity() {

    private lateinit var R_et_telefono: EditText
    private lateinit var R_Btn_enviar_sms: MaterialButton
    private lateinit var R_et_codigo:EditText
    private lateinit var R_et_password: EditText
    private lateinit var R_et_r_password: EditText
    private lateinit var R_Btn_Registrar: MaterialButton
    private lateinit var selector_codigo_pais: CountryCodePicker
    private lateinit var spinnerPreguntas: Spinner
    private lateinit var R_et_respuesta: EditText

    lateinit var auth: FirebaseAuth
    private lateinit var reference: DatabaseReference

    var number:String = ""

    lateinit var storedVerificationId: String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        MainActivity.setDayNight(1)
        initComponents()
        R_Btn_enviar_sms.setOnClickListener {
            comprobarNumero()
        }

        R_Btn_Registrar.setOnClickListener {
            if (isNetworkAvailable()) {
                val telefono = R_et_telefono.text.toString().trim()
                val password = R_et_password.text.toString().trim()
                val repPassword = R_et_r_password.text.toString().trim()
                val otp = R_et_codigo.text.trim().toString()

                if (telefono.isEmpty() || password.isEmpty() || repPassword.isEmpty() || otp.isEmpty()) {
                    Toast.makeText(this, "Rellene los campos vacíos", Toast.LENGTH_SHORT).show()
                } else {
                    if (password != repPassword) {
                        Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                    } else {
                        val credential: PhoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId, otp)
                        iniciarSesionConCredencial(credential)
                    }
                }
            } else {
                Toast.makeText(this, "No hay conexión a Internet", Toast.LENGTH_SHORT).show()
            }
        }

        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("Verification", "onVerificationCompleted: $credential")
                iniciarSesionConCredencial(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("Verification", "onVerificationFailed: ${e.message}")
                when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        Log.e("Verification", "Número de teléfono inválido: ${e.message}")
                        Toast.makeText(this@Registro, "Número de teléfono inválido.", Toast.LENGTH_SHORT).show()
                    }
                    is FirebaseTooManyRequestsException -> {
                        Log.e("Verification", "Se ha excedido el límite de solicitudes.")
                        Toast.makeText(this@Registro, "Se ha excedido el límite de solicitudes. Intenta de nuevo más tarde.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.e("Verification", "Fallo en la verificación: ${e.message}")
                        Toast.makeText(this@Registro, "Fallo en la verificación: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d("Verification", "onCodeSent: $verificationId")
                storedVerificationId = verificationId
                resendToken = token
            }
        }



    }

    private fun initComponents(){
        R_et_telefono = findViewById(R.id.R_et_telefono)
        R_Btn_enviar_sms = findViewById(R.id.R_Btn_enviar_sms)
        R_et_codigo = findViewById(R.id.R_et_codigo)

        R_et_password = findViewById(R.id.R_et_password)
        R_et_r_password = findViewById(R.id.R_et_r_password)
        R_Btn_Registrar = findViewById(R.id.R_Btn_Registrar)
        selector_codigo_pais = findViewById(R.id.selector_codigo_pais)
        auth = FirebaseAuth.getInstance()
        spinnerPreguntas = findViewById(R.id.R_spinner_preguntas)
        R_et_respuesta = findViewById(R.id.R_et_respuesta)
    }

    private fun validarDatos(){
        if(R_et_telefono.text.isEmpty()){
            Toast.makeText(applicationContext, "Ingrese su número de teléfono", Toast.LENGTH_SHORT).show()
        } else if(R_et_password.text.isEmpty()){
            Toast.makeText(applicationContext, "Ingrese su contraseña", Toast.LENGTH_SHORT).show()
        } else if(R_et_r_password.text.isEmpty()){
            Toast.makeText(applicationContext, "Por favor repita su contraseña", Toast.LENGTH_SHORT).show()
        } else if(R_et_password.text.toString() != R_et_r_password.text.toString()){
            Toast.makeText(applicationContext, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
        } else{
            registrarUsuarioBD()
        }
    }

    private fun hashPassword(password:String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(bytes)
    }


    private fun registrarUsuarioBD() {
        val uid = auth.currentUser?.uid ?: return
        val telefono = "${selector_codigo_pais.selectedCountryCodeWithPlus}${R_et_telefono.text.trim()}"
        val password = R_et_password.text.toString()
        val passwordCifrada = hashPassword(password)
        val pregunta = spinnerPreguntas.selectedItem.toString() // Obtener la pregunta seleccionada
        val respuesta = R_et_respuesta.text.toString() // Obtener la respuesta proporcionada por el usuario

        val hashMap = HashMap<String, Any?>()
        hashMap["uid"] = uid
        hashMap["telefono"] = telefono
        hashMap["password"] = passwordCifrada
        hashMap["imagen"] = ""
        hashMap["fondoPerfilUrl"] = ""
        hashMap["apellido"] = ""
        hashMap["infoAdicional"] = ""
        hashMap["nombre"] = ""
        hashMap["pregunta"] = pregunta // Guardar la pregunta
        hashMap["respuesta"] = respuesta // Guardar la respuesta

        reference = FirebaseDatabase.getInstance().reference.child("usuarios").child(uid)

        try {
            reference.updateChildren(hashMap).addOnCompleteListener { task2 ->
                if (task2.isSuccessful) {
                    val intent = Intent(this@Registro, MainActivity::class.java)
                    Toast.makeText(applicationContext, "Se ha registrado con éxito", Toast.LENGTH_SHORT).show()
                    startActivity(intent)
                }
            }.addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Error al registrar usuario: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("Registro", "Error al registrar usuario: ${e.message}")
            Toast.makeText(applicationContext, "Error al registrar usuario: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun comprobarNumero() {
        try {
            var number = R_et_telefono.text.trim().toString()

            if (number.isNotEmpty()) {
                // Combinar el código de país seleccionado con el número de teléfono
                number = "${selector_codigo_pais.selectedCountryCodeWithPlus}$number"

                // Verificar si el código de país es de España y el número tiene 9 dígitos
                if (selector_codigo_pais.selectedCountryNameCode == "ES" && number.length != 12) {
                    Toast.makeText(this@Registro, "El número de teléfono debe tener 9 dígitos", Toast.LENGTH_SHORT).show()
                    return
                }

                if (isNetworkAvailable()) {
                    // Verificar si el número ya está registrado
                    verificarNumeroRegistrado(number)
                } else {
                    Toast.makeText(this@Registro, "No hay conexión a Internet", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@Registro, "Por favor, introduzca un número de teléfono", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("Registro", "Error al comprobar el número: ${e.message}")
            Toast.makeText(this@Registro, "Error al comprobar el número: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificarNumeroRegistrado(number: String) {
        val usersRef = FirebaseDatabase.getInstance().reference.child("usuarios")
        usersRef.orderByChild("telefono").equalTo(number).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // El número ya está registrado
                    Toast.makeText(this@Registro, "Este número de teléfono ya está registrado.", Toast.LENGTH_SHORT).show()
                } else {
                    // El número no está registrado, enviar SMS para verificación
                    enviarSms(number)
                    Toast.makeText(this@Registro, "Código de verificación enviado.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Registro", "Error al verificar el número: ${databaseError.message}")
                Toast.makeText(this@Registro, "Error al verificar el número: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun enviarSms(number: String) {
        try {
            Log.d("EnviarSMS", "Intentando enviar SMS al número: $number")
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number) // Número de teléfono a verificar
                .setTimeout(60L, TimeUnit.SECONDS) // Tiempo de espera para el código OTP
                .setActivity(this) // Actividad actual
                .setCallbacks(callbacks) // Callbacks de verificación
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
            Log.d("EnviarSMS", "Solicitud de verificación de SMS enviada")
        } catch (e: Exception) {
            Log.e("EnviarSMS", "Error al enviar SMS: ${e.message}")
            Toast.makeText(applicationContext, "Error al enviar SMS: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun iniciarSesionConCredencial(credential: PhoneAuthCredential) {
        try {
            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    validarDatos()
                } else {
                    Toast.makeText(applicationContext, "Error al iniciar sesión: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(applicationContext, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("IniciarSesion", "Error al iniciar sesión con credencial: ${e.message}")
            Toast.makeText(applicationContext, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun Context.isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


}