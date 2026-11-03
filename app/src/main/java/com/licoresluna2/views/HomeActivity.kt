package com.licoresluna2.views

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val app by lazy { application as LicoresLunaApp }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener { performLogout() }

        binding.cardSale.setOnClickListener {
            startActivity(Intent(this, SaleActivity::class.java))
        }

        binding.cardProducts.setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }

        binding.cardQuickSale.setOnClickListener {
            startActivity(Intent(this, QuickSaleActivity::class.java))
        }

        binding.cardExpenses.setOnClickListener {
            startActivity(Intent(this, ExpensesActivity::class.java))
        }

        binding.cardContainers.setOnClickListener {
            startActivity(Intent(this, ContainersActivity::class.java))
        }

        binding.cardSummary.setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                app.authManager.signOut()
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
