package com.leiva.prototipo_chatapp

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Utiliza la función preferencesDataStore para crear la propiedad delegada userDataStore
val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class MyApp : Application() {

    // Define tu clave para el estado del switch
    private val switchStateKey = booleanPreferencesKey("switch_state")
    private val switchClaroOscuroStateKey = booleanPreferencesKey("switch_claro_oscuro_state")
    private val switchNotificationStateKey = booleanPreferencesKey("switch_Notificaciones_state")

    // Función para leer el estado del switch desde DataStore
    suspend fun readSwitchState(): Flow<Boolean> {
        return userDataStore.data
            .catch { exception ->
                // Maneja cualquier excepción durante la lectura
                if (exception is IOException) {
                    // En caso de error, emite un flujo de preferencias vacías
                    emit(emptyPreferences())
                } else {
                    // Relanza la excepción para otros casos
                    throw exception
                }
            }
            .map { preferences ->
                // Mapea las preferencias al estado del switch
                preferences[switchStateKey] ?: false // Si no se encuentra la clave, devuelve false
            }
    }

    // Función para escribir el estado del switch en DataStore
    suspend fun writeSwitchState(state: Boolean) {
        userDataStore.edit { settings ->
            settings[switchStateKey] = state
        }
    }

    suspend fun readSwitchClaroOscuroState(): Flow<Boolean> {
        return userDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[switchClaroOscuroStateKey] ?: false
            }
    }

    suspend fun writeSwitchClaroOscuroState(state: Boolean) {
        userDataStore.edit { settings ->
            settings[switchClaroOscuroStateKey] = state
        }
    }

    suspend fun readSwitchNotificacionesState(): Flow<Boolean> {
        return userDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[switchNotificationStateKey] ?: false
            }
    }

    suspend fun writeSwitchNotificacionesState(state: Boolean) {
        userDataStore.edit { settings ->
            settings[switchNotificationStateKey] = state
        }
    }
}

