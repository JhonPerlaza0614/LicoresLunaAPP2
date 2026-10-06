package com.licoresluna2.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.MemoryCodeVerifierCache
import io.github.jan.supabase.gotrue.MemorySessionManager
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://jpasnudxbdnqizwmovvx.supabase.co",
        supabaseKey = "sb_publishable_2aZfi4v3i24OESO6akrrCQ_oDL1oKfm"
    ) {
        install(Auth) {
            // La sesión se controla con SessionManager; no usar SettingsSessionManager
            // porque no puede crear su almacenamiento predeterminado en este dispositivo.
            sessionManager = MemorySessionManager()
            codeVerifierCache = MemoryCodeVerifierCache()
            autoLoadFromStorage = false
            autoSaveToStorage = false
        }
        install(Postgrest)
    }
}