package com.licoresluna2.views

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.licoresluna2.LicoresLunaApp
import com.licoresluna2.R
import com.licoresluna2.data.models.InventoryUpsertDto
import com.licoresluna2.data.models.ProductAdminRow
import com.licoresluna2.data.models.ProductInsertDto
import com.licoresluna2.data.models.ProductInsertedId
import com.licoresluna2.data.models.ProductUpdateDto
import com.licoresluna2.data.models.TagMapInsertDto
import com.licoresluna2.data.models.TagOption
import com.licoresluna2.databinding.ActivityProductsBinding
import com.licoresluna2.views.adapters.ProductAdapter
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

class ProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductsBinding
    private val app by lazy { application as LicoresLunaApp }

    private lateinit var adapter: ProductAdapter

    private var allProducts: List<ProductAdminRow> = emptyList()
    private var availableTags: List<TagOption> = emptyList()

    // Form state
    private val selectedTagIds = mutableSetOf<String>()
    private var editingProductId: String? = null
    private var selectedImageUri: Uri? = null
    private var selectedImageBytes: ByteArray? = null
    private var selectedImageExtension: String = "jpg"

    // List Filter state
    private var selectedFilterCategoryTagId: String? = null
    private var selectedFilterStock: String = "ALL" // "ALL", "ACTIVE", "AVAILABLE", "OUT_OF_STOCK", "ARCHIVED"

    private val selectImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                selectedImageBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val fileName = getFileNameFromUri(uri) ?: "imagen.jpg"
                binding.tvSelectedFileName.text = fileName
                selectedImageExtension = fileName.substringAfterLast('.', "jpg")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        setupListeners()
        setupFilterStockChips()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            onEditClick = { product -> startEdit(product) },
            onArchiveClick = { product -> confirmArchiveProduct(product) },
            onRestoreClick = { product -> restoreProduct(product.id) },
        )
        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.isNestedScrollingEnabled = false
        binding.rvProducts.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnToggleCreateForm.setOnClickListener {
            toggleFormVisibility()
        }

        binding.swVatEnabled.setOnCheckedChangeListener { _, isChecked ->
            binding.tilVatRate.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.swDepositEnabled.setOnCheckedChangeListener { _, isChecked ->
            binding.tilDepositAmount.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        binding.btnSubmitProduct.setOnClickListener {
            submitForm()
        }

        binding.btnCancelEdit.setOnClickListener {
            resetForm()
            toggleFormVisibility(show = false)
        }

        binding.etSearch.doAfterTextChanged {
            filterProducts()
        }
    }

    private fun toggleFormVisibility(show: Boolean? = null) {
        val shouldShow = show ?: binding.containerForm.isGone
        binding.containerForm.visibility = if (shouldShow) View.VISIBLE else View.GONE
        binding.btnToggleCreateForm.text = if (shouldShow) {
            getString(R.string.product_hide_form)
        } else {
            getString(R.string.product_new_button)
        }
        if (shouldShow) {
            binding.scrollView.smoothScrollTo(0, 0)
        }
    }

    private fun loadData() {
        setLoadingList(loading = true)
        hideStatusMessage()

        lifecycleScope.launch {
            try {
                val productsData = app.supabaseClient.postgrest.from("product").select(
                    columns = Columns.raw("id,name,price_gross,vat_enabled,vat_rate,deposit_enabled,deposit_amount,image_url,active,product_tag_map(tag(id,name)),inventory(qty_on_hand)"),
                ) {
                    order("created_at", order = Order.DESCENDING)
                }.decodeList<ProductAdminRow>()

                val tagsData = app.supabaseClient.postgrest.from("tag").select(
                    columns = Columns.raw("id,name")
                ) {
                    order("name", order = Order.ASCENDING)
                }.decodeList<TagOption>()

                allProducts = productsData
                availableTags = tagsData

                setupFormTagChips()
                setupFilterCategoryChips()
                filterProducts()

            } catch (e: Exception) {
                showStatusMessage(
                    e.localizedMessage ?: getString(R.string.product_error_generic),
                    isSuccess = false,
                )
            } finally {
                setLoadingList(false)
            }
        }
    }

    private fun setupFormTagChips() {
        binding.chipGroupTags.removeAllViews()
        for (tag in availableTags) {
            val chip = Chip(this).apply {
                text = tag.name
                isCheckable = true
                isChecked = selectedTagIds.contains(tag.id)
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTagIds.add(tag.id)
                    } else {
                        selectedTagIds.remove(tag.id)
                    }
                }
            }
            binding.chipGroupTags.addView(chip)
        }
    }

    private fun setupFilterCategoryChips() {
        binding.chipGroupFilterCategories.setOnCheckedStateChangeListener(null)
        binding.chipGroupFilterCategories.removeAllViews()

        val allChipId = View.generateViewId()
        val allChip = Chip(this).apply {
            id = allChipId
            text = getString(R.string.filter_all_categories)
            isCheckable = true
        }
        binding.chipGroupFilterCategories.addView(allChip)

        val tagIdMap = mutableMapOf<Int, String>()
        for (tag in availableTags) {
            val chipId = View.generateViewId()
            val chip = Chip(this).apply {
                id = chipId
                text = tag.name
                isCheckable = true
            }
            tagIdMap[chipId] = tag.id
            binding.chipGroupFilterCategories.addView(chip)
        }

        val targetChipId = tagIdMap.entries.firstOrNull { it.value == selectedFilterCategoryTagId }?.key ?: allChipId
        binding.chipGroupFilterCategories.check(targetChipId)

        binding.chipGroupFilterCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            if (checkedId == null || checkedId == allChipId) {
                selectedFilterCategoryTagId = null
            } else {
                selectedFilterCategoryTagId = tagIdMap[checkedId]
            }
            filterProducts()
        }
    }

    private fun setupFilterStockChips() {
        binding.chipGroupFilterStock.setOnCheckedStateChangeListener(null)
        binding.chipGroupFilterStock.removeAllViews()

        val options = listOf(
            "ALL" to getString(R.string.filter_status_all),
            "ACTIVE" to getString(R.string.filter_status_active),
            "AVAILABLE" to getString(R.string.filter_status_available),
            "OUT_OF_STOCK" to getString(R.string.filter_status_out_of_stock),
            "ARCHIVED" to getString(R.string.filter_status_archived),
        )

        val stockIdMap = mutableMapOf<Int, String>()
        var defaultCheckId = View.NO_ID

        for ((code, label) in options) {
            val chipId = View.generateViewId()
            val chip = Chip(this).apply {
                id = chipId
                text = label
                isCheckable = true
            }
            stockIdMap[chipId] = code
            if (code == selectedFilterStock) {
                defaultCheckId = chipId
            }
            binding.chipGroupFilterStock.addView(chip)
        }

        if (defaultCheckId != View.NO_ID) {
            binding.chipGroupFilterStock.check(defaultCheckId)
        }

        binding.chipGroupFilterStock.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            selectedFilterStock = stockIdMap[checkedId] ?: "ALL"
            filterProducts()
        }
    }

    private fun filterProducts() {
        val query = binding.etSearch.text?.toString()?.trim()?.lowercase().orEmpty()

        val filtered = allProducts.filter { product ->
            // 1. Search Query Filter
            val matchesSearch = query.isEmpty() || product.name.lowercase().contains(query)

            // 2. Category Filter
            val matchesCategory = if (selectedFilterCategoryTagId == null) {
                true
            } else {
                product.productTagMap?.any { it.tag?.id == selectedFilterCategoryTagId } ?: false
            }

            // 3. Status / Stock Filter
            val qtyOnHand = product.stockQty
            val matchesStatus = when (selectedFilterStock) {
                "ACTIVE" -> product.isActive
                "AVAILABLE" -> product.isActive && qtyOnHand > 0
                "OUT_OF_STOCK" -> product.isActive && qtyOnHand <= 0
                "ARCHIVED" -> !product.isActive
                else -> true
            }

            matchesSearch && matchesCategory && matchesStatus
        }

        adapter.submitList(filtered)
    }

    private fun submitForm() {
        val name = binding.etProductName.text?.toString()?.trim().orEmpty()
        val price = binding.etProductPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
        val stock = binding.etInitialStock.text?.toString()?.toIntOrNull() ?: 0
        val vatEnabled = binding.swVatEnabled.isChecked
        val vatRate = if (vatEnabled) {
            binding.etVatRate.text?.toString()?.toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }
        val depositEnabled = binding.swDepositEnabled.isChecked
        val depositAmount = if (depositEnabled) {
            binding.etDepositAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        if (name.isEmpty()) {
            showStatusMessage("Por favor ingresa el nombre del producto.", isSuccess = false)
            binding.etProductName.requestFocus()
            return
        }

        val isUpdating = editingProductId != null
        val confirmTitle = if (isUpdating) "Guardar Cambios" else "Crear Producto"
        val confirmMsg = if (isUpdating) {
            getString(R.string.product_confirm_update, name)
        } else {
            getString(R.string.product_confirm_create, name)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(confirmTitle)
            .setMessage(confirmMsg)
            .setPositiveButton("Confirmar") { _, _ ->
                executeFormSubmit(
                    name = name,
                    price = price,
                    stock = stock,
                    vatEnabled = vatEnabled,
                    vatRate = vatRate,
                    depositEnabled = depositEnabled,
                    depositAmount = depositAmount,
                    isUpdating = isUpdating,
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executeFormSubmit(
        name: String,
        price: Double,
        stock: Int,
        vatEnabled: Boolean,
        vatRate: Double,
        depositEnabled: Boolean,
        depositAmount: Double,
        isUpdating: Boolean,
    ) {
        setLoadingForm(true)
        hideStatusMessage()

        lifecycleScope.launch {
            try {
                var productId = editingProductId
                var finalImageUrl: String? = null

                if (isUpdating && productId != null) {
                    // Update flow
                    if (selectedImageBytes != null) {
                        finalImageUrl = uploadProductImage(productId, selectedImageBytes!!, selectedImageExtension)
                    }

                    app.supabaseClient.postgrest.from("product").update(
                        ProductUpdateDto(
                            name = name,
                            priceGross = price,
                            vatEnabled = vatEnabled,
                            vatRate = vatRate,
                            depositEnabled = depositEnabled,
                            depositAmount = depositAmount,
                            imageUrl = finalImageUrl,
                        )
                    ) {
                        filter { eq("id", productId) }
                    }

                    // Upsert inventory
                    app.supabaseClient.postgrest.from("inventory").upsert(
                        InventoryUpsertDto(productId = productId, qtyOnHand = stock),
                        onConflict = "product_id",
                    )

                    // Delete existing tag map
                    app.supabaseClient.postgrest.from("product_tag_map").delete {
                        filter { eq("product_id", productId) }
                    }

                    // Insert new tag map
                    if (selectedTagIds.isNotEmpty()) {
                        val mappings = selectedTagIds.map { tagId ->
                            TagMapInsertDto(productId = productId, tagId = tagId)
                        }
                        app.supabaseClient.postgrest.from("product_tag_map").insert(mappings)
                    }

                    showStatusMessage(getString(R.string.product_updated_success), isSuccess = true)

                } else {
                    // Insert flow
                    val inserted = app.supabaseClient.postgrest.from("product").insert(
                        ProductInsertDto(
                            name = name,
                            priceGross = price,
                            vatEnabled = vatEnabled,
                            vatRate = vatRate,
                            depositEnabled = depositEnabled,
                            depositAmount = depositAmount,
                        )
                    ) {
                        select(Columns.raw("id"))
                    }.decodeList<ProductInsertedId>()

                    if (inserted.isEmpty()) {
                        throw IllegalStateException("No se pudo obtener el ID del producto creado.")
                    }

                    productId = inserted.first().id

                    // Upload image if provided
                    if (selectedImageBytes != null) {
                        finalImageUrl = uploadProductImage(productId, selectedImageBytes!!, selectedImageExtension)
                        app.supabaseClient.postgrest.from("product").update(
                            mapOf("image_url" to finalImageUrl)
                        ) {
                            filter { eq("id", productId) }
                        }
                    }

                    // Upsert inventory
                    app.supabaseClient.postgrest.from("inventory").upsert(
                        InventoryUpsertDto(productId = productId, qtyOnHand = stock),
                        onConflict = "product_id",
                    )

                    // Tag mappings
                    if (selectedTagIds.isNotEmpty()) {
                        val mappings = selectedTagIds.map { tagId ->
                            TagMapInsertDto(productId = productId, tagId = tagId)
                        }
                        app.supabaseClient.postgrest.from("product_tag_map").insert(mappings)
                    }

                    showStatusMessage(getString(R.string.product_created_success), isSuccess = true)
                }

                resetForm()
                toggleFormVisibility(show = false)
                loadData()

            } catch (e: Exception) {
                e.printStackTrace()
                showStatusMessage(
                    e.localizedMessage ?: getString(R.string.product_error_generic),
                    isSuccess = false,
                )
            } finally {
                setLoadingForm(false)
            }
        }
    }

    private suspend fun uploadProductImage(productId: String, bytes: ByteArray, extension: String): String {
        val path = "$productId-${System.currentTimeMillis()}.$extension"
        val bucket = app.supabaseClient.storage.from("product-images")
        bucket.upload(path, bytes, upsert = true)
        return bucket.publicUrl(path)
    }

    private fun startEdit(product: ProductAdminRow) {
        editingProductId = product.id
        binding.tvFormHeaderTitle.text = getString(R.string.products_header_edit)
        binding.etProductName.setText(product.name)
        binding.etProductPrice.setText(product.grossPrice.toString())
        binding.etInitialStock.setText(product.stockQty.toString())

        binding.swVatEnabled.isChecked = product.isVatEnabled
        binding.tilVatRate.visibility = if (product.isVatEnabled) View.VISIBLE else View.GONE
        binding.etVatRate.setText(product.vatRateVal.toString())

        binding.swDepositEnabled.isChecked = product.isDepositEnabled
        binding.tilDepositAmount.visibility = if (product.isDepositEnabled) View.VISIBLE else View.GONE
        binding.etDepositAmount.setText(product.depositAmountVal.toString())

        selectedTagIds.clear()
        product.productTagMap?.forEach { map ->
            map.tag?.id?.let { selectedTagIds.add(it) }
        }
        setupFormTagChips()

        selectedImageUri = null
        selectedImageBytes = null
        binding.tvSelectedFileName.text = product.imageUrl ?: "Mantiene imagen actual"

        binding.btnSubmitProduct.text = getString(R.string.product_submit_update)
        binding.btnCancelEdit.visibility = View.VISIBLE

        toggleFormVisibility(show = true)
    }

    private fun confirmArchiveProduct(product: ProductAdminRow) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Archivar Producto")
            .setMessage(getString(R.string.product_confirm_archive, product.name))
            .setPositiveButton("Archivar") { _, _ ->
                archiveProduct(product.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun archiveProduct(productId: String) {
        setLoadingList(loading = true)
        lifecycleScope.launch {
            try {
                app.supabaseClient.postgrest.from("product").update(
                    mapOf("active" to false)
                ) {
                    filter { eq("id", productId) }
                }
                showStatusMessage(getString(R.string.product_archived_success), isSuccess = true)
                loadData()
            } catch (e: Exception) {
                showStatusMessage(
                    e.localizedMessage ?: getString(R.string.product_error_generic),
                    isSuccess = false,
                )
            } finally {
                setLoadingList(false)
            }
        }
    }

    private fun restoreProduct(productId: String) {
        setLoadingList(loading = true)
        lifecycleScope.launch {
            try {
                app.supabaseClient.postgrest.from("product").update(
                    mapOf("active" to true)
                ) {
                    filter { eq("id", productId) }
                }
                showStatusMessage(getString(R.string.product_unarchived_success), isSuccess = true)
                loadData()
            } catch (e: Exception) {
                showStatusMessage(
                    e.localizedMessage ?: getString(R.string.product_error_generic),
                    isSuccess = false,
                )
            } finally {
                setLoadingList(false)
            }
        }
    }

    private fun resetForm() {
        editingProductId = null
        binding.tvFormHeaderTitle.text = getString(R.string.products_header_new)
        binding.etProductName.text?.clear()
        binding.etProductPrice.text?.clear()
        binding.etInitialStock.text?.clear()
        binding.swVatEnabled.isChecked = false
        binding.etVatRate.text?.clear()
        binding.swDepositEnabled.isChecked = false
        binding.etDepositAmount.text?.clear()
        selectedTagIds.clear()
        setupFormTagChips()
        selectedImageUri = null
        selectedImageBytes = null
        binding.tvSelectedFileName.text = getString(R.string.no_file_selected)
        binding.btnSubmitProduct.text = getString(R.string.product_submit_create)
        binding.btnCancelEdit.visibility = View.GONE
    }

    private fun setLoadingList(loading: Boolean) {
        binding.progressBarList.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun setLoadingForm(loading: Boolean) {
        binding.progressBarForm.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSubmitProduct.isEnabled = !loading
        binding.btnCancelEdit.isEnabled = !loading
    }

    private fun showStatusMessage(message: String, isSuccess: Boolean) {
        binding.tvStatusMessage.text = message
        binding.tvStatusMessage.setTextColor(
            getColor(if (isSuccess) R.color.stock_available else R.color.stock_empty)
        )
        binding.tvStatusMessage.setBackgroundResource(
            if (isSuccess) R.drawable.quick_sale_success_bg else R.drawable.quick_sale_error_bg
        )
        binding.tvStatusMessage.visibility = View.VISIBLE
    }

    private fun hideStatusMessage() {
        binding.tvStatusMessage.visibility = View.GONE
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) name = cursor.getString(index)
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                name = name.substring(cut + 1)
            }
        }
        return name
    }
}
