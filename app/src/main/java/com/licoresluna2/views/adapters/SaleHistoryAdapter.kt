package com.licoresluna2.views.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.licoresluna2.R
import com.licoresluna2.data.models.SaleHistoryRow
import com.licoresluna2.databinding.ItemSaleHistoryBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SaleHistoryAdapter(
    private val onEdit: (SaleHistoryRow) -> Unit,
    private val onDelete: (SaleHistoryRow) -> Unit,
    private val editingSaleId: () -> String?
) : RecyclerView.Adapter<SaleHistoryAdapter.ViewHolder>() {

    private val items = mutableListOf<SaleHistoryRow>()
    private val money = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun submitList(values: List<SaleHistoryRow>) {
        items.clear()
        items.addAll(values)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSaleHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position])

    inner class ViewHolder(
        private val binding: ItemSaleHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sale: SaleHistoryRow) {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val time = try {
                val date = parser.parse(sale.createdAt.substringBefore(".")) ?: return
                SimpleDateFormat("hh:mm a", Locale("es", "CO"))
                    .format(date)
            } catch (_: Exception) {
                sale.createdAt
            }

            binding.tvTime.text = time
            binding.tvMethod.text = if (sale.paymentMethod == "cash") {
                binding.root.context.getString(R.string.sale_cash)
            } else {
                binding.root.context.getString(R.string.sale_transfer)
            }
            binding.tvItems.text = binding.root.context.resources.getQuantityString(
                R.plurals.sale_items_count,
                sale.saleItems.size,
                sale.saleItems.size
            )
            binding.tvTotal.text = "$${money.format(sale.total)}"
            binding.btnEdit.isEnabled = editingSaleId() != sale.id
            binding.btnEdit.setOnClickListener { onEdit(sale) }
            binding.btnDelete.setOnClickListener { onDelete(sale) }
        }
    }
}
