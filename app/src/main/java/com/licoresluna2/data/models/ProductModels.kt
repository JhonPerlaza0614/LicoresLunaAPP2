@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.licoresluna2.data.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object FlexibleInventorySerializer : KSerializer<List<ProductInventory>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FlexibleInventoryList")

    override fun serialize(encoder: Encoder, value: List<ProductInventory>?) {}

    override fun deserialize(decoder: Decoder): List<ProductInventory>? {
        val jsonDecoder = decoder as? JsonDecoder ?: return emptyList()
        return try {
            val element = jsonDecoder.decodeJsonElement()
            when (element) {
                is JsonArray -> {
                    element.mapNotNull { item ->
                        val obj = item as? JsonObject ?: return@mapNotNull null
                        val qty = obj["qty_on_hand"]?.jsonPrimitive?.intOrNull ?: 0
                        ProductInventory(qtyOnHand = qty)
                    }
                }
                is JsonObject -> {
                    val qty = element["qty_on_hand"]?.jsonPrimitive?.intOrNull ?: 0
                    listOf(ProductInventory(qtyOnHand = qty))
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

object FlexibleTagRefSerializer : KSerializer<TagRef?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FlexibleTagRef")

    override fun serialize(encoder: Encoder, value: TagRef?) {}

    override fun deserialize(decoder: Decoder): TagRef? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        return try {
            val element = jsonDecoder.decodeJsonElement()
            when (element) {
                is JsonArray -> {
                    val firstObj = element.firstOrNull() as? JsonObject
                    val id = firstObj?.get("id")?.jsonPrimitive?.content
                    val name = firstObj?.get("name")?.jsonPrimitive?.content
                    TagRef(id = id, name = name)
                }
                is JsonObject -> {
                    val id = element["id"]?.jsonPrimitive?.content
                    val name = element["name"]?.jsonPrimitive?.content
                    TagRef(id = id, name = name)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

@Serializable
data class TagOption(
    val id: String,
    val name: String,
)

@Serializable
data class TagRef(
    val id: String? = null,
    val name: String? = null,
)

@Serializable
data class ProductTagMap(
    @Serializable(with = FlexibleTagRefSerializer::class) val tag: TagRef? = null,
)

@Serializable
data class ProductInventory(
    @SerialName("qty_on_hand") val qtyOnHand: Int = 0,
)

@Serializable
data class ProductAdminRow(
    val id: String = "",
    val name: String = "",
    @SerialName("price_gross") val priceGross: Double? = 0.0,
    @SerialName("vat_enabled") val vatEnabled: Boolean? = false,
    @SerialName("vat_rate") val vatRate: Double? = 0.0,
    @SerialName("deposit_enabled") val depositEnabled: Boolean? = false,
    @SerialName("deposit_amount") val depositAmount: Double? = 0.0,
    @SerialName("image_url") val imageUrl: String? = null,
    val active: Boolean? = true,
    @SerialName("product_tag_map") val productTagMap: List<ProductTagMap>? = emptyList(),
    @Serializable(with = FlexibleInventorySerializer::class) val inventory: List<ProductInventory>? = emptyList(),
) {
    val stockQty: Int
        get() = inventory?.firstOrNull()?.qtyOnHand ?: 0
    val isVatEnabled: Boolean
        get() = vatEnabled == true
    val isDepositEnabled: Boolean
        get() = depositEnabled == true
    val grossPrice: Double
        get() = priceGross ?: 0.0
    val vatRateVal: Double
        get() = vatRate ?: 0.0
    val depositAmountVal: Double
        get() = depositAmount ?: 0.0
    val isActive: Boolean
        get() = active != false
}

@Serializable
data class ProductInsertDto(
    val name: String,
    @SerialName("price_gross") val priceGross: Double,
    @SerialName("vat_enabled") val vatEnabled: Boolean,
    @SerialName("vat_rate") val vatRate: Double,
    @SerialName("deposit_enabled") val depositEnabled: Boolean,
    @SerialName("deposit_amount") val depositAmount: Double,
)

@Serializable
data class ProductUpdateDto(
    val name: String,
    @SerialName("price_gross") val priceGross: Double,
    @SerialName("vat_enabled") val vatEnabled: Boolean,
    @SerialName("vat_rate") val vatRate: Double,
    @SerialName("deposit_enabled") val depositEnabled: Boolean,
    @SerialName("deposit_amount") val depositAmount: Double,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class InventoryUpsertDto(
    @SerialName("product_id") val productId: String,
    @SerialName("qty_on_hand") val qtyOnHand: Int,
)

@Serializable
data class TagMapInsertDto(
    @SerialName("product_id") val productId: String,
    @SerialName("tag_id") val tagId: String,
)

@Serializable
data class ProductInsertedId(
    val id: String,
)
