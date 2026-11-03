package com.licoresluna2.views

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.licoresluna2.data.ExpenseRow
import com.licoresluna2.databinding.ItemExpenseBinding
import java.text.NumberFormat
import java.util.Locale

class ExpenseAdapter : ListAdapter<ExpenseRow, ExpenseAdapter.VH>(Diff) {

    private val money = NumberFormat.getInstance(Locale("es", "CO"))

    inner class VH(val b: ItemExpenseBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val e = getItem(position)
        holder.b.tvNote.text = e.note
        holder.b.tvType.text = if (e.productId != null)
            "📦 Stock: +${e.quantityBought} ${e.product?.name.orEmpty()}"
        else "⚙️ General"
        holder.b.tvAmount.text = "-$${money.format(e.amount)}"
        holder.b.tvDate.text = e.createdAt.take(16).replace('T', ' ')
    }

    object Diff : DiffUtil.ItemCallback<ExpenseRow>() {
        override fun areItemsTheSame(a: ExpenseRow, b: ExpenseRow) = a.id == b.id
        override fun areContentsTheSame(a: ExpenseRow, b: ExpenseRow) = a == b
    }
}