package com.leiva.prototipo_chatapp.Notificaciones

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import androidx.core.content.getSystemService

// Clase para gestionar las notificaciones en Android Oreo y versiones posteriores
class OreoNotificacion(base: Context) : ContextWrapper(base) {

    // Variable para el administrador de notificaciones
    private var notificationManager: NotificationManager? = null

    // Constantes para el canal de notificaciones
    companion object {
        private const val CHANNEL_ID = "com.leiva.prototipo_chatapp"
        private const val CHANNEL_NAME = "Chat App"
    }

    // Bloque init para inicializar el canal de notificaciones si la versión es Oreo o superior
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CrearCanal()
        }
    }

    // Método para crear el canal de notificaciones (solo para Oreo y versiones superiores)
    @TargetApi(Build.VERSION_CODES.O)
    private fun CrearCanal() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.enableLights(false)
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getManager!!.createNotificationChannel(channel)
    }

    // Propiedad para obtener el administrador de notificaciones, creándolo si es necesario
    val getManager: NotificationManager?
        get() {
            if (notificationManager == null) {
                notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            return notificationManager
        }

    // Método para construir una notificación en Android Oreo y versiones superiores
    @TargetApi(Build.VERSION_CODES.O)
    fun getOreoNotification(
        titulo: String?,
        cuerpo: String?,
        pendingIntent: PendingIntent,
        sonidoUri: Uri?,
        icono: String?
    ): Notification.Builder {
        return Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setSmallIcon(icono!!.toInt())
            .setSound(sonidoUri)
            .setAutoCancel(true)
    }
}
