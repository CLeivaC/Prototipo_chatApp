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
    }
}