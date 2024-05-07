package com.leiva.prototipo_chatapp


import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
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
import com.leiva.prototipo_chatapp.Modelo.Chat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private var selectedFragmentId: Int = R.id.chatF


    private lateinit var bottomNavigationView: BottomNavigationView

    var reference: DatabaseReference? = null
    var firebaseUser: FirebaseUser? = null

    //val ref = FirebaseDatabase.getInstance().reference.child("chats")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initComponents()
        navegacionFragmentos()
        restoreSelectedFragment(savedInstanceState)
        cargarEstadoSwitchClaroOscuro()

        /*ref.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val viewPagerAdapter = supportFragmentManager
                var contMensajesNoLeidos = 0
                for(dataSnapshot in snapshot.children){
                    val chat = dataSnapshot.getValue(Chat::class.java)
                    if(chat!!.getReceptor().equals(firebaseUser!!.uid) && !chat.isVisto()){

                        contMensajesNoLeidos+=1
                    }
                }
                if(contMensajesNoLeidos == 0){
                    viewPagerAdapter.addItem(ChatFragment(),"Chats")
                }else{
                    viewPagerAdapter.addItem(ChatFragment(),"[$contMensajesNoLeidos] Chats")
                }

                viewPagerAdapter.addItem(ContactosFragment(),"Contctos")
                viewPager.adapter= viewPagerAdapter
                tabLayout.setupWithViewPager(viewPager)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })*/

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
                R.id.juegosF -> replaceFrame(JuegosFragment())
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
            if (!switchState) {
                updateUserStatus(false)
            }
        }

    }

   override fun onDestroy() {
        super.onDestroy()
       if (isFinishing) {
           updateUserStatus(true)
       }
    }

    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations) {
            updateUserStatus(true)
        }
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
    fun setDayNight(mode: Int) {
        if (mode == 0) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }


    private fun cargarEstadoSwitchClaroOscuro() {
        lifecycleScope.launch {
            val isChecked = (application as MyApp).readSwitchClaroOscuroState().first()
            // Establecer el tema según el estado del switch
            if (isChecked) {
                setDayNight(0) // Modo oscuro
            } else {
                setDayNight(1) // Modo claro
            }
        }
    }

}


