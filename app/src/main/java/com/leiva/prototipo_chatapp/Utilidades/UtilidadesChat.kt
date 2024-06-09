package com.leiva.prototipo_chatapp.Utilidades

import android.content.ContentResolver
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log

class UtilidadesChat {
    companion object {
        // Función estática para obtener el nombre asociado a un número de teléfono desde los contactos del dispositivo
        fun obtenerNombreDesdeTelefono(contentResolver: ContentResolver, numeroTelefono: String): String {
            val numeroNormalizado = numeroTelefono.replace(Regex("[^\\d]"), "")
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val numeroIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nombreIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                    if (numeroIndex != -1 && nombreIndex != -1) {
                        val numeroEnContacto = it.getString(numeroIndex).replace(Regex("[^\\d]"), "")
                        Log.d("ContactLookup", "Número en contacto: $numeroEnContacto, Número buscado: $numeroNormalizado")

                        if (numeroEnContacto == numeroNormalizado) {
                            val nombre = it.getString(nombreIndex)
                            Log.d("ContactLookup", "Nombre encontrado: $nombre")
                            return nombre
                        }
                    }
                }
            }

            Log.d("ContactLookup", "No se encontró el nombre para el número: $numeroNormalizado")
            return numeroTelefono
        }
    }



}