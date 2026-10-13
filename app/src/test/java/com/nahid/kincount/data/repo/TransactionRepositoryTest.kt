package com.nahid.kincount.data.repo

import com.nahid.kincount.data.FakeTransactionDao
import com.nahid.kincount.data.testTransactionEntity
import com.nahid.kincount.data.model.TxType
import com.nahid.kincount.util.Dates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TransactionRepositoryTest {

    private lateinit var dao: FakeTransactionDao
    private lateinit var repository: TransactionRepository

    private val today = LocalDate.of(2026, 10, 13)
    private val yesterday = today.minusDays(1)

    @Before
    fun setUp() {
        dao = FakeTransactionDao()
        repository = TransactionRepository(dao)
    }

    @Test
    fun `observeDay returns only that day's live entries newest first`() = runTest {
        dao.upsert(testTransactionEntity(id = "morning", timestamp = Dates.timestampOf(today, LocalTime.of(9, 0))))
        dao.upsert(testTransactionEntity(id = "afternoon", timestamp = Dates.timestampOf(today, LocalTime.of(15, 0))))
        dao.upsert(testTransactionEntity(id = "otherDay", timestamp = Dates.timestampOf(yesterday, LocalTime.of(12, 0))))
        dao.upsert(testTransactionEntity(id = "deleted", timestamp = Dates.timestampOf(today, LocalTime.of(10, 0)), deleted = true))

        val entries = repository.observeDay(today).first()

        assertEquals(listOf("afternoon", "morning"), entries.map { it.id })
    }

    @Test
    fun `save inserts and get maps back to domain`() = runTest {
        val tx = com.nahid.kincount.data.model.Transaction(
            id = "new", type = TxType.INCOME, amount = 500.0, categoryId = "cat_salary",
            note = "Bonus", rawInput = null, owner = "local", ownerName = "Me",
            timestamp = Dates.timestampOf(today, LocalTime.of(8, 0)),
            createdAt = 1L, updatedAt = 1L,
        )
        repository.save(tx)

        val loaded = repository.get("new")
        assertNotNull(loaded)
        assertEquals(TxType.INCOME, loaded?.type)
        assertEquals(500.0, loaded?.amount ?: 0.0, 1e-9)
    }

    @Test
    fun `get returns null for unknown id`() = runTest {
        assertNull(repository.get("nope"))
    }

    @Test
    fun `softDelete hides the entry and bumps updatedAt`() = runTest {
        dao.upsert(
            testTransactionEntity(id = "t1", timestamp = Dates.timestampOf(today, LocalTime.NOON))
                .copy(createdAt = 0L, updatedAt = 0L),
        )
        val before = dao.rows.value.first { it.id == "t1" }
        repository.softDelete("t1")

        val row = dao.rows.value.first { it.id == "t1" }
        assertTrue(row.deleted)
        assertTrue("updatedAt should be bumped", row.updatedAt > before.updatedAt)
        assertTrue(repository.observeDay(today).first().isEmpty())
    }

    @Test
    fun `restore brings a deleted entry back`() = runTest {
        dao.upsert(testTransactionEntity(id = "t1", timestamp = Dates.timestampOf(today, LocalTime.NOON)))
        repository.softDelete("t1")
        repository.restore("t1")

        assertEquals(1, repository.observeDay(today).first().size)
    }
}
