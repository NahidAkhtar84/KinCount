package com.nahid.kincount.data.local

import com.nahid.kincount.data.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategorySeedTest {

    @Test
    fun `seeds 25 defaults - 17 expense and 8 income`() {
        assertEquals(25, CategorySeed.defaults.size)
        assertEquals(17, CategorySeed.defaults.count { it.type == TxType.EXPENSE })
        assertEquals(8, CategorySeed.defaults.count { it.type == TxType.INCOME })
    }

    @Test
    fun `ids are globally unique`() {
        assertEquals(CategorySeed.defaults.size, CategorySeed.defaults.map { it.id }.toSet().size)
    }

    @Test
    fun `names are unique within each type`() {
        val expenses = CategorySeed.defaults.filter { it.type == TxType.EXPENSE }.map { it.name }
        val incomes = CategorySeed.defaults.filter { it.type == TxType.INCOME }.map { it.name }
        assertEquals(expenses.size, expenses.toSet().size)
        assertEquals(incomes.size, incomes.toSet().size)
    }

    @Test
    fun `every seed has an emoji and is marked default`() {
        CategorySeed.defaults.forEach {
            assertTrue("emoji blank for ${it.name}", it.emoji.isNotBlank())
            assertTrue(it.isDefault)
        }
    }
}
