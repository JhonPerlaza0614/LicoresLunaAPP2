package com.licoresluna2.views

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.databinding.ActivitySettingsBinding
import com.licoresluna2.utils.SessionManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val app by lazy { application as LicoresLunaApp }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupCurrentThemeSelection()
        setupListeners()
    }

    private fun setupCurrentThemeSelection() {
        when (app.sessionManager.getThemeMode()) {
            SessionManager.THEME_LIGHT -> binding.rbThemeLight.isChecked = true
            SessionManager.THEME_DARK -> binding.rbThemeDark.isChecked = true
            else -> binding.rbThemeSystem.isChecked = true
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.rgThemeOptions.setOnCheckedChangeListener { _, checkedId ->
            val newTheme = when (checkedId) {
                binding.rbThemeLight.id -> SessionManager.THEME_LIGHT
                binding.rbThemeDark.id -> SessionManager.THEME_DARK
                else -> SessionManager.THEME_SYSTEM
            }

            if (app.sessionManager.getThemeMode() != newTheme) {
                app.sessionManager.saveThemeMode(newTheme)
                app.sessionManager.applySavedTheme()
            }
        }
    }
}
