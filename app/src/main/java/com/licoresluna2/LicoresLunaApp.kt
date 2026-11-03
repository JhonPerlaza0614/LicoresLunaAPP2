package com.licoresluna2

import android.app.Application
import com.licoresluna2.data.SupabaseClient
import com.licoresluna2.network.AuthManager
import com.licoresluna2.utils.SessionManager
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient

class LicoresLunaApp : Application() {

    val supabaseClient: JanSupabaseClient by lazy {
        SupabaseClient.client
    }

    val sessionManager: SessionManager by lazy {
        SessionManager(this)
    }

    val authManager: AuthManager by lazy {
        AuthManager(supabaseClient)
    }
}