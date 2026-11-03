package com.licoresluna2.views

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.R
import com.licoresluna2.data.ExpenseInsert
import com.licoresluna2.data.ExpenseRow
import com.licoresluna2.data.ExpensesRepository
import com.licoresluna2.data.ProductOption
import com.licoresluna2.databinding.ActivityExpensesBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpensesBinding
    private val app by lazy { application as LicoresLunaApp }
    private val repository by lazy { ExpensesRepository(app.supabaseClient) }
    private val expenseAdapter = ExpenseAdapter()
    private val numberFormat = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    private var expenses: List<ExpenseRow> = emptyList()
    private var products: List<ProductOption> = emptyList()
    private var selectedProductId: String = ""
    private var submitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupProductDropdown()
        setupListeners()
        loadData()
    }

    private fun setupRecyclerView() {
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = expenseAdapter
    }

    private fun setupProductDropdown() {
        binding.actProduct.setText(getString(R.string.expenses_general_option), false)
        binding.actProduct.setOnItemClickListener { _, _, position, _ ->
            hideMessages()
            if (position <= 0) {
                selectedProductId = ""
                binding.etQuantity.setText("")
                setQuantityEnabled(false)
            } else {
                val product = products.getOrNull(position - 1)
                selectedProductId = product?.id.orEmpty()
                setQuantityEnabled(product != null)
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSubmit.setOnClickListener { submitExpense() }

        binding.etSearch.doAfterTextChanged {
            applyFilter()
        }

        binding.etAmount.doAfterTextChanged { hideMessages() }
        binding.etNote.doAfterTextChanged { hideMessages() }
        binding.etQuantity.doAfterTextChanged { hideMessages() }
    }

    private fun loadData() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val loadedExpenses = repository.loadExpenses()
                val loadedProducts = repository.loadActiveProducts()

                expenses = loadedExpenses
                products = loadedProducts

                renderProductOptions()
                applyFilter()
            } catch (exception: Exception) {
                showError(
                    exception.localizedMessage
                        ?: getString(R.string.expenses_load_error)
                )
                applyFilter()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun renderProductOptions() {
        if (selectedProductId.isNotEmpty() && products.none { it.id == selectedProductId }) {
            selectedProductId = ""
        }

        val labels = mutableListOf(getString(R.string.expenses_general_option))
        labels.addAll(products.map { it.name })

        binding.actProduct.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)
        )

        val selectedIndex = if (selectedProductId.isEmpty()) {
            0
        } else {
            products.indexOfFirst { it.id == selectedProductId } + 1
        }

        binding.actProduct.setText(labels.getOrElse(selectedIndex) { labels[0] }, false)
        setQuantityEnabled(selectedProductId.isNotEmpty())
    }

    private fun applyFilter() {
        val term = binding.etSearch.text?.toString()?.trim()?.lowercase(Locale.getDefault())
            .orEmpty()

        val filtered = if (term.isEmpty()) {
            expenses
        } else {
            expenses.filter { expense ->
                expense.note.lowercase(Locale.getDefault()).contains(term) ||
                        expense.product?.name
                            ?.lowercase(Locale.getDefault())
                            ?.contains(term) == true
            }
        }

        expenseAdapter.submitList(filtered)
        binding.tvTotal.text = getString(
            R.string.expenses_money_format,
            numberFormat.format(filtered.sumOf { it.amount })
        )
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun submitExpense() {
        if (submitting) return

        val amount = binding.etAmount.text?.toString()?.trim()?.toDoubleOrNull()
        val note = binding.etNote.text?.toString()?.trim().orEmpty()
        val isStockPurchase = selectedProductId.isNotEmpty()
        val quantity = binding.etQuantity.text?.toString()?.trim()?.toDoubleOrNull()?.toInt()

        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            showError(getString(R.string.expenses_invalid_amount))
            binding.etAmount.requestFocus()
            return
        }

        if (note.isEmpty()) {
            showError(getString(R.string.expenses_note_required))
            binding.etNote.requestFocus()
            return
        }

        if (isStockPurchase && (quantity == null || quantity <= 0)) {
            showError(getString(R.string.expenses_invalid_quantity))
            binding.etQuantity.requestFocus()
            return
        }

        hideMessages()
        setSubmitting(true)

        lifecycleScope.launch {
            try {
                repository.insertExpense(
                    ExpenseInsert(
                        amount = amount,
                        note = note,
                        productId = if (isStockPurchase) selectedProductId else null,
                        quantityBought = if (isStockPurchase) quantity else null
                    )
                )

                if (isStockPurchase) {
                    val currentStock = repository.getStock(selectedProductId)
                    repository.upsertStock(
                        productId = selectedProductId,
                        qtyOnHand = currentStock + (quantity ?: 0)
                    )
                }

                showSuccess(
                    getString(
                        if (isStockPurchase) {
                            R.string.expenses_success_stock
                        } else {
                            R.string.expenses_success_general
                        }
                    )
                )

                resetForm()
                loadData()
            } catch (exception: Exception) {
                showError(
                    exception.localizedMessage
                        ?: getString(R.string.expenses_submit_error)
                )
            } finally {
                setSubmitting(false)
            }
        }
    }

    private fun resetForm() {
        binding.etAmount.setText("")
        binding.etNote.setText("")
        binding.etQuantity.setText("")
        selectedProductId = ""
        binding.actProduct.setText(getString(R.string.expenses_general_option), false)
        setQuantityEnabled(false)
    }

    private fun setQuantityEnabled(enabled: Boolean) {
        val isEnabled = enabled && !submitting
        binding.tilQuantity.isEnabled = isEnabled
        binding.etQuantity.isEnabled = isEnabled
    }

    private fun setSubmitting(isSubmitting: Boolean) {
        submitting = isSubmitting
        binding.btnSubmit.isEnabled = !isSubmitting
        binding.btnSubmit.text = getString(
            if (isSubmitting) R.string.expenses_submitting else R.string.expenses_submit
        )
        binding.progressBar.visibility = if (isSubmitting) View.VISIBLE else View.GONE
        binding.etAmount.isEnabled = !isSubmitting
        binding.actProduct.isEnabled = !isSubmitting
        binding.etNote.isEnabled = !isSubmitting
        binding.etSearch.isEnabled = !isSubmitting
        setQuantityEnabled(selectedProductId.isNotEmpty())
    }

    private fun showLoading(isLoading: Boolean) {
        binding.tvLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.tvEmpty.visibility = View.GONE
        }
    }

    private fun showSuccess(message: String) {
        binding.tvMessage.text = message
        binding.tvMessage.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        binding.tvMessage.visibility = View.GONE
    }

    private fun hideMessages() {
        binding.tvMessage.visibility = View.GONE
        binding.tvError.visibility = View.GONE
    }
}