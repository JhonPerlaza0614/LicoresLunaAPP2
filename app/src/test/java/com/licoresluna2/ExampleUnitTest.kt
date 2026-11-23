package com.licoresluna2

import com.licoresluna2.data.dateKeyFromIso
import com.licoresluna2.data.formatIsoDateTime
import com.licoresluna2.data.SummarySaleRow
import com.licoresluna2.data.models.ProductAdminRow
import com.licoresluna2.data.models.ProductInventory
import com.licoresluna2.data.models.SaleInventory
import com.licoresluna2.data.models.SaleProduct
import org.junit.Test
import org.junit.Assert.*

class AppUnitTest {
    @Test
    fun dateKeyFromIso_extractsDatePrefix() {
        assertEquals("2026-11-22", dateKeyFromIso("2026-11-22T14:30:00Z"))
    }

    @Test
    fun dateKeyFromIso_rejectsShortValue() {
        assertTrue(dateKeyFromIso("short").isNotBlank())
    }

    @Test
    fun formatIsoDateTime_returnsReadableDate() {
        val result = formatIsoDateTime("2026-11-22T14:30:00Z")

        assertTrue(result.contains("/"))
        assertTrue(result.contains("2026"))
    }

    @Test
    fun saleProduct_stockUsesFirstInventoryRow() {
        val product = SaleProduct(
            id = "1",
            name = "Ron",
            inventory = listOf(SaleInventory(qtyOnHand = 7))
        )

        assertEquals(7, product.stock)
    }

    @Test
    fun saleProduct_stockIsZeroWithoutInventory() {
        val product = SaleProduct(id = "1", name = "Ron")

        assertEquals(0, product.stock)
    }

    @Test
    fun productAdmin_stockAndFlagsUseDefaults() {
        val product = ProductAdminRow(
            id = "1",
            name = "Ron",
            priceGross = null,
            vatEnabled = null,
            depositEnabled = true,
            depositAmount = null,
            inventory = listOf(ProductInventory(qtyOnHand = 12))
        )

        assertEquals(12, product.stockQty)
        assertEquals(0.0, product.grossPrice, 0.0)
        assertFalse(product.isVatEnabled)
        assertTrue(product.isDepositEnabled)
        assertEquals(0.0, product.depositAmountVal, 0.0)
    }

    @Test
    fun summaryEffectiveTotal_usesStoredTotalWhenPositive() {
        val sale = SummarySaleRow(
            total = 25000.0,
            depositTotal = 3000.0,
            subtotalNet = 20000.0,
            createdAt = "2026-11-22T14:30:00Z"
        )

        assertEquals(25000.0, sale.effectiveTotal, 0.0)
        assertEquals(3000.0, sale.deposit, 0.0)
    }

    @Test
    fun summaryEffectiveTotal_addsNetAndDepositWhenTotalIsZero() {
        val sale = SummarySaleRow(
            total = 0.0,
            depositTotal = 3000.0,
            subtotalNet = 20000.0,
            createdAt = "2026-11-22T14:30:00Z"
        )

        assertEquals(23000.0, sale.effectiveTotal, 0.0)
    }

    @Test
    fun summaryTotals_canCalculateSalesExpensesAndNet() {
        val sales = listOf(
            SummarySaleRow(total = 10000.0, depositTotal = 1000.0, createdAt = ""),
            SummarySaleRow(total = 0.0, depositTotal = 2000.0, subtotalNet = 15000.0, createdAt = "")
        )

        val totalSales = sales.sumOf { it.effectiveTotal }
        val deposits = sales.sumOf { it.deposit }
        val expenses = listOf(3000.0, 2000.0).sum()
        val netReal = totalSales - expenses - deposits

        assertEquals(27000.0, totalSales, 0.0)
        assertEquals(3000.0, deposits, 0.0)
        assertEquals(5000.0, expenses, 0.0)
        assertEquals(19000.0, netReal, 0.0)
    }
}