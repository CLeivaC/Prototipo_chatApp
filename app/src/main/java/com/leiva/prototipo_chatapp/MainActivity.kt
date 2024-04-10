package com.leiva.prototipo_chatapp


import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.leiva.prototipo_chatapp.Fragmentos.ChatFragment
import com.leiva.prototipo_chatapp.Fragmentos.ContactosFragment
import com.leiva.prototipo_chatapp.Fragmentos.JuegosFragment
import com.leiva.prototipo_chatapp.Fragmentos.PerfilFragment
import com.leiva.prototipo_chatapp.Modelo.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "UserPrefs"
        const val USER_STATUS_KEY = "UserStatus"
    }


    private lateinit var bottomNavigationView: BottomNavigationView

    var reference: DatabaseReference? = null
    var firebaseUser: FirebaseUser? = null
    private lateinit var nombre_usuario: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initComponents()
        replaceFrame(ChatFragment())
        navegacionFragmentos()
        obtenerDatos()
    }

    private fun initComponents() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        val toolbar: Toolbar = findViewById(R.id.toolbarMain)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""

        firebaseUser = FirebaseAuth.getInstance().currentUser
        reference =
            FirebaseDatabase.getInstance().reference.child("usuarios").child(firebaseUser!!.uid)
        nombre_usuario = findViewById(R.id.Nombre_usuario)

    }

    fun obtenerDatos() {

        reference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val usuario: Usuario? = snapshot.getValue(Usuario::class.java)
                    nombre_usuario.text = usuario!!.getN_Usuario()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun navegacionFragmentos() {
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.chatF ->
                    replaceFrame(ChatFragment())

                R.id.contactosF ->
                    replaceFrame(ContactosFragment())

                R.id.juegosF ->
                    replaceFrame(JuegosFragment())

                R.id.PerfilF ->
                    replaceFrame(PerfilFragment())

                else -> {
                }
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

    override fun onPause() {
        super.onPause()
        updateUserStatus("Oculto")
    }

    override fun onResume() {
        super.onResume()
        updateUserStatus("En línea")
    }

    private fun updateUserStatus(status: String) {
        val userStatusMap = mapOf("estado" to status)
        reference!!.updateChildren(userStatusMap)
            .addOnSuccessListener {
                // Operación de actualización exitosa
            }
            .addOnFailureListener { e ->
                // Error al actualizar la base de datos
                Log.e("MainActivity", "Error al actualizar el estado del usuario: $e")
            }
    }


}


