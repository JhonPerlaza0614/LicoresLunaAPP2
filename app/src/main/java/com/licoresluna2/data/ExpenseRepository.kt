package com.licoresluna2.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseProductRow(val name: String)

@Serializable
data class ExpenseProductOption(val id: String, val name: String)

@Serializable
data class ExpenseRow(
    val id: String,
    val amount: Double,
    val note: String,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("quantity_bought") val quantityBought: Int? = null,
    @SerialName("created_at") val createdAt: String,
    val product: ExpenseProductRow? = null
)

@Serializable
data class ExpenseInsert(
    val amount: Double,
    val note: String,
    @SerialName("product_id") val productId: String?,
    @SerialName("quantity_bought") val quantityBought: Int?
)

@Serializable
data class StockRow(
    @SerialName("qty_on_hand") val qtyOnHand: Int = 0
)

@Serializable
data class InventoryUpsert(
    @SerialName("product_id") val productId: String,
    @SerialName("qty_on_hand") val qtyOnHand: Int
)

class ExpenseRepository(private val supabase: SupabaseClient) {

    suspend fun loadExpenses(): List<ExpenseRow> =
        supabase.from("expense")
            .select(
                Columns.raw("id,amount,note,product_id,quantity_bought,created_at,product:product_id(name)")
            ) {
                order("created_at", Order.DESCENDING)
            }
            .decodeList()

    suspend fun loadProducts(): List<ExpenseProductOption> =
        supabase.from("product")
            .select(Columns.list("id", "name")) {
                filter { eq("active", true) }
                order("name", Order.ASCENDING)
            }
            .decodeList()

    suspend fun registerExpense(
        amount: Double,
        note: String,
        productId: String?,
        quantity: Int?
    ) {
        supabase.from("expense").insert(ExpenseInsert(amount, note, productId, quantity))

        if (productId != null && quantity != null) {
            val current = supabase.from("inventory")
                .select(Columns.list("qty_on_hand")) {
                    filter { eq("product_id", productId) }
                }
                .decodeSingleOrNull<StockRow>()?.qtyOnHand ?: 0

            supabase.from("inventory").upsert(
                InventoryUpsert(productId, current + quantity),
                onConflict = "product_id"
            )
        }
    }
}