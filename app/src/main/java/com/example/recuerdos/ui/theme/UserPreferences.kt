package com.example.recuerdos.ui.theme

import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensión de Context para DataStore
val Context.dataStore by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_TYPE_KEY = stringPreferencesKey("user_type") // "personal" o "cuidador"
        val IS_REGISTERED_KEY = booleanPreferencesKey("is_registered")
    }

    // Guardar nombre de usuario y tipo
    suspend fun saveUserInfo(name: String, userType: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
            preferences[USER_TYPE_KEY] = userType
            preferences[IS_REGISTERED_KEY] = true
        }
    }

    // Obtener nombre de usuario
    val userName: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME_KEY] ?: ""
        }

    // Obtener tipo de usuario
    val userType: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_TYPE_KEY] ?: "personal" // Valor por defecto
        }

    // Verificar si ya está registrado
    val isRegistered: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_REGISTERED_KEY] ?: false
        }
}