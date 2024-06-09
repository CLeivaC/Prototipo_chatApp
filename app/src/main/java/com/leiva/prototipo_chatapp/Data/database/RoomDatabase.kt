package com.leiva.prototipo_chatapp.Data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.leiva.prototipo_chatapp.Data.database.dao.ContactoDao
import com.leiva.prototipo_chatapp.Data.database.dao.ImagenDao
import com.leiva.prototipo_chatapp.Data.database.dao.MensajeDao
import com.leiva.prototipo_chatapp.Data.database.dao.UsuarioDao
import com.leiva.prototipo_chatapp.Data.database.entities.ContactoEntity
import com.leiva.prototipo_chatapp.Data.database.entities.ImagenEntity
import com.leiva.prototipo_chatapp.Data.database.entities.MensajeEntity
import com.leiva.prototipo_chatapp.Data.database.entities.UsuarioData

// Anotación @Database define la configuración de la base de datos de Room
@Database(
    // Lista de entidades (tablas) que estarán en la base de datos
    entities = [UsuarioData::class, ContactoEntity::class, MensajeEntity::class, ImagenEntity::class],
    // Versión de la base de datos, importante para las migraciones
    version = 1,
    // Indica si se debe exportar el esquema de la base de datos para herramientas externas
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Métodos abstractos para obtener los DAO (Data Access Objects) de cada entidad
    abstract fun usuarioDao(): UsuarioDao
    abstract fun contactoDao(): ContactoDao
    abstract fun mensajeDao(): MensajeDao
    abstract fun imagenDao(): ImagenDao

    // Compañero de objeto para implementar el patrón Singleton
    companion object {
        // Variable para almacenar la instancia única de la base de datos
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Método para obtener la instancia única de la base de datos
        fun getInstance(context: Context): AppDatabase {
            // Si INSTANCE es null, se crea una nueva instancia en un bloque sincronizado
            return INSTANCE ?: synchronized(this) {
                // Se construye la base de datos usando Room.databaseBuilder
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mi_app_database" // Nombre del archivo de la base de datos
                ).build()
                // Se asigna la nueva instancia a INSTANCE
                INSTANCE = instance
                // Se devuelve la instancia
                instance
            }
        }
    }

}