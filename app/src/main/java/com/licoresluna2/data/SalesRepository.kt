package com.licoresluna2.data

import com.licoresluna2.data.models.SaleHistoryRow
import com.licoresluna2.data.models.SaleProduct
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class SalesRepository(private val supabase: SupabaseClient) {

    suspend fun loadProducts(): List<SaleProduct> {
        return supabase.from("product")
            .select(
                Columns.raw(
                    "id,name,price_gross,deposit_enabled,deposit_amount,inventory(qty_on_hand)"
                )
            ) {
                filter { eq("active", true) }
                order("name", Order.ASCENDING)
            }
            .decodeList()
    }

    suspend fun loadHistory(startIso: String, endIso: String): List<SaleHistoryRow> {
        return supabase.from("sale")
            .select(
                Columns.raw(
                    "id,created_at,payment_method,total,deposit_total,subtotal_net," +
                        "sale_item(product_id,qty,deposit_charged)"
                )
            ) {
                filter {
                    gte("created_at", startIso)
                    lte("created_at", endIso)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun createSale(
        paymentMethod: String,
        items: List<com.licoresluna2.data.models.SaleLine>
    ): String {
        val itemJson = buildJsonArray {
            items.forEach { line ->
                add(buildJsonObject {
                    put("product_id", line.productId)
                    put("qty", line.qty)
                    put(
                        "charge_deposit",
                        line.mode == com.licoresluna2.data.models.SaleLineMode.CHARGE
                    )
                })
            }
        }
        val parameters = buildJsonObject {
            put("p_payment_method", paymentMethod)
            put("p_items", itemJson)
        }

        val result = supabase.postgrest.rpc("create_sale", parameters)
            .decodeAs<JsonElement>()

        return when (result) {
            is JsonObject -> {
                result.values
                    .firstOrNull()
                    ?.jsonPrimitive
                    ?.content
                    .orEmpty()
            }
            is JsonArray -> result
                .firstOrNull()
                ?.let { element ->
                    (element as? JsonObject)?.values?.firstOrNull()
                        ?: element
                }
                ?.jsonPrimitive
                ?.content
                .orEmpty()
            else -> result.jsonPrimitive.content
        }
    }

    suspend fun updateSale(
        saleId: String,
        paymentMethod: String,
        items: List<com.licoresluna2.data.models.SaleLine>
    ) {
        val itemJson = buildJsonArray {
            items.forEach { line ->
                add(buildJsonObject {
                    put("product_id", line.productId)
                    put("qty", line.qty)
                    put(
                        "charge_deposit",
                        line.mode == com.licoresluna2.data.models.SaleLineMode.CHARGE
                    )
                })
            }
        }
        val parameters = buildJsonObject {
            put("p_sale_id", saleId)
            put("p_payment_method", paymentMethod)
            put("p_items", itemJson)
        }
        supabase.postgrest.rpc("update_sale", parameters)
    }

    suspend fun deleteSale(saleId: String) {
        supabase.postgrest.rpc(
            "delete_sale",
            buildJsonObject { put("p_sale_id", saleId) }
        )
    }

    suspend fun registerExchange(productId: String, qty: Int, saleId: String) {
        supabase.postgrest.rpc(
            "register_containers_received",
            buildJsonObject {
                put("p_product_id", productId)
                put("p_qty", qty)
                put("p_reason", "exchange_in")
                put("p_note", "Intercambio asociado a la venta $saleId")
            }
        )
    }
}
