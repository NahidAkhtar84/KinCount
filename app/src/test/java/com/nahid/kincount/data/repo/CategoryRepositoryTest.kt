package com.nahid.kincount.data.repo

import com.nahid.kincount.data.FakeCategoryDao
import com.nahid.kincount.data.local.CategorySeed
import com.nahid.kincount.data.testCategoryEntity
import com.nahid.kincount.data.model.TxType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CategoryRepositoryTest {

    private lateinit var dao: FakeCategoryDao
    private lateinit var repository: CategoryRepository

    @Before
    fun setUp() {
        dao = FakeCategoryDao()
        repository = CategoryRepository(dao)
    }

    @Test
    fun `seeding an empty database inserts all defaults`() = runTest {
        repository.seedDefaultsIfEmpty()
        assertEquals(CategorySeed.defaults.size, dao.count())
    }

    @Test
    fun `seeding is idempotent - never duplicates`() = runTest {
        repository.seedDefaultsIfEmpty()
        repository.seedDefaultsIfEmpty()
        assertEquals(CategorySeed.defaults.size, dao.count())
    }

    @Test
    fun `seeding skips when user categories already exist`() = runTest {
        dao.upsertAll(listOf(testCategoryEntity(id = "cat_custom", name = "Pets")))
        repository.seedDefaultsIfEmpty()
        assertEquals(1, dao.count())
    }

    @Test
    fun `observeAll orders by sortOrder`() = runTest {
        dao.upsertAll(
            listOf(
                testCategoryEntity(id = "b", sortOrder = 20),
                testCategoryEntity(id = "a", sortOrder = 10),
            ),
        )
        val all = dao.observeAll().first()
        assertEquals(listOf("a", "b"), all.map { it.id })
    }

    @Test
    fun `get returns category by id or null`() = runTest {
        dao.upsertAll(listOf(testCategoryEntity()))
        assertEquals("Food & Dining", repository.get("cat_food")?.name)
        assertEquals(TxType.EXPENSE, repository.get("cat_food")?.type)
        assertNull(repository.get("missing"))
        assertNotNull(repository.get("cat_food"))
    }
}
