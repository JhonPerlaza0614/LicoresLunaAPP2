package com.licoresluna2.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicInteger

/**
 * Modelo correspondiente a la tabla public.admin_users:
 * user_id uuid not null primary key references auth.users(id)
 * created_at timestamp with time zone not null default now()
 */
@Serializable
internal data class AdminUserRow(
    val user_id: String,
    val created_at: String? = null
)

/**
 * Gestor global de autenticación y verificación de roles para Supabase.
 */
class AuthManager(private val supabase: SupabaseClient) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _authReady = MutableStateFlow(false)
    val authReady: StateFlow<Boolean> = _authReady.asStateFlow()

    private val _adminReady = MutableStateFlow(true)
    val adminReady: StateFlow<Boolean> = _adminReady.asStateFlow()

    private val adminRequestRef = AtomicInteger(0)

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        scope.launch {
            // Escucha cambios de sesión en tiempo real
            supabase.auth.sessionStatus.collect { _ ->
                val user = supabase.auth.currentUserOrNull()
                _currentUser.value = user
                _authReady.value = true

                if (user != null) {
                    checkAdminStatus(user.id)
                } else {
                    _isAdmin.value = false
                    _adminReady.value = true
                }
            }
        }
    }

    /**
     * Consulta la función RPC 'is_admin' y la tabla 'admin_users' para verificar el rol.
     * Compatible con el esquema:
     * - Función PostgreSQL: is_admin() que evalúa auth.uid()
     * - Tabla: public.admin_users con política RLS "Permitir lectura a autenticados"
     */
    suspend fun checkIsAdmin(userId: String): Boolean {
        // 1. Invocar la función RPC 'is_admin' de PostgreSQL en Supabase
        try {
            val rpcResult = supabase.postgrest.rpc("is_admin").decodeAs<Boolean>()
            return rpcResult
        } catch (e: Exception) {
            println("ℹ️ RPC is_admin no respondió o no está registrada, consultando tabla admin_users: ${e.message}")
        }

        // 2. Consulta directa a la tabla public.admin_users (RLS: Permitir lectura a autenticados)
        return try {
            val result = supabase.postgrest
                .from("admin_users")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<AdminUserRow>()

            result != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Refresca manualmente la sesión y el estado de administrador.
     */
    suspend fun refreshAdmin(): Boolean {
        val user = try {
            supabase.auth.retrieveUserForCurrentSession()
            supabase.auth.currentUserOrNull()
        } catch (e: Exception) {
            null
        }

        if (user == null) {
            _currentUser.value = null
            _isAdmin.value = false
            _adminReady.value = true
            return false
        }

        _currentUser.value = user
        return checkAdminStatus(user.id)
    }

    private suspend fun checkAdminStatus(userId: String): Boolean {
        val requestId = adminRequestRef.incrementAndGet()
        _adminReady.value = false

        val isUserAdmin = checkIsAdmin(userId)

        if (adminRequestRef.get() == requestId) {
            _isAdmin.value = isUserAdmin
            _adminReady.value = true
        }

        return isUserAdmin
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _currentUser.value = null
            _isAdmin.value = false
            _adminReady.value = true
        }
    }
}
