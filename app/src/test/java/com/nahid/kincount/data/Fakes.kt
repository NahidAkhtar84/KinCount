package com.nahid.kincount.data

import com.nahid.kincount.data.local.CategoryDao
import com.nahid.kincount.data.local.CategoryEntity
import com.nahid.kincount.data.local.OWNER_LOCAL
import com.nahid.kincount.data.local.SYNC_PENDING
import com.nahid.kincount.data.local.TransactionDao
import com.nahid.kincount.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory DAO fakes so repositories and ViewModels are testable on the JVM (no device). */

class FakeTransactionDao : TransactionDao {

    val rows = MutableStateFlow<List<TransactionEntity>>(emptyList())

    /** Non-suspend seeding for test setup. */
    fun seed(list: List<TransactionEntity>) {
        rows.value = list
    }

    override suspend fun upsert(tx: TransactionEntity) {
        rows.value = rows.value.filterNot { it.id == tx.id } + tx
    }

    override fun observeDay(from: Long, to: Long): Flow<List<TransactionEntity>> =
        rows.map { list ->
            list.filter { !it.deleted && it.timestamp >= from && it.timestamp < to }
                .sortedByDescending { it.timestamp }
        }

    override suspend fun getById(id: String): TransactionEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun setDeleted(id: String, deleted: Boolean, updatedAt: Long) {
        rows.value = rows.value.map {
            if (it.id == id) it.copy(deleted = deleted, updatedAt = updatedAt) else it
        }
    }
}

class FakeCategoryDao : CategoryDao {

    val rows = MutableStateFlow<List<CategoryEntity>>(emptyList())

    /** Non-suspend seeding for test setup. */
    fun seed(list: List<CategoryEntity>) {
        rows.value = list
    }

    override suspend fun upsertAll(categories: List<CategoryEntity>) {
        rows.value = rows.value.filterNot { existing -> categories.any { it.id == existing.id } } + categories
    }

    override fun observeAll(): Flow<List<CategoryEntity>> =
        rows.map { list -> list.sortedBy { it.sortOrder } }

    override suspend fun count(): Int = rows.value.size

    override suspend fun getById(id: String): CategoryEntity? =
        rows.value.firstOrNull { it.id == id }
}

fun testTransactionEntity(
    id: String = "t1",
    type: String = "EXPENSE",
    amount: Double = 250.0,
    categoryId: String = "cat_food",
    timestamp: Long = 1_000L,
    deleted: Boolean = false,
) = TransactionEntity(
    id = id,
    type = type,
    amount = amount,
    categoryId = categoryId,
    note = "Lunch",
    rawInput = null,
    owner = OWNER_LOCAL,
    ownerName = "Me",
    timestamp = timestamp,
    createdAt = timestamp,
    updatedAt = timestamp,
    deleted = deleted,
    syncState = SYNC_PENDING,
)

fun testCategoryEntity(
    id: String = "cat_food",
    name: String = "Food & Dining",
    type: String = "EXPENSE",
    sortOrder: Int = 10,
) = CategoryEntity(
    id = id,
    name = name,
    type = type,
    emoji = "🍔",
    isDefault = true,
    sortOrder = sortOrder,
    createdBy = OWNER_LOCAL,
    updatedAt = 0L,
    deleted = false,
    syncState = SYNC_PENDING,
)
