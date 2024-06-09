package com.leiva.prototipo_chatapp.Notificaciones

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.leiva.prototipo_chatapp.ui.MainActivity

// Clase que extiende FirebaseMessagingService para manejar mensajes FCM
class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Método que se llama cuando se recibe un mensaje FCM
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Extrae datos del mensaje
        val enviado = message.data["enviado"]
        val usuario = message.data["usuario"]

        // Obtiene las preferencias compartidas
        val sharedPref = getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        val usuarioActualConectado = sharedPref.getString("usuarioActual", "none")
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        // Verifica si el mensaje es para el usuario actual
        if (firebaseUser != null && enviado == firebaseUser.uid) {
            // Verifica si el usuario actual conectado es diferente del usuario en el mensaje
            if (usuarioActualConectado != usuario) {
                // Envía la notificación dependiendo de la versión de Android
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    EnviarNotificacionesOreo(message)
                } else {
                    EnviarNotificacion(message)
                }
            }
        }
    }

    // Método para enviar notificaciones en versiones de Android anteriores a Oreo
    private fun EnviarNotificacion(message: RemoteMessage) {
        val usuario = message.data["usuario"]
        val icono = message.data["icono"]
        val titulo = message.data["titulo"]
        val cuerpo = message.data["cuerpo"]
        val notificacion = message.notification
        val j = usuario!!.replace("[\\D]".toRegex(), "").toInt()
        val intent = Intent(this, MainActivity::class.java)
        val bundle = Bundle()
        bundle.putString("usuarioid", usuario)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // Crea un PendingIntent para la notificación
        val pendingIntent = PendingIntent.getActivity(this, j, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE)

        // Configura el sonido de la notificación
        val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Construye la notificación
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this)
            .setSmallIcon(icono!!.toInt())
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setSound(sonido)
            .setContentIntent(pendingIntent)

        // Muestra la notificación
        val noti = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var i = 0
        if (j > 0) {
            i = j
        }
        noti.notify(i, builder.build())
    }

    // Método para enviar notificaciones en Android Oreo y versiones posteriores
    private fun EnviarNotificacionesOreo(message: RemoteMessage) {
        val usuario = message.data["usuario"]
        val icono = message.data["icono"]
        val titulo = message.data["titulo"]
        val cuerpo = message.data["cuerpo"]
        val j = usuario!!.replace("[\\D]".toRegex(), "").toInt()
        val intent = Intent(this, MainActivity::class.java)
        val bundle = Bundle()
        bundle.putString("usuarioid", usuario)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // Crea un PendingIntent para la notificación
        val pendingIntent = PendingIntent.getActivity(this, j, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE)

        // Configura el sonido de la notificación
        val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val oreoNotification = OreoNotificacion(this)

        // Construye la notificación usando OreoNotificacion
        val builder: Notification.Builder = oreoNotification.getOreoNotification(
            titulo, cuerpo, pendingIntent, sonido, icono
        )

        var i = 0
        if (j > 0) {
            i = j
        }

        // Muestra la notificación
        oreoNotification.getManager!!.notify(i, builder.build())
    }
}
