package com.meya.doumenculture.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

object TokenManager {

    private const val TOKEN_KEY = "auth_token"
    private const val USER_KEY = "current_user"

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val dataStore: DataStore<Preferences>
        get() = appContext.dataStore

    val token: Flow<String?>
        get() = dataStore.data.map { it[stringPreferencesKey(TOKEN_KEY)] }

    val userJson: Flow<String?>
        get() = dataStore.data.map { it[stringPreferencesKey(USER_KEY)] }

    suspend fun saveToken(token: String) {
        dataStore.edit { it[stringPreferencesKey(TOKEN_KEY)] = token }
    }

    suspend fun saveUserJson(json: String) {
        dataStore.edit { it[stringPreferencesKey(USER_KEY)] = json }
    }

    suspend fun getToken(): String? = token.first()

    suspend fun getUserJson(): String? = userJson.first()

    suspend fun clear() {
        dataStore.edit {
            it.remove(stringPreferencesKey(TOKEN_KEY))
            it.remove(stringPreferencesKey(USER_KEY))
        }
    }
}
