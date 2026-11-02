package com.licoresluna2.views

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.R
import com.licoresluna2.databinding.ActivityQuickSaleBinding
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.NumberFormat
import java.util.Locale

class QuickSaleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickSaleBinding
    private val app by lazy { application as LicoresLunaApp }
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickSaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etAmount.requestFocus()
        binding.etAmount.doAfterTextChanged { binding.tvMessage.visibility = View.GONE }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSubmit.setOnClickListener { submitQuickSale() }
    }

    private fun submitQuickSale() {
        val amountText = binding.etAmount.text?.toString()?.trim().orEmpty()
        val amount = amountText.toDoubleOrNull()

        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            showMessage(getString(R.string.quick_sale_invalid_amount), false)
            binding.etAmount.requestFocus()
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                app.supabaseClient.postgrest.rpc(
                    function = "insert_quick_sale",
                    parameters = buildJsonObject {
                        put("p_total", amount)
                        put("p_payment_method", "cash")
                    }
                )
                binding.etAmount.text?.clear()
                hideKeyboard()
                showMessage(
                    getString(
                        R.string.quick_sale_success,
                        currencyFormat.format(amount)
                    ),
                    true
                )
            } catch (exception: Exception) {
                showMessage(
                    exception.localizedMessage
                        ?: getString(R.string.quick_sale_error),
                    false
                )
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSubmit.isEnabled = !loading
        binding.etAmount.isEnabled = !loading
    }

    private fun showMessage(message: String, success: Boolean) {
        binding.tvMessage.text = message
        binding.tvMessage.setTextColor(
            getColor(if (success) R.color.stock_available else R.color.stock_empty)
        )
        binding.tvMessage.setBackgroundResource(
            if (success) R.drawable.quick_sale_success_bg else R.drawable.quick_sale_error_bg
        )
        binding.tvMessage.visibility = View.VISIBLE
    }

    private fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.etAmount.windowToken, 0)
    }
}
