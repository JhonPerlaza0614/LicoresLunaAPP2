package com.licoresluna2.views

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.R
import com.licoresluna2.databinding.ActivityHomeBinding
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val app by lazy { application as LicoresLunaApp }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        verifySessionAndRole()
    }

    private fun setupUI() {
        val email = app.sessionManager.getUserEmail() ?: "Usuario"
        val userId = app.sessionManager.getUserId() ?: ""
        val isAdmin = app.sessionManager.isAdmin()

        binding.tvUserEmail.text = email
        binding.tvUserId.text = if (userId.isNotEmpty()) "ID: $userId" else ""
        updateRoleBadge(isAdmin)
    }

    private fun updateRoleBadge(isAdmin: Boolean) {
        if (isAdmin) {
            binding.tvRoleBadge.text = "👑 Administrador"
            binding.tvRoleBadge.setBackgroundResource(R.drawable.badge_admin_bg)
            binding.tvRoleBadge.setTextColor(ContextCompat.getColor(this, R.color.admin_badge_text))
        } else {
            binding.tvRoleBadge.text = "👤 Usuario"
            binding.tvRoleBadge.setBackgroundResource(R.drawable.badge_user_bg)
            binding.tvRoleBadge.setTextColor(ContextCompat.getColor(this, R.color.user_badge_text))
        }
    }

    private fun verifySessionAndRole() {
        lifecycleScope.launch {
            try {
                val user = app.supabaseClient.auth.currentUserOrNull()
                if (user != null) {
                    val email = user.email ?: app.sessionManager.getUserEmail() ?: "Usuario"
                    binding.tvUserEmail.text = email
                    binding.tvUserId.text = "ID: ${user.id}"

                    val isAdmin = app.authManager.checkIsAdmin(user.id)
                    app.sessionManager.updateAdminStatus(isAdmin)
                    updateRoleBadge(isAdmin)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                app.authManager.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                app.sessionManager.clearSession()
                val intent = Intent(this@HomeActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
    }
}
