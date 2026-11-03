package com.licoresluna2.views

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.R
import com.licoresluna2.data.ExpenseProductOption
import com.licoresluna2.data.ExpenseRepository
import com.licoresluna2.data.ExpenseRow
import com.licoresluna2.databinding.ActivityExpensesBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpensesBinding
    private val app by lazy { application as LicoresLunaApp }
    private val repository by lazy { ExpenseRepository(app.supabaseClient) }
    private val adapter = ExpenseAdapter()

    private var expenses = listOf<ExpenseRow>()
    private var products = listOf<ExpenseProductOption>()
    private var submitting = false
    private val money = NumberFormat.getInstance(Locale("es", "CO"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter

        binding.spProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                binding.etQuantity.isEnabled = pos > 0
                if (pos == 0) binding.etQuantity.text?.clear()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        binding.etSearch.doAfterTextChanged { applyFilter() }
        binding.btnRegister.setOnClickListener { submit() }

        loadData()
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                expenses = repository.loadExpenses()
                products = repository.loadProducts()

                val labels = listOf("-- Gasto general operativo --") + products.map { it.name }
                binding.spProduct.adapter = ArrayAdapter(
                    this@ExpensesActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    labels
                )
                applyFilter()
            } catch (e: Exception) {
                showMessage(e.localizedMessage ?: "Error al sincronizar egresos.", false)
            }
        }
    }

    private fun applyFilter() {
        val term = binding.etSearch.text?.toString()?.trim()?.lowercase().orEmpty()
        val filtered = if (term.isEmpty()) expenses else expenses.filter {
            it.note.lowercase().contains(term) ||
                    it.product?.name?.lowercase()?.contains(term) == true
        }
        adapter.submitList(filtered)
        binding.tvTotal.text = "Total egresos: $${money.format(filtered.sumOf { it.amount })}"
    }

    private fun submit() {
        if (submitting) return

        val amount = binding.etAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
        val note = binding.etNote.text?.toString()?.trim().orEmpty()
        val pos = binding.spProduct.selectedItemPosition
        val product = if (pos > 0) products.getOrNull(pos - 1) else null
        val qty = binding.etQuantity.text?.toString()?.toIntOrNull()

        if (amount <= 0) return showMessage("El monto debe ser mayor a cero.", false)
        if (note.isEmpty()) return showMessage("Escribe el concepto del gasto.", false)
        if (product != null && (qty == null || qty <= 0))
            return showMessage("Si eliges un producto, indica una cantidad válida.", false)

        submitting = true
        binding.btnRegister.isEnabled = false
        lifecycleScope.launch {
            try {
                repository.registerExpense(
                    amount, note, product?.id, if (product != null) qty else null
                )
                showMessage(
                    if (product != null) "Gasto registrado e inventario incrementado."
                    else "Gasto operativo registrado.",
                    true
                )
                binding.etAmount.text?.clear()
                binding.etNote.text?.clear()
                binding.etQuantity.text?.clear()
                binding.spProduct.setSelection(0)
                loadData()
            } catch (e: Exception) {
                showMessage(e.localizedMessage ?: "Error al procesar la transacción.", false)
            } finally {
                submitting = false
                binding.btnRegister.isEnabled = true
            }
        }
    }

    private fun showMessage(message: String, success: Boolean) {
        binding.tvMessage.text = message
        binding.tvMessage.setTextColor(
            getColor(if (success) R.color.stock_available else R.color.stock_empty)
        )
        binding.tvMessage.visibility = View.VISIBLE
    }
}