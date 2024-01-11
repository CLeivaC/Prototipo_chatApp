package com.leiva.prototipo_chatapp


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract.Data
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
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

class MainActivity : AppCompatActivity() {

    private  lateinit var bottomNavigationView: BottomNavigationView

    var reference: DatabaseReference?=null
    var firebaseUser: FirebaseUser?=null
    private lateinit var nombre_usuario: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initComponents()
        replaceFrame(ChatFragment())
        navegacionFragmentos()
        obtenerDatos()


    }

    private fun initComponents(){
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        val toolbar: Toolbar = findViewById(R.id.toolbarMain)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""

        firebaseUser = FirebaseAuth.getInstance().currentUser
        reference = FirebaseDatabase.getInstance().reference.child("usuarios").child(firebaseUser!!.uid)
        nombre_usuario = findViewById(R.id.Nombre_usuario)
    }

    fun obtenerDatos(){

        reference!!.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val usuario : Usuario? = snapshot.getValue(Usuario::class.java)
                    nombre_usuario.text = usuario!!.getN_Usuario()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun navegacionFragmentos(){
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


}


