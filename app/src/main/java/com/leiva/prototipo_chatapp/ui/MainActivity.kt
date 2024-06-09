package com.leiva.prototipo_chatapp.ui


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.leiva.prototipo_chatapp.R
import com.leiva.prototipo_chatapp.Utilidades.MyApp
import com.leiva.prototipo_chatapp.ui.Fragments.Chat.ChatFragment
import com.leiva.prototipo_chatapp.ui.Fragments.Contactos.ContactosFragment
import com.leiva.prototipo_chatapp.ui.Fragments.Perfil.PerfilFragment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private var selectedFragmentId: Int = R.id.chatF


    private lateinit var bottomNavigationView: BottomNavigationView

    var reference: DatabaseReference? = null
    var firebaseUser: FirebaseUser? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initComponents()
        navegacionFragmentos()
        restoreSelectedFragment(savedInstanceState)
        cargarEstadoSwitchClaroOscuro()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Guardar el fragmento seleccionado aquí
        outState.putInt("selected_fragment_id", selectedFragmentId)
    }

    private fun initComponents() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        firebaseUser = FirebaseAuth.getInstance().currentUser
        reference =
            FirebaseDatabase.getInstance().reference.child("usuarios").child(firebaseUser!!.uid)

    }

    private fun navegacionFragmentos() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.chatF -> replaceFrame(ChatFragment())
                R.id.contactosF -> replaceFrame(ContactosFragment())
                R.id.PerfilF -> replaceFrame(PerfilFragment())
            }
            true
        }
    }

    private fun replaceFrame(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragmentoMain, fragment)
            .commit()
    }

    private fun restoreSelectedFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.chatF
        } else {
            selectedFragmentId = savedInstanceState.getInt("selected_fragment_id", R.id.chatF)
        }
    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val switchState = (application as MyApp).readSwitchState().first()
            Log.d("estado",switchState.toString())
            if (!switchState) {
                updateUserStatus(false)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
       /* if (isFinishing) {

            updateUserStatus(true)
        }*/
        updateUserStatus(true)
    }

    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations) {
            updateUserStatus(true)
        }
        //updateUserStatus(true)
    }

    private fun updateUserStatus(boolen: Boolean) {
        val userStatusMap = mapOf("oculto" to boolen)
        reference!!.updateChildren(userStatusMap)
            .addOnSuccessListener {

                // Operación de actualización exitosa
            }
            .addOnFailureListener { e ->
                // Error al actualizar la base de datos
                Log.e("MainActivity", "Error al actualizar el estado del usuario: $e")
            }
    }

    // Restaurar el fragmento seleccionado desde SharedPreferences
    companion object {
        fun setDayNight(mode: Int) {
            if (mode == 0) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }


    private fun cargarEstadoSwitchClaroOscuro() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            // Si el usuario está autenticado, cargar el estado del switch claro/oscuro
            lifecycleScope.launch {
                val isChecked = (application as MyApp).readSwitchClaroOscuroState().first()
                // Establecer el tema según el estado del switch
                if (isChecked) {
                    setDayNight(0) // Modo oscuro
                } else {
                    setDayNight(1) // Modo claro
                }
            }
        } else {
            // Si el usuario no está autenticado, establecer siempre el modo claro
            setDayNight(1)
        }
    }



}



