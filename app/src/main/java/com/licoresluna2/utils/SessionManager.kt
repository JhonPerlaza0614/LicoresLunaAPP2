package com.licoresluna2.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit

/**
 * Gestor local de sesión para almacenar las credenciales, el estado del usuario y sus preferencias.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "licores_luna_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_IS_ADMIN = "is_admin"

        const val KEY_THEME_MODE = "theme_mode"
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
    }

    /**
     * Guarda los datos de sesión tras un inicio de sesión exitoso.
     */
    fun saveUserSession(userId: String, email: String, isAdmin: Boolean) {
        prefs.edit {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putBoolean(KEY_IS_ADMIN, isAdmin)
        }
    }

    /**
     * Actualiza el rol de administrador si se verifica posteriormente contra Supabase.
     */
    fun updateAdminStatus(isAdmin: Boolean) {
        prefs.edit {
            putBoolean(KEY_IS_ADMIN, isAdmin)
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun isAdmin(): Boolean = prefs.getBoolean(KEY_IS_ADMIN, false)

    /**
     * Guarda la preferencia del tema de la aplicación.
     */
    fun saveThemeMode(themeMode: String) {
        prefs.edit {
            putString(KEY_THEME_MODE, themeMode)
        }
    }

    /**
     * Retorna el modo de tema guardado o el valor por defecto ('SYSTEM').
     */
    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM

    /**
     * Aplica el tema guardado en AppCompatDelegate.
     */
    fun applySavedTheme() {
        val mode = when (getThemeMode()) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Elimina los datos de sesión del usuario pero mantiene la preferencia del tema.
     */
    fun clearSession() {
        val currentTheme = getThemeMode()
        prefs.edit {
            clear()
            putString(KEY_THEME_MODE, currentTheme)
        }
    }
}
