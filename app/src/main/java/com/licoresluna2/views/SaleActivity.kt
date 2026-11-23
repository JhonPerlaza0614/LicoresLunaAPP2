package com.licoresluna2.views

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.R
import com.licoresluna2.data.SalesRepository
import com.licoresluna2.data.models.SaleHistoryRow
import com.licoresluna2.data.models.SaleLine
import com.licoresluna2.data.models.SaleLineMode
import com.licoresluna2.data.models.SaleProduct
import com.licoresluna2.databinding.ActivitySaleBinding
import com.licoresluna2.views.adapters.SaleHistoryAdapter
import com.licoresluna2.views.adapters.SaleLineAdapter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SaleActivity : BaseActivity() {

    private lateinit var binding: ActivitySaleBinding
    private val app by lazy { application as LicoresLunaApp }
    private val repository by lazy { SalesRepository(app.supabaseClient) }
    private val money = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    private var products: List<SaleProduct> = emptyList()
    private val lines = mutableListOf<SaleLine>()
    private var editingSaleId: String? = null
    private var paymentMethod = "cash"
    private var selectedDateKey = localDateKey()
    private var submitting = false

    private lateinit var lineAdapter: SaleLineAdapter
    private lateinit var historyAdapter: SaleHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySaleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupAdapters()
        setupPayment()
        setupListeners()
        loadData()
    }

    private fun setupAdapters() {
        lineAdapter = SaleLineAdapter { refreshTotals() }
        binding.rvLines.layoutManager = LinearLayoutManager(this)
        binding.rvLines.adapter = lineAdapter

        historyAdapter = SaleHistoryAdapter(
            onEdit = ::prepareEdit,
            onDelete = ::confirmDelete,
            editingSaleId = { editingSaleId }
        )
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = historyAdapter
    }

    private fun setupPayment() {
        val options = listOf(
            getString(R.string.sale_cash),
            getString(R.string.sale_transfer)
        )
        binding.actPayment.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, options)
        )
        binding.actPayment.setText(options[0], false)
        binding.actPayment.setOnItemClickListener { _, _, position, _ ->
            paymentMethod = if (position == 0) "cash" else "transfer"
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnSubmit.setOnClickListener { submitSale() }
        binding.btnCancelEdit.setOnClickListener { cancelEdit() }
        binding.btnSelectDate.setOnClickListener { showDatePicker() }

        binding.actProduct.setOnItemClickListener { _, _, position, _ ->
            val productName = binding.actProduct.adapter.getItem(position)?.toString().orEmpty()
            val product = products.firstOrNull { it.name == productName } ?: return@setOnItemClickListener
            addProduct(product)
            binding.actProduct.setText("", false)
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                products = repository.loadProducts()
                binding.actProduct.setAdapter(
                    ArrayAdapter(
                        this@SaleActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        products.map { it.name }
                    )
                )
                refreshLines()
            } catch (exception: Exception) {
                showError(exception.localizedMessage ?: getString(R.string.sale_load_error))
            }
        }
        loadHistory()
    }

    private fun addProduct(product: SaleProduct) {
        val existingIndex = lines.indexOfFirst { it.productId == product.id }
        if (existingIndex >= 0) {
            lines[existingIndex].qty++
        } else {
            lines.add(
                SaleLine(
                    productId = product.id,
                    qty = 1,
                    mode =
                        if (product.depositEnabled) SaleLineMode.EXCHANGE
                        else SaleLineMode.CHARGE
                )
            )
        }
        hideMessages()
        refreshLines()
    }

    private fun refreshLines() {
        lineAdapter.submit(lines, products)
        binding.tvEmptyLines.visibility = if (lines.isEmpty()) View.VISIBLE else View.GONE
        refreshTotals()
    }

    private fun refreshTotals() {
        var subtotal = 0.0
        var depositTotal = 0.0

        lines.forEach { line ->
            val product = products.firstOrNull { it.id == line.productId } ?: return@forEach
            subtotal += product.priceGross * line.qty
            if (line.mode == SaleLineMode.CHARGE && product.depositEnabled) {
                depositTotal += (product.depositAmount ?: 0.0) * line.qty
            }
        }

        val grand = subtotal + depositTotal
        binding.tvSubtotal.text = getString(R.string.sale_subtotal, money.format(subtotal))
        binding.tvDepositTotal.text = getString(R.string.sale_deposit_total, money.format(depositTotal))
        binding.tvGrandTotal.text = getString(R.string.sale_grand_total, money.format(grand))
        binding.btnSubmit.isEnabled = !submitting && lines.isNotEmpty()
        binding.btnSubmit.text = if (submitting) {
            getString(R.string.sale_submitting)
        } else if (editingSaleId != null) {
            getString(R.string.sale_update)
        } else {
            getString(R.string.sale_submit)
        }
    }

    private fun submitSale() {
        if (submitting) return
        if (lines.isEmpty()) {
            showError(getString(R.string.sale_add_product))
            return
        }

        submitting = true
        hideMessages()
        binding.progressBar.visibility = View.VISIBLE
        refreshTotals()

        lifecycleScope.launch {
            try {
                if (editingSaleId != null) {
                    repository.updateSale(editingSaleId!!, paymentMethod, lines)
                    showSuccess(getString(R.string.sale_updated))
                } else {
                    val saleId = repository.createSale(paymentMethod, lines)
                    lines
                        .filter { it.mode == SaleLineMode.EXCHANGE }
                        .forEach { line ->
                            try {
                                repository.registerExchange(line.productId, line.qty, saleId)
                            } catch (exception: Exception) {
                                android.util.Log.e(
                                    "SaleActivity",
                                    "No se pudo registrar el envase recibido",
                                    exception
                                )
                            }
                        }
                    showSuccess(getString(R.string.sale_created))
                }
                editingSaleId = null
                lines.clear()
                refreshLines()
                updateEditingUi()
                loadData()
            } catch (exception: Exception) {
                showError(exception.localizedMessage ?: getString(R.string.sale_submit_error))
            } finally {
                submitting = false
                binding.progressBar.visibility = View.GONE
                refreshTotals()
            }
        }
    }

    private fun prepareEdit(sale: SaleHistoryRow) {
        if (lines.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sale_replace_title)
                .setMessage(R.string.sale_replace_message)
                .setPositiveButton(R.string.sale_replace) { _, _ ->
                    restoreSaleForEdit(sale)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            restoreSaleForEdit(sale)
        }
    }

    private fun restoreSaleForEdit(sale: SaleHistoryRow) {
        lines.clear()
        sale.saleItems.forEach { item ->
            lines.add(
                SaleLine(
                    productId = item.productId,
                    qty = item.qty,
                    mode = if (item.depositCharged) SaleLineMode.CHARGE else SaleLineMode.EXCHANGE
                )
            )
        }
        editingSaleId = sale.id
        paymentMethod = sale.paymentMethod
        binding.actPayment.setText(
            if (paymentMethod == "cash") getString(R.string.sale_cash)
            else getString(R.string.sale_transfer),
            false
        )
        hideMessages()
        refreshLines()
        updateEditingUi()
        binding.scrollView.smoothScrollTo(0, 0)
    }

    private fun cancelEdit() {
        editingSaleId = null
        lines.clear()
        hideMessages()
        refreshLines()
        updateEditingUi()
    }

    private fun updateEditingUi() {
        val editing = editingSaleId != null
        binding.tvEditing.visibility = if (editing) View.VISIBLE else View.GONE
        binding.btnCancelEdit.visibility = if (editing) View.VISIBLE else View.GONE
        refreshTotals()
    }

    private fun confirmDelete(sale: SaleHistoryRow) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sale_delete_title)
            .setMessage(R.string.sale_delete_message)
            .setPositiveButton(R.string.sale_delete) { _, _ ->
                lifecycleScope.launch {
                    try {
                        repository.deleteSale(sale.id)
                        if (editingSaleId == sale.id) cancelEdit()
                        showSuccess(getString(R.string.sale_deleted))
                        loadData()
                    } catch (exception: Exception) {
                        showError(exception.localizedMessage ?: getString(R.string.sale_delete_error))
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun loadHistory() {
        binding.progressHistory.visibility = View.VISIBLE
        binding.tvEmptyHistory.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val (startIso, endIso) = dayRange(selectedDateKey)
                val history = repository.loadHistory(startIso, endIso)
                historyAdapter.submitList(history)
                binding.tvEmptyHistory.visibility =
                    if (history.isEmpty()) View.VISIBLE else View.GONE
                binding.btnSelectDate.text = selectedDateKey
            } catch (exception: Exception) {
                showError(exception.localizedMessage ?: getString(R.string.sale_history_error))
            } finally {
                binding.progressHistory.visibility = View.GONE
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val parts = selectedDateKey.split("-")
        if (parts.size == 3) {
            calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDateKey = "%04d-%02d-%02d".format(year, month + 1, day)
                loadHistory()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun localDateKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())

    private fun dayRange(dateKey: String): Pair<String, String> {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        val date = parser.parse(dateKey) ?: java.util.Date()
        val startCalendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCalendar = (startCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return iso.format(startCalendar.time) to iso.format(endCalendar.time)
    }

    private fun showSuccess(message: String) {
        binding.tvError.visibility = View.GONE
        binding.tvMessage.text = message
        binding.tvMessage.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.tvMessage.visibility = View.GONE
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideMessages() {
        binding.tvMessage.visibility = View.GONE
        binding.tvError.visibility = View.GONE
    }
}
