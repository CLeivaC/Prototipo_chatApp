    package com.leiva.prototipo_chatapp

    import android.app.Activity
    import android.content.ContentValues.TAG
    import android.content.Intent
    import android.content.SharedPreferences
    import android.os.Bundle
    import android.preference.PreferenceManager
    import android.util.Log
    import android.view.View
    import android.widget.CheckBox
    import android.widget.EditText
    import android.widget.TextView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import com.google.android.material.button.MaterialButton
    import com.google.firebase.FirebaseException
    import com.google.firebase.auth.FirebaseAuth
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
        private lateinit var L_checkbox_recuerdame: CheckBox
        private lateinit var sharedPreferences: SharedPreferences


        private lateinit var auth: FirebaseAuth



        var reference: DatabaseReference?=null

        var number:String = ""

        var storedVerificationId: String = ""
        lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
        private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_login)
            auth = FirebaseAuth.getInstance()
            initComponents()
            initListeners()
            inicializarCheckBoxRecuerdame()

            storedVerificationId = VerificationState.getStoredVerificationId().toString()
            Log.d(TAG, "Stored verification ID al iniciar Login: $storedVerificationId")
            callbacks = object: PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                    startActivity(Intent(applicationContext,MainActivity::class.java))
                    finish()
                }

                override fun onVerificationFailed(p0: FirebaseException) {
                    //TODO: Manejar el error de verificación
                }

                override fun onCodeSent(verificacionId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    storedVerificationId = verificacionId
                    resendToken = token
                    Log.d(TAG, "Stored verification ID updated: $storedVerificationId")
                    Toast.makeText(applicationContext, "Código de verificación enviado", Toast.LENGTH_SHORT).show()

                }
            }

        }

        private fun inicializarCheckBoxRecuerdame() {
            // Inicializa la preferencia compartida
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

            // Inicializa el CheckBox
            L_checkbox_recuerdame = findViewById(R.id.L_checkbox_recuerdame)

            // Recupera el valor de la preferencia compartida y actualiza el estado del CheckBox
            val recordarSesion = sharedPreferences.getBoolean("recordar_sesion", false)
            L_checkbox_recuerdame.isChecked = recordarSesion
        }

        //Permanecer sesion abierto
        override fun onStart() {
            super.onStart()

            // Verifica si el usuario ya está autenticado
            val currentUser = auth.currentUser
            val recordarSesion = sharedPreferences.getBoolean("recordar_sesion", false)

            if (recordarSesion && currentUser != null) {
                // Si "Recuérdame" está marcado y hay un usuario autenticado, redirige a la actividad principal
                startActivity(Intent(this, MainActivity::class.java))
                finish()
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
            L_checkbox_recuerdame = findViewById(R.id.L_checkbox_recuerdame)
        }

        private fun initListeners(){


            L_checkbox_recuerdame.setOnCheckedChangeListener { buttonView, isChecked ->
                // Guarda el estado seleccionado en las preferencias compartidas
                sharedPreferences.edit().putBoolean("recordar_sesion", isChecked).apply()
            }

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
                            Log.d(TAG, "Stored verification ID: $storedVerificationId")
                            val credential: PhoneAuthCredential =
                                PhoneAuthProvider.getCredential(storedVerificationId, otp)
                            iniciarSesionConCredencial(credential)
                        } else {
                            Toast.makeText(
                                this@Login,
                                "Por favor, ingrese el código SMS",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

        }

        private suspend fun comprobarNumeroBD(): Boolean {
            return suspendCoroutine { continuation ->
                try {
                    number = L_et_telefono.text.trim().toString()
                    Log.i(TAG, "Número obtenido del campo de texto: $number")

                    if (number.isNotEmpty()) {
                        number = "${selector_codigo_pais_L.selectedCountryCodeWithPlus}$number"
                        Log.i(TAG, "Número formateado con el código de país: $number")

                        reference = FirebaseDatabase.getInstance().reference.child("usuarios")

                        reference!!.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var comprobar = false

                                if (snapshot.exists()) {
                                    for (sh in snapshot.children) {
                                        val usuario: Usuario? = sh.getValue(Usuario::class.java)

                                        if (number == usuario!!.getTelefono().toString()) {
                                            comprobar = true
                                            Log.d(TAG, "Número encontrado en la base de datos")
                                            break
                                        }
                                    }
                                }

                                // Resuelve la continuación solo una vez al final del onDataChange
                                Log.i(TAG, "Resultado de la comprobación: $comprobar")
                                continuation.resume(comprobar)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e(TAG, "Error al comprobar el número en la base de datos: ${error.message}")
                                Toast.makeText(applicationContext, "Error al comprobar el número", Toast.LENGTH_SHORT).show()
                                continuation.resume(false)
                            }
                        })
                    } else {
                        Log.i(TAG, "El número está vacío")
                        continuation.resume(false)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al comprobar el número en la base de datos: ${e.message}")
                    Toast.makeText(applicationContext, "Error al comprobar el número", Toast.LENGTH_SHORT).show()
                    continuation.resume(false)
                }
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
                Log.e(TAG, "Error al enviar SMS: ${e.message}")
                Toast.makeText(applicationContext, "Error al enviar SMS", Toast.LENGTH_SHORT).show()
            }
        }

        private suspend fun comprobarPasswordBD(): Boolean {
            return suspendCoroutine { continuation ->
                try {
                    reference = FirebaseDatabase.getInstance().reference.child("usuarios")
                    Log.i(TAG, "Obteniendo referencia de la base de datos de usuarios")

                    reference!!.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            var comprobar = false
                            var passwordCorrecta = false  // Variable para comprobar si la contraseña es correcta
                            if (snapshot.exists()) {
                                for (sh in snapshot.children) {
                                    val usuario: Usuario? = sh.getValue(Usuario::class.java)
                                    val passwordIngresada = L_et_password.text.toString()
                                    val passwordCifradaIngresada = hashPassword(passwordIngresada)
                                    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this@Login)
                                    val storedPassword = sharedPreferences.getString("password", "") ?: ""
                                    Log.i(TAG, "Contraseña ingresada: $passwordIngresada")
                                    Log.i(TAG, "Contraseña cifrada ingresada: $passwordCifradaIngresada")
                                    Log.i(TAG, "Contraseña almacenada: $storedPassword")
                                    // Comprobar si la contraseña coincide con la contraseña del usuario actual
                                    if (usuario!!.getPassword().equals(passwordCifradaIngresada) && usuario.getTelefono().equals("${selector_codigo_pais_L.selectedCountryCodeWithPlus}${L_et_telefono.text}")) {
                                        comprobar = true
                                        passwordCorrecta = true  // Contraseña correcta
                                        Log.d(TAG, "Contraseña correcta para el usuario: ${usuario.getTelefono()}")
                                        break
                                    }
                                }

                                // Si ninguna contraseña coincide, mostrar el mensaje de contraseña incorrecta
                                if (!passwordCorrecta) {
                                    Log.d(TAG, "Contraseña incorrecta")
                                    Toast.makeText(applicationContext, "Contraseña incorrecta", Toast.LENGTH_SHORT).show()
                                }
                            }
                            continuation.resume(comprobar)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Error al comprobar la contraseña en la base de datos: ${error.message}")
                            Toast.makeText(applicationContext, "Error al comprobar la contraseña", Toast.LENGTH_SHORT).show()
                            continuation.resume(false)
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error al comprobar la contraseña en la base de datos: ${e.message}")
                    Toast.makeText(applicationContext, "Error al comprobar la contraseña", Toast.LENGTH_SHORT).show()
                    continuation.resume(false)
                }
            }
        }



        private fun iniciarSesionConCredencial(credential: PhoneAuthCredential) {
            try {
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Inicio de sesión exitoso")
                        val intent = Intent(this@Login, MainActivity::class.java)
                        Toast.makeText(
                            applicationContext,
                            "Ha iniciado sesión con éxito",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(intent)
                        finish()
                    }
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Error al iniciar sesión: ${e.message}")
                    Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar sesión: ${e.message}")
                Toast.makeText(applicationContext, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
            }
        }


        private fun hashPassword(password:String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
            val hashedPassword = Base64.getEncoder().encodeToString(bytes)
            Log.d(TAG, "Contraseña cifrada: $hashedPassword")
            return Base64.getEncoder().encodeToString(bytes)
        }

        fun irARecuperarContrasena(view: View) {
            // Aquí coloca la lógica para ir a la pantalla de recuperación de contraseña
            // Por ejemplo:
            val intent = Intent(this, RecuperarContrasena::class.java)
            startActivity(intent)
        }




    }