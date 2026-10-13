package com.nahid.kincount.ui.daily

import com.nahid.kincount.MainDispatcherRule
import com.nahid.kincount.data.FakeCategoryDao
import com.nahid.kincount.data.FakeTransactionDao
import com.nahid.kincount.data.model.Transaction
import com.nahid.kincount.data.repo.CategoryRepository
import com.nahid.kincount.data.repo.TransactionRepository
import com.nahid.kincount.data.testTransactionEntity
import com.nahid.kincount.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class DailyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var txDao: FakeTransactionDao
    private lateinit var viewModel: DailyViewModel
    private val today = LocalDate.of(2026, 10, 13)

    @Before
    fun setUp() {
        txDao = FakeTransactionDao()
        txDao.seed(
            listOf(
                testTransactionEntity(id = "morning", timestamp = Dates.timestampOf(today, LocalTime.of(9, 0))),
                testTransactionEntity(id = "afternoon", timestamp = Dates.timestampOf(today, LocalTime.of(15, 0))),
                testTransactionEntity(id = "yesterdayTx", timestamp = Dates.timestampOf(today.minusDays(1), LocalTime.NOON)),
                testTransactionEntity(id = "deletedTx", timestamp = Dates.timestampOf(today, LocalTime.of(10, 0)), deleted = true),
            ),
        )

        viewModel = DailyViewModel(TransactionRepository(txDao), CategoryRepository(FakeCategoryDao()))
    }

    private fun withToday(today: LocalDate = this.today) {
        viewModel.selectDate(today)
    }

    @Test
    fun `entries shows only the selected day's live rows newest first`() = runTest {
        val collected = mutableListOf<List<Transaction>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect { collected.add(it) }
        }
        withToday()
        advanceUntilIdle()

        assertEquals(listOf("afternoon", "morning"), collected.last().map { it.id })
    }

    @Test
    fun `previous day navigation re-queries the new date`() = runTest {
        val collected = mutableListOf<List<Transaction>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect { collected.add(it) }
        }
        withToday()
        viewModel.previousDay()
        advanceUntilIdle()

        assertEquals(listOf("yesterdayTx"), collected.last().map { it.id })
    }

    @Test
    fun `next day from yesterday returns to today`() = runTest {
        val collected = mutableListOf<List<Transaction>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect { collected.add(it) }
        }
        viewModel.selectDate(today.minusDays(1))
        viewModel.nextDay()
        advanceUntilIdle()

        assertEquals(listOf("afternoon", "morning"), collected.last().map { it.id })
    }

    @Test
    fun `go today jumps back from any date`() = runTest {
        viewModel.selectDate(today.minusWeeks(3))
        viewModel.goToday()
        assertEquals(LocalDate.now(), viewModel.selectedDate.value)
    }

    @Test
    fun `delete soft-deletes the row and emits the transaction for undo`() = runTest {
        val collected = mutableListOf<List<Transaction>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect { collected.add(it) }
        }
        val events = mutableListOf<Transaction>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.deleteEvents.collect { events.add(it) }
        }
        withToday()
        advanceUntilIdle()

        val target = viewModel.entries.value.first { it.id == "morning" }
        viewModel.delete(target)
        advanceUntilIdle()

        assertEquals("morning", events.single().id)
        assertTrue(txDao.rows.value.first { it.id == "morning" }.deleted)
        assertFalse(collected.last().any { it.id == "morning" })
    }

    @Test
    fun `undoDelete restores the row`() = runTest {
        val collected = mutableListOf<List<Transaction>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.entries.collect { collected.add(it) }
        }
        withToday()
        advanceUntilIdle()
        val target = viewModel.entries.value.first { it.id == "morning" }
        viewModel.delete(target)
        advanceUntilIdle()

        viewModel.undoDelete(target)
        advanceUntilIdle()

        assertFalse(txDao.rows.value.first { it.id == "morning" }.deleted)
        assertTrue(viewModel.entries.value.any { it.id == "morning" })
    }
}
