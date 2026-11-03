package com.licoresluna2.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SummarySaleRow(
    val total: Double? = null,
    @SerialName("deposit_total") val depositTotal: Double? = null,
    @SerialName("subtotal_net") val subtotalNet: Double? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("created_at") val createdAt: String
) {
    val deposit: Double get() = depositTotal ?: 0.0

    // Igual que en React: si total > 0 se usa, si no, neto + depósito
    val effectiveTotal: Double
        get() = if ((total ?: 0.0) > 0.0) total!! else (subtotalNet ?: 0.0) + deposit
}

@Serializable
data class SummaryExpenseRow(
    val amount: Double? = null,
    @SerialName("created_at") val createdAt: String
)

class SummaryRepository(private val supabase: SupabaseClient) {

    suspend fun loadSales(): List<SummarySaleRow> =
        supabase.from("sale")
            .select(Columns.list("total", "deposit_total", "subtotal_net", "payment_method", "created_at")) {
                order("created_at", Order.DESCENDING)
            }
            .decodeList()

    suspend fun loadExpenses(): List<SummaryExpenseRow> =
        supabase.from("expense")
            .select(Columns.list("amount", "created_at"))
            .decodeList()
}