package com.leiva.prototipo_chatapp.Notificaciones

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Client {
    // Objeto Singleton para mantener una única instancia de Retrofit
    object Client {
        // Variable para almacenar la instancia de Retrofit
        private var retrofit: Retrofit? = null

        // Método para obtener la instancia de Retrofit
        fun getClient(url: String?): Retrofit? {
            // Si retrofit es null, se crea una nueva instancia
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    // Configura la URL base para las solicitudes
                    .baseUrl(url)
                    // Añade un convertidor para transformar JSON a objetos de Kotlin (y viceversa)
                    .addConverterFactory(GsonConverterFactory.create())
                    // Construye la instancia de Retrofit
                    .build()
            }
            // Devuelve la instancia de Retrofit (nueva o existente)
            return retrofit
        }
    }
}