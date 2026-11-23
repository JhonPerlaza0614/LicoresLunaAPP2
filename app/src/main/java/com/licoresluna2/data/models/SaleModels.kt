package com.licoresluna2.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

object FlexibleBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("FlexibleBoolean")

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value)
    }

    override fun deserialize(decoder: Decoder): Boolean {
        val jsonDecoder = decoder as? JsonDecoder ?: return false
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> element.booleanOrNull
                ?: element.content.trim().lowercase(Locale.ROOT).let {
                    it == "true" || it == "1" || it == "t" || it == "yes"
                }
            else -> false
        }
    }
}

object FlexibleSaleInventorySerializer : KSerializer<List<SaleInventory>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("FlexibleSaleInventory")

    override fun serialize(encoder: Encoder, value: List<SaleInventory>) {}

    override fun deserialize(decoder: Decoder): List<SaleInventory> {
        val jsonDecoder = decoder as? JsonDecoder ?: return emptyList()
        return try {
            when (val element = jsonDecoder.decodeJsonElement()) {
                is JsonArray -> element.mapNotNull { item ->
                    val obj = item as? JsonObject ?: return@mapNotNull null
                    val qty = obj["qty_on_hand"]?.jsonPrimitive?.intOrNull ?: 0
                    SaleInventory(qty)
                }
                is JsonObject -> listOf(
                    SaleInventory(
                        element["qty_on_hand"]?.jsonPrimitive?.intOrNull ?: 0
                    )
                )
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

@Serializable
data class SaleProduct(
    val id: String,
    val name: String,
    @SerialName("price_gross") val priceGross: Double = 0.0,
    @SerialName("deposit_enabled") val depositEnabled: Boolean = false,
    @SerialName("deposit_amount") val depositAmount: Double? = null,
    @Serializable(with = FlexibleSaleInventorySerializer::class)
    val inventory: List<SaleInventory> = emptyList()
) {
    val stock: Int get() = inventory.firstOrNull()?.qtyOnHand ?: 0
}

@Serializable
data class SaleInventory(
    @SerialName("qty_on_hand") val qtyOnHand: Int = 0
)

@Serializable
data class SaleItemRow(
    @SerialName("product_id") val productId: String,
    val qty: Int = 1,
    @SerialName("deposit_charged")
    @Serializable(with = FlexibleBooleanSerializer::class)
    val depositCharged: Boolean = false
)

@Serializable
data class SaleHistoryRow(
    val id: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("payment_method") val paymentMethod: String,
    val total: Double = 0.0,
    @SerialName("deposit_total") val depositTotal: Double = 0.0,
    @SerialName("subtotal_net") val subtotalNet: Double = 0.0,
    @SerialName("sale_item") val saleItems: List<SaleItemRow> = emptyList()
)

data class SaleLine(
    val productId: String,
    var qty: Int = 1,
    var mode: SaleLineMode = SaleLineMode.CHARGE
)

enum class SaleLineMode {
    CHARGE,
    EXCHANGE
}
