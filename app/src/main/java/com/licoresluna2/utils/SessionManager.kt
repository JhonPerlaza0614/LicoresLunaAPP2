package com.licoresluna2.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Gestor local de sesión para almacenar las credenciales y el estado del usuario.
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
    }

    /**
     * Guarda los datos de sesión tras un inicio de sesión exitoso.
     */
    fun saveUserSession(userId: String, email: String, isAdmin: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putBoolean(KEY_IS_ADMIN, isAdmin)
            apply()
        }
    }

    /**
     * Actualiza el rol de administrador si se verifica posteriormente contra Supabase.
     */
    fun updateAdminStatus(isAdmin: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ADMIN, isAdmin).apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun isAdmin(): Boolean = prefs.getBoolean(KEY_IS_ADMIN, false)

    /**
     * Elimina todos los datos de la sesión actual al cerrar sesión.
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
