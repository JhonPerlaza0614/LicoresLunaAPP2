package com.licoresluna2.views.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.licoresluna2.R
import com.licoresluna2.data.models.ProductAdminRow
import com.licoresluna2.databinding.ItemProductCardBinding
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var products: List<ProductAdminRow> = emptyList(),
    private val onEditClick: (ProductAdminRow) -> Unit,
    private val onArchiveClick: (ProductAdminRow) -> Unit,
    private val onRestoreClick: (ProductAdminRow) -> Unit = {},
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun submitList(newList: List<ProductAdminRow>) {
        products = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(
        private val binding: ItemProductCardBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductAdminRow) {
            val context = binding.root.context
            val currentStock = product.stockQty
            val hasStock = currentStock > 0

            // Image loading
            if (!product.imageUrl.isNullOrBlank()) {
                binding.ivProductImage.visibility = View.VISIBLE
                binding.tvNoImage.visibility = View.GONE
                binding.ivProductImage.load(product.imageUrl) {
                    crossfade(enable = true)
                    placeholder(R.color.input_background)
                    error(R.color.input_background)
                }
            } else {
                binding.ivProductImage.visibility = View.GONE
                binding.tvNoImage.visibility = View.VISIBLE
            }

            // Stock / Status badge
            if (!product.isActive) {
                binding.tvStockBadge.text = "Archivado"
                binding.tvStockBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                binding.tvStockBadge.setBackgroundResource(R.drawable.badge_admin_bg)
                binding.btnArchive.text = context.getString(R.string.product_unarchive_button)
                binding.btnArchive.setOnClickListener { onRestoreClick(product) }
            } else if (hasStock) {
                binding.tvStockBadge.text = context.getString(R.string.product_units_format, currentStock)
                binding.tvStockBadge.setTextColor(ContextCompat.getColor(context, R.color.stock_available))
                binding.tvStockBadge.setBackgroundResource(R.drawable.quick_sale_success_bg)
                binding.btnArchive.text = context.getString(R.string.product_archive_button)
                binding.btnArchive.setOnClickListener { onArchiveClick(product) }
            } else {
                binding.tvStockBadge.text = context.getString(R.string.product_out_of_stock)
                binding.tvStockBadge.setTextColor(ContextCompat.getColor(context, R.color.stock_empty))
                binding.tvStockBadge.setBackgroundResource(R.drawable.quick_sale_error_bg)
                binding.btnArchive.text = context.getString(R.string.product_archive_button)
                binding.btnArchive.setOnClickListener { onArchiveClick(product) }
            }

            // Tags
            val tagNames = product.productTagMap?.mapNotNull { it.tag?.name }
            if (!tagNames.isNullOrEmpty()) {
                binding.tvTags.visibility = View.VISIBLE
                binding.tvTags.text = tagNames.joinToString(" • ")
            } else {
                binding.tvTags.visibility = View.GONE
            }

            // Name & Price
            binding.tvProductName.text = product.name
            binding.tvProductPrice.text = currencyFormat.format(product.grossPrice)

            // VAT Info
            binding.tvVatInfo.text = if (product.isVatEnabled) {
                val ratePercent = (product.vatRateVal * 100).toInt()
                "IVA: $ratePercent%"
            } else {
                "IVA: Exento"
            }

            // Deposit Info
            binding.tvDepositInfo.text = if (product.isDepositEnabled) {
                "Fianza: ${currencyFormat.format(product.depositAmountVal)}"
            } else {
                "Fianza: No"
            }

            // Actions
            binding.btnEdit.setOnClickListener { onEditClick(product) }
        }
    }
}
