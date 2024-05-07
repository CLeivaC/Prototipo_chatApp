package com.leiva.prototipo_chatapp

import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.hbb20.CountryCodePicker
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit

class Registro : AppCompatActivity() {

    private lateinit var R_et_telefono: EditText
    private lateinit var R_Btn_enviar_sms: MaterialButton
    private lateinit var R_et_codigo:EditText
    private lateinit var R_et_n_usuario: EditText
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
        initComponents()




        R_Btn_enviar_sms.setOnClickListener {
            comprobarNumero()
        }

        R_Btn_Registrar.setOnClickListener {

            val otp = R_et_codigo.text.trim().toString()
            if(otp.isNotEmpty()){
                val credential : PhoneAuthCredential = PhoneAuthProvider.getCredential(storedVerificationId,otp)
                iniciarSesionConCredencial(credential)

            }else{
                Toast.makeText(this,"Introduzca el Código",Toast.LENGTH_SHORT).show()
            }
        }



        callbacks = object: PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                startActivity(Intent(applicationContext,MainActivity::class.java))
                finish()
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                //TODO("Not yet implemented")
            }

            override fun onCodeSent(verificacionId: String, token: PhoneAuthProvider.ForceResendingToken) {
                storedVerificationId = verificacionId
                resendToken = token
            }

        }


    }

    private fun initComponents(){
        R_et_telefono = findViewById(R.id.R_et_telefono)
        R_Btn_enviar_sms = findViewById(R.id.R_Btn_enviar_sms)
        R_et_codigo = findViewById(R.id.R_et_codigo)
        R_et_n_usuario = findViewById(R.id.R_et_n_usuario)
        R_et_password = findViewById(R.id.R_et_password)
        R_et_r_password = findViewById(R.id.R_et_r_password)
        R_Btn_Registrar = findViewById(R.id.R_Btn_Registrar)
        selector_codigo_pais = findViewById(R.id.selector_codigo_pais)
        auth = FirebaseAuth.getInstance()
        spinnerPreguntas = findViewById(R.id.R_spinner_preguntas)
        R_et_respuesta = findViewById(R.id.R_et_respuesta)
    }

    private fun validarDatos(){
        if(R_et_n_usuario.text.isEmpty()){
            Toast.makeText(applicationContext, "Igrese nombre de usuario", Toast.LENGTH_SHORT)
                .show()
        }else if(R_et_password.text.isEmpty()){
            Toast.makeText(applicationContext, "Ingrese su contraseña", Toast.LENGTH_SHORT).show()
        }else if(R_et_r_password.text.isEmpty()){
            Toast.makeText(applicationContext, "Por favor repita su contraseña", Toast.LENGTH_SHORT)
                .show()
        }else if(!R_et_password.text.toString().equals(R_et_r_password.text.toString())){
            Toast.makeText(applicationContext, "Las contraseñas no coinciden", Toast.LENGTH_SHORT)
                .show()
        }else{
            registrarUsuarioBD()
        }
    }


    private fun hashPassword(password:String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(bytes)
    }


    private fun registrarUsuarioBD() {
        var uid: String = ""
        uid = auth.currentUser!!.uid
        reference = FirebaseDatabase.getInstance().reference.child("usuarios").child(uid)

        val password = R_et_password.text.toString()
        val passwordCifrada = hashPassword(password)
        val hashMap = HashMap<String,Any?>()
        val n_usuario: String = R_et_n_usuario.text.toString()
        val telefono: String = number
        val pregunta: String = spinnerPreguntas.selectedItem.toString() // Obtener la pregunta seleccionada
        val respuesta: String = R_et_respuesta.text.toString() // Obtener la respuesta proporcionada por el usuario

        hashMap["uid"] = uid
        hashMap["n_usuario"] = n_usuario
        hashMap["telefono"] = number
        hashMap["password"] = passwordCifrada
        hashMap["imagen"] = ""
        hashMap["apellido"] = ""
        hashMap["estado"] = ""
        hashMap["genero"] = ""
        hashMap["infoAdicional"] = ""
        hashMap["nombre"] = ""
        hashMap["pregunta"] = pregunta // Guardar la pregunta
        hashMap["respuesta"] = respuesta // Guardar la respuesta
        try {
            reference.updateChildren(hashMap).addOnCompleteListener { task2 ->
                if (task2.isSuccessful) {
                    val intent = Intent(this@Registro, MainActivity::class.java)
                    Toast.makeText(
                        applicationContext,
                        "Se ha registrado con éxito",
                        Toast.LENGTH_SHORT
                    ).show()
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
                number = "${selector_codigo_pais.selectedCountryCodeWithPlus}$number"
                enviarSms(number)
            } else {
                Toast.makeText(this@Registro, "Introduce un número de teléfono.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("Registro", "Error al comprobar el número: ${e.message}")
            Toast.makeText(this@Registro, "Error al comprobar el número: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun enviarSms(number: String) {
        try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(number)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
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
                }
            }.addOnFailureListener { e ->
                Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("IniciarSesion", "Error al iniciar sesión con credencial: ${e.message}")
            Toast.makeText(applicationContext, "Error al iniciar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }





}