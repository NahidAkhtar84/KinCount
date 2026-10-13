package com.nahid.kincount.util

import com.nahid.kincount.data.model.Transaction
import com.nahid.kincount.data.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    private fun tx(type: TxType, amount: Double) = Transaction(
        id = "t", type = type, amount = amount, categoryId = "cat_food", note = "",
        rawInput = null, owner = "local", ownerName = "Me",
        timestamp = 0L, createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `formats whole amounts with grouping and no decimals`() {
        assertEquals("৳ 1,250", Money.format(1250.0))
        assertEquals("৳ 12,450", Money.format(12450.0))
        assertEquals("৳ 0", Money.format(0.0))
    }

    @Test
    fun `keeps two decimals only when a fraction exists`() {
        assertEquals("৳ 250.50", Money.format(250.5))
        assertEquals("৳ 12,450.25", Money.format(12450.25))
    }

    @Test
    fun `signed form prefixes minus for expenses and plus for income`() {
        assertEquals("−৳ 250", Money.formatSigned(tx(TxType.EXPENSE, 250.0)))
        assertEquals("+৳ 1,200", Money.formatSigned(tx(TxType.INCOME, 1200.0)))
    }
}
