package com.licoresluna2.views

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.databinding.ActivityLoginBinding
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val app by lazy { application as LicoresLunaApp }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya hay una sesión activa, ir directo a HomeActivity
        if (app.sessionManager.isLoggedIn() || app.supabaseClient.auth.currentUserOrNull() != null) {
            navigateToHome()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (!validateInputs(email, password)) {
                return@setOnClickListener
            }

            performLogin(email, password)
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        hideMessage()

        if (email.isEmpty()) {
            showMessage("Por favor ingresa tu correo electrónico.")
            binding.etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("El formato del correo electrónico no es válido.")
            binding.etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            showMessage("Por favor ingresa tu contraseña.")
            binding.etPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            showMessage("La contraseña debe tener al menos 6 caracteres.")
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun performLogin(emailInput: String, passwordInput: String) {
        setLoading(true)
        hideMessage()

        lifecycleScope.launch {
            try {
                // Iniciar sesión con Supabase Auth
                app.supabaseClient.auth.signInWith(Email) {
                    email = emailInput
                    password = passwordInput
                }

                val user = app.supabaseClient.auth.currentUserOrNull()
                if (user == null) {
                    showMessage("No se pudo obtener la sesión del usuario. Intenta de nuevo.")
                    setLoading(false)
                    return@launch
                }

                // Verificar rol en backend (tabla admin_users / triggers de Supabase)
                val isAdmin = app.authManager.checkIsAdmin(user.id)

                // Guardar la sesión persistente
                app.sessionManager.saveUserSession(
                    userId = user.id,
                    email = user.email ?: emailInput,
                    isAdmin = isAdmin
                )

                navigateToHome()

            } catch (e: Exception) {
                e.printStackTrace()
                val message = e.message ?: ""
                val friendlyMessage = when {
                    message.contains("Invalid login credentials", ignoreCase = true) ||
                    message.contains("invalid_grant", ignoreCase = true) ->
                        "Correo electrónico o contraseña incorrectos."

                    message.contains("Email not confirmed", ignoreCase = true) ->
                        "Debes confirmar tu correo electrónico antes de ingresar."

                    message.contains("Unable to resolve host", ignoreCase = true) ||
                    message.contains("ConnectException", ignoreCase = true) ||
                    message.contains("timeout", ignoreCase = true) ->
                        "Error de conexión con el servidor. Revisa tu acceso a internet."

                    else ->
                        e.localizedMessage ?: "No se pudo iniciar sesión. Verifica tus datos."
                }

                showMessage(friendlyMessage)
                setLoading(false)
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun showMessage(msg: String) {
        binding.tvMessage.text = "⚠️ $msg"
        binding.tvMessage.visibility = View.VISIBLE
    }

    private fun hideMessage() {
        binding.tvMessage.visibility = View.GONE
    }
}
