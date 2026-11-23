package com.licoresluna2.views.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.licoresluna2.R
import com.licoresluna2.data.models.SaleLine
import com.licoresluna2.data.models.SaleLineMode
import com.licoresluna2.data.models.SaleProduct
import com.licoresluna2.databinding.ItemSaleLineBinding
import java.text.NumberFormat
import java.util.Locale

class SaleLineAdapter(
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<SaleLineAdapter.ViewHolder>() {

    private val lines = mutableListOf<SaleLine>()
    private var products: List<SaleProduct> = emptyList()
    private val money = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun submit(lines: List<SaleLine>, products: List<SaleProduct>) {
        this.lines.clear()
        this.lines.addAll(lines)
        this.products = products
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSaleLineBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = lines.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lines[position], position)
    }

    inner class ViewHolder(
        private val binding: ItemSaleLineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(line: SaleLine, position: Int) {
            val product = products.firstOrNull { it.id == line.productId } ?: return
            val deposit = if (
                line.mode == SaleLineMode.CHARGE && product.depositEnabled
            ) (product.depositAmount ?: 0.0).toLong() * line.qty else 0L
            val total = product.priceGross.toLong() * line.qty + deposit

            binding.tvName.text = product.name
            binding.tvBase.text = binding.root.context.getString(
                R.string.sale_line_base,
                money.format(product.priceGross),
                if (product.depositEnabled) money.format(product.depositAmount ?: 0.0) else "0"
            )
            binding.etQty.setText(line.qty.toString())
            binding.tvLineTotal.text = "$${money.format(total)}"

            binding.containerDeposit.visibility =
                if (product.depositEnabled) android.view.View.VISIBLE else android.view.View.GONE
            binding.swChargeDeposit.isChecked = line.mode == SaleLineMode.CHARGE
            binding.swChargeDeposit.setOnCheckedChangeListener { _, checked ->
                line.mode =
                    if (checked) SaleLineMode.CHARGE else SaleLineMode.EXCHANGE
                binding.tvDepositMode.text = if (checked) {
                    binding.root.context.getString(R.string.sale_charge_deposit)
                } else {
                    binding.root.context.getString(R.string.sale_exchange_deposit)
                }
                onChanged()
            }
            binding.tvDepositMode.text = if (binding.swChargeDeposit.isChecked) {
                binding.root.context.getString(R.string.sale_charge_deposit)
            } else {
                binding.root.context.getString(R.string.sale_exchange_deposit)
            }

            binding.etQty.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val qty = binding.etQty.text?.toString()?.toIntOrNull() ?: 1
                    line.qty = qty.coerceAtLeast(1)
                    binding.etQty.setText(line.qty.toString())
                    onChanged()
                }
            }
            binding.btnRemove.setOnClickListener {
                lines.removeAt(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, lines.size)
                onChanged()
            }
        }
    }
}
