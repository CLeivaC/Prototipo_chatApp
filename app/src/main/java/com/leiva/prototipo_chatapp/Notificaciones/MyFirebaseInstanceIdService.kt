package com.leiva.prototipo_chatapp.Notificaciones

import android.text.TextUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService

// Clase que extiende FirebaseMessagingService para manejar eventos relacionados con FCM
class MyFirebaseInstanceIdService : FirebaseMessagingService() {

    // Método que se llama cuando se genera un nuevo token
    override fun onNewToken(token: String) {
        // Obtener el usuario actual de Firebase Authentication
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        // Obtener el token de FCM
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful) {
                    // Si la tarea es exitosa, obtener el resultado (el token)
                    if (tarea.result != null && !TextUtils.isEmpty(tarea.result)) {
                        val mi_token: String = tarea.result!!
                        // Si el usuario está autenticado, actualizar el token
                        if (firebaseUser != null) {
                            ActualizarToken(mi_token)
                        }
                    }
                }
            }
    }

    // Método privado para actualizar el token en la base de datos
    private fun ActualizarToken(miToken: String) {
        // Obtener el usuario actual de Firebase Authentication
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        // Obtener una referencia a la base de datos de Firebase
        val reference = FirebaseDatabase.getInstance().getReference().child("Tokens")
        // Crear un objeto Token con el nuevo token
        val token = Token(miToken)
        // Guardar el token en la base de datos bajo el UID del usuario actual
        reference.child(firebaseUser!!.uid).setValue(token)
    }
}