package com.nahid.kincount.ui.add

import com.nahid.kincount.MainDispatcherRule
import com.nahid.kincount.data.FakeCategoryDao
import com.nahid.kincount.data.FakeTransactionDao
import com.nahid.kincount.data.local.OWNER_LOCAL
import com.nahid.kincount.data.model.TxType
import com.nahid.kincount.data.repo.CategoryRepository
import com.nahid.kincount.data.repo.TransactionRepository
import com.nahid.kincount.data.testCategoryEntity
import com.nahid.kincount.data.testTransactionEntity
import com.nahid.kincount.util.Dates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var txDao: FakeTransactionDao
    private lateinit var catDao: FakeCategoryDao
    private val today = LocalDate.of(2026, 10, 13)

    @Before
    fun setUp() {
        txDao = FakeTransactionDao()
        catDao = FakeCategoryDao()
        catDao.seed(
            listOf(
                testCategoryEntity(id = "cat_food", name = "Food & Dining", type = "EXPENSE"),
                testCategoryEntity(id = "cat_salary", name = "Salary", type = "INCOME", sortOrder = 10),
            ),
        )
    }

    private fun vm(editingId: String? = null) =
        AddEditViewModel(TransactionRepository(txDao), CategoryRepository(catDao), editingId)

    private fun validForm(viewModel: AddEditViewModel) {
        viewModel.setAmount("250")
        viewModel.setCategory("cat_food")
        viewModel.setNote("Lunch")
    }

    @Test
    fun `blank form is invalid - needs amount and category`() = runTest {
        val viewModel = vm()
        assertFalse(viewModel.form.value.isValid)

        viewModel.setAmount("250")
        assertFalse(viewModel.form.value.isValid) // still no category

        viewModel.setCategory("cat_food")
        assertTrue(viewModel.form.value.isValid)
    }

    @Test
    fun `amount parsing accepts dots commas and ignores letters`() = runTest {
        val viewModel = vm()
        viewModel.setAmount("12,50")
        assertEquals(12.5, viewModel.form.value.amount!!, 1e-9)

        viewModel.setAmount("abc99")
        assertEquals(99.0, viewModel.form.value.amount!!, 1e-9)

        viewModel.setAmount("0")
        assertFalse(viewModel.form.value.isValid)
    }

    @Test
    fun `saving a new transaction persists with owner local and emits saved`() = runTest {
        val viewModel = vm()
        val savedEvents = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.saved.collect { savedEvents.add(it) }
        }

        viewModel.setType(TxType.EXPENSE)
        viewModel.setAmount("250")
        viewModel.setCategory("cat_food")
        viewModel.setNote("Lunch")
        viewModel.setDate(today)
        viewModel.setTime(LocalTime.of(13, 30))
        viewModel.save()
        advanceUntilIdle()

        assertEquals(1, savedEvents.size)
        val stored = txDao.rows.value.single()
        assertEquals(TxType.EXPENSE.name, stored.type)
        assertEquals(250.0, stored.amount, 1e-9)
        assertEquals("cat_food", stored.categoryId)
        assertEquals("Lunch", stored.note)
        assertEquals(OWNER_LOCAL, stored.owner)
        assertEquals(Dates.timestampOf(today, LocalTime.of(13, 30)), stored.timestamp)
        assertFalse(stored.deleted)
    }

    @Test
    fun `switching type clears the stale category pick`() = runTest {
        val viewModel = vm()
        viewModel.setCategory("cat_food")
        viewModel.setType(TxType.INCOME)
        assertEquals(null, viewModel.form.value.categoryId)
        assertFalse(viewModel.form.value.isValid)
    }

    @Test
    fun `editing loads the existing transaction into the form`() = runTest {
        val ts = Dates.timestampOf(today, LocalTime.of(14, 45))
        txDao.upsert(testTransactionEntity(id = "t1", timestamp = ts, amount = 80.0))
        val viewModel = vm(editingId = "t1")
        advanceUntilIdle()

        val form = viewModel.form.value
        assertTrue(form.loaded)
        assertEquals("80", form.amountText)
        assertEquals("cat_food", form.categoryId)
        assertEquals(TxType.EXPENSE, form.type)
        assertEquals(today, form.date)
        assertEquals(LocalTime.of(14, 45), form.time)
    }

    @Test
    fun `updating preserves id and createdAt but bumps updatedAt`() = runTest {
        val ts = Dates.timestampOf(today, LocalTime.of(14, 45))
        txDao.upsert(testTransactionEntity(id = "t1", timestamp = ts, amount = 80.0).copy(createdAt = 100L, updatedAt = 100L))
        val viewModel = vm(editingId = "t1")
        advanceUntilIdle()

        viewModel.setAmount("120")
        viewModel.save()
        advanceUntilIdle()

        val stored = txDao.rows.value.single { it.id == "t1" }
        assertEquals(120.0, stored.amount, 1e-9)
        assertEquals(100L, stored.createdAt)
        assertTrue(stored.updatedAt >= 100L)
    }

    @Test
    fun `editing a deleted transaction resurrects it as visible`() = runTest {
        txDao.upsert(testTransactionEntity(id = "t1", deleted = true))
        val viewModel = vm(editingId = "t1")
        advanceUntilIdle()

        viewModel.setAmount("10")
        viewModel.setCategory("cat_food")
        viewModel.save()
        advanceUntilIdle()

        assertNotNull(txDao.rows.value.single { it.id == "t1" && !it.deleted })
    }
}
