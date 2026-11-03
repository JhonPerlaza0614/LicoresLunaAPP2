package com.licoresluna2.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductOption(
    val id: String,
    val name: String
)

@Serializable
data class ProductName(
    val name: String
)

@Serializable
data class ExpenseRow(
    val id: String,
    val amount: Double,
    val note: String = "",
    @SerialName("product_id") val productId: String? = null,
    @SerialName("quantity_bought") val quantityBought: Int? = null,
    @SerialName("created_at") val createdAt: String,
    val product: ProductName? = null
)

@Serializable
data class ExpenseInsert(
    val amount: Double,
    val note: String,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("quantity_bought") val quantityBought: Int? = null
)

@Serializable
data class InventoryRow(
    @SerialName("qty_on_hand") val qtyOnHand: Int = 0
)

@Serializable
data class InventoryUpsert(
    @SerialName("product_id") val productId: String,
    @SerialName("qty_on_hand") val qtyOnHand: Int
)

class ExpensesRepository(private val supabase: SupabaseClient) {

    suspend fun loadExpenses(): List<ExpenseRow> {
        return supabase.from("expense")
            .select(
                Columns.raw(
                    "id,amount,note,product_id,quantity_bought,created_at,product:product_id(name)"
                )
            ) {
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun loadActiveProducts(): List<ProductOption> {
        return supabase.from("product")
            .select(Columns.list("id", "name")) {
                filter {
                    eq("active", true)
                }
                order("name", Order.ASCENDING)
            }
            .decodeList()
    }

    suspend fun insertExpense(expense: ExpenseInsert) {
        supabase.from("expense").insert(expense)
    }

    suspend fun getStock(productId: String): Int {
        val inventory = supabase.from("inventory")
            .select(Columns.list("qty_on_hand")) {
                filter {
                    eq("product_id", productId)
                }
            }
            .decodeSingleOrNull<InventoryRow>()

        return inventory?.qtyOnHand ?: 0
    }

    suspend fun upsertStock(productId: String, qtyOnHand: Int) {
        supabase.from("inventory").upsert(
            InventoryUpsert(
                productId = productId,
                qtyOnHand = qtyOnHand
            )
        )
    }
}