package com.leiva.prototipo_chatapp.Utilidades

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract

class UtilidadesChat {
    companion object {
        fun obtenerNombreDesdeTelefono(contentResolver: ContentResolver, numeroTelefono: String): String {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?",
                arrayOf(numeroTelefono),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val nombreColumnIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    if (nombreColumnIndex != -1) {
                        return it.getString(nombreColumnIndex)
                    }
                }
            }

            // Si no se encuentra el nombre en la agenda, devolver el número de teléfono como nombre
            return numeroTelefono
        }

        fun obtenerNombreDesdeTelefonoLimpio(contentResolver: ContentResolver, numeroTelefono: String): String {
            // Limpiar el número de teléfono eliminando caracteres no numéricos
            val numeroLimpio = limpiarNumeroTelefono(numeroTelefono)

            // Consultar la agenda del dispositivo
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?",
                arrayOf(numeroLimpio),
                null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val nombreColumnIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    if (nombreColumnIndex != -1) {
                        return it.getString(nombreColumnIndex)
                    }
                }
            }

            // Si no se encuentra el nombre en la agenda, devolver el número de teléfono como nombre
            return numeroLimpio
        }

       private fun limpiarNumeroTelefono(numero: String): String {
            // Eliminar todos los caracteres que no sean dígitos
            val numeroLimpio = numero.replace(Regex("[^\\d]"), "")
            // Agregar el prefijo de país si no está presente
            if (!numeroLimpio.startsWith("+")) {
                return "+$numeroLimpio"
            }
            return numeroLimpio
        }
    }


}