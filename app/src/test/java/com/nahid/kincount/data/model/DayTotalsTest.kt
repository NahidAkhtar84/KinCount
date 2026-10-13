package com.nahid.kincount.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DayTotalsTest {

    private fun tx(type: TxType, amount: Double) = Transaction(
        id = "t", type = type, amount = amount, categoryId = "cat", note = "",
        rawInput = null, owner = "local", ownerName = "Me",
        timestamp = 0L, createdAt = 0L, updatedAt = 0L,
    )

    @Test
    fun `empty list totals to zero`() {
        val totals = emptyList<Transaction>().dayTotals()
        assertEquals(0.0, totals.spent, 1e-9)
        assertEquals(0.0, totals.earned, 1e-9)
    }

    @Test
    fun `sums expenses and income separately`() {
        val totals = listOf(
            tx(TxType.EXPENSE, 250.0),
            tx(TxType.EXPENSE, 30.0),
            tx(TxType.INCOME, 45_000.0),
        ).dayTotals()
        assertEquals(280.0, totals.spent, 1e-9)
        assertEquals(45_000.0, totals.earned, 1e-9)
    }
}
