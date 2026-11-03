package com.licoresluna2.views

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.licoresluna2.R
import com.licoresluna2.data.ExpenseRow
import com.licoresluna2.data.formatIsoDateTime
import com.licoresluna2.databinding.ItemExpenseBinding
import java.text.NumberFormat
import java.util.Locale

class ExpenseAdapter : ListAdapter<ExpenseRow, ExpenseAdapter.ViewHolder>(Diff) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    inner class ViewHolder(val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.itemView.context

        with(holder.binding) {
            tvDate.text = formatIsoDateTime(item.createdAt)
            tvNote.text = item.note
            tvAmount.text = context.getString(
                R.string.expenses_negative_amount,
                currencyFormat.format(item.amount)
            )

            if (item.productId != null) {
                tvType.text = context.getString(
                    R.string.expenses_type_stock,
                    item.quantityBought ?: 0,
                    item.product?.name.orEmpty()
                )
                tvType.setBackgroundResource(R.drawable.tag_bg)
                tvType.setTextColor(ContextCompat.getColor(context, R.color.amber_light))
            } else {
                tvType.text = context.getString(R.string.expenses_type_general)
                tvType.setBackgroundResource(R.drawable.bg_badge_general)
                tvType.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ExpenseRow>() {
        override fun areItemsTheSame(oldItem: ExpenseRow, newItem: ExpenseRow): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExpenseRow, newItem: ExpenseRow): Boolean {
            return oldItem == newItem
        }
    }
}
