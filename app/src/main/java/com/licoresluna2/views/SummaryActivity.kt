package com.licoresluna2.views

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.R
import com.licoresluna2.data.SummaryExpenseRow
import com.licoresluna2.data.SummaryRepository
import com.licoresluna2.data.SummarySaleRow
import com.licoresluna2.databinding.ActivitySummaryBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

class SummaryActivity : AppCompatActivity() {

    private enum class Period { DATE, WEEK, MONTH, ALL }

    private lateinit var binding: ActivitySummaryBinding
    private val app by lazy { application as LicoresLunaApp }
    private val repository by lazy { SummaryRepository(app.supabaseClient) }
    private val money = NumberFormat.getInstance(Locale("es", "CO"))

    private var sales = listOf<SummarySaleRow>()
    private var expenses = listOf<SummaryExpenseRow>()
    private var period = Period.DATE
    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnDate.setOnClickListener { pickDate() }
        binding.btnPeriodDate.setOnClickListener { selectedDate = LocalDate.now(); setPeriod(Period.DATE) }
        binding.btnPeriodWeek.setOnClickListener { setPeriod(Period.WEEK) }
        binding.btnPeriodMonth.setOnClickListener { setPeriod(Period.MONTH) }
        binding.btnPeriodAll.setOnClickListener { setPeriod(Period.ALL) }

        updateFilterUi()
        loadData()
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE
        lifecycleScope.launch {
            try {
                sales = repository.loadSales()
                expenses = repository.loadExpenses()
                render()
            } catch (e: Exception) {
                binding.tvError.text = e.localizedMessage ?: "Error cargando resumen operativo."
                binding.tvError.visibility = View.VISIBLE
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun pickDate() {
        DatePickerDialog(
            this,
            { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d)
                setPeriod(Period.DATE)
            },
            selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
        ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
    }

    private fun setPeriod(p: Period) {
        period = p
        updateFilterUi()
        render()
    }

    private fun updateFilterUi() {
        binding.btnDate.text = "Fecha: $selectedDate"
        binding.btnPeriodDate.alpha = if (period == Period.DATE) 1f else 0.5f
        binding.btnPeriodWeek.alpha = if (period == Period.WEEK) 1f else 0.5f
        binding.btnPeriodMonth.alpha = if (period == Period.MONTH) 1f else 0.5f
        binding.btnPeriodAll.alpha = if (period == Period.ALL) 1f else 0.5f
    }

    // Misma lógica de filtrado que filterSalesByPeriod de React
    private fun inPeriod(iso: String): Boolean {
        if (period == Period.ALL) return true
        val dt = OffsetDateTime.parse(iso)
        val localDate = dt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
        return when (period) {
            Period.DATE -> localDate == selectedDate
            Period.WEEK -> dt.toInstant() >= Instant.now().minus(7, ChronoUnit.DAYS)
            Period.MONTH -> !localDate.isBefore(LocalDate.now().withDayOfMonth(1))
            Period.ALL -> true
        }
    }

    private fun render() {
        val saleRows = sales.filter { inPeriod(it.createdAt) }
        val expenseRows = expenses.filter { inPeriod(it.createdAt) }

        val totalSales = saleRows.sumOf { it.effectiveTotal }
        val deposits = saleRows.sumOf { it.deposit }
        val cash = saleRows.filter { it.paymentMethod == "cash" }.sumOf { it.effectiveTotal }
        val transfer = saleRows.filter { it.paymentMethod == "transfer" }.sumOf { it.effectiveTotal }
        val expensesTotal = expenseRows.sumOf { it.amount ?: 0.0 }
        val netReal = totalSales - expensesTotal - deposits

        binding.tvTotalSales.text = fmt(totalSales)
        binding.tvSalesCount.text = "${saleRows.size} órdenes liquidadas"
        binding.tvNetReal.text = fmt(netReal)

        binding.tvReconGross.text = fmt(totalSales)
        binding.tvReconExpenses.text = "− ${fmt(expensesTotal)}"
        binding.tvReconDeposits.text = "− ${fmt(deposits)}"
        binding.tvReconNet.text = fmt(netReal)
        val netColor = getColor(if (netReal >= 0) R.color.stock_available else R.color.stock_empty)
        binding.tvReconNet.setTextColor(netColor)
        binding.tvNetReal.setTextColor(netColor)

        binding.tvCash.text = fmt(cash)
        binding.tvTransfer.text = fmt(transfer)
        binding.tvDeposits.text = fmt(deposits)
        binding.tvExpenses.text = fmt(expensesTotal)
    }

    private fun fmt(value: Double) = "$${money.format(value)}"
}