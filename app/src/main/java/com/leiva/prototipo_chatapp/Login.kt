package com.leiva.prototipo_chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.hbb20.CountryCodePicker
import com.leiva.prototipo_chatapp.Modelo.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class Login : AppCompatActivity() {

    private lateinit var selector_codigo_pais_L: CountryCodePicker
    private lateinit var L_et_telefono: EditText
    private lateinit var L_enviar_otp: MaterialButton
    private lateinit var L_et_codigo: EditText
    private lateinit var L_et_password: EditText
    private lateinit var L_iniciar_sesion: MaterialButton
    private lateinit var L_registro: TextView

    private lateinit var auth: FirebaseAuth
    var reference: DatabaseReference?=null

    var number:String = ""

    lateinit var storedVerificationId: String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initComponents()
        initListeners()


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
        selector_codigo_pais_L = findViewById(R.id.selector_codigo_pais_L)
        L_et_telefono = findViewById(R.id.L_et_telefono)
        L_enviar_otp = findViewById(R.id.L_enviar_otp)
        L_et_codigo = findViewById(R.id.L_et_codigo)
        L_et_password = findViewById(R.id.L_et_password)
        L_iniciar_sesion = findViewById(R.id.L_iniciar_sesion)
        L_registro = findViewById(R.id.L_registro)
        auth = FirebaseAuth.getInstance()
    }

    private fun initListeners(){

        L_enviar_otp.setOnClickListener {
            // Utiliza una coroutine para manejar la lógica asíncrona
            CoroutineScope(Dispatchers.Main).launch {
                // Llama a la función comprobarNumero y espera su resultado
                val resultado = comprobarNumeroBD()

                // Verifica el resultado de comprobarNumero
                if (resultado) {
                    // Si el número está verificado, llama a la función comprobarYEnviarSMS
                    enviarSms(number)
                } else {
                    // Si el número no está verificado, muestra un mensaje de Toast
                    Toast.makeText(applicationContext, "Teléfono no verificado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        L_registro.setOnClickListener {
            val intent = Intent(this@Login,Registro::class.java)
            startActivity(intent)
        }

        L_iniciar_sesion.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val resultado = comprobarPasswordBD()
                val resultado2 = comprobarNumeroBD()
                if(resultado && resultado2){
                    val otp = L_et_codigo.text.trim().toString()

                    if (otp.isNotEmpty()) {
                        val credential: PhoneAuthCredential =
                            PhoneAuthProvider.getCredential(storedVerificationId, otp)
                        enviarSms(number)
                        iniciarSesionConCredencial(credential)

                    } else {
                        Toast.makeText(
                            this@Login,
                            "Introduzca el Código",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            }


        }

    }

    private suspend fun comprobarNumeroBD(): Boolean {
        number = L_et_telefono.text.trim().toString()

        return suspendCoroutine { continuation ->
            if (number.isNotEmpty()) {
                number = "${selector_codigo_pais_L.selectedCountryCodeWithPlus}$number"
                reference = FirebaseDatabase.getInstance().reference.child("usuarios")

                reference!!.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var comprobar = false

                        if (snapshot.exists()) {
                            for (sh in snapshot.children) {
                                val usuario: Usuario? = sh.getValue(Usuario::class.java)

                                if (number == usuario!!.getTelefono().toString()) {
                                    comprobar = true
                                    break
                                }
                            }
                        }

                        // Resuelve la continuación solo una vez al final del onDataChange
                        continuation.resume(comprobar)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(error.toException())
                    }
                })
            } else {
                continuation.resume(false)
            }
        }
    }



    private fun enviarSms(number: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private suspend fun comprobarPasswordBD() :Boolean{

        return suspendCoroutine { continuation ->

            reference = FirebaseDatabase.getInstance().reference.child("usuarios")
            reference!!.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var comprobar = false
                    if (snapshot.exists()) {

                        for (sh in snapshot.children) {
                            val usuario: Usuario? = sh.getValue(Usuario::class.java)
                            val passwordIngresada = L_et_password.text.toString()

                            val passwordCifradaIngresada = hashPassword(passwordIngresada)

                            if (usuario!!.getPassword().equals(passwordCifradaIngresada)) {
                                comprobar = true
                                break
                            }

                            if (!usuario.getPassword().equals(passwordCifradaIngresada)) {
                                Toast.makeText(
                                    applicationContext,
                                    "Contraseña incorrecta",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    }
                    continuation.resume(comprobar)
                    if(comprobar){

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }

            })
        }

    }


    private fun iniciarSesionConCredencial(credential: PhoneAuthCredential){
        auth.signInWithCredential(credential).addOnCompleteListener{task->
            if(task.isSuccessful){
                val intent = Intent(this@Login, MainActivity::class.java)
                Toast.makeText(
                    applicationContext,
                    "Ha iniciado sesion con éxito",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(intent)
                finish()
            }
        }.addOnFailureListener { e->
            Toast.makeText(applicationContext,"${e.message}",Toast.LENGTH_SHORT).show()
        }
    }

    private fun hashPassword(password:String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(bytes)
    }


}