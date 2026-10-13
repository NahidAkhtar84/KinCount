package com.nahid.kincount.data.repo

import com.nahid.kincount.data.local.CategoryDao
import com.nahid.kincount.data.local.CategorySeed
import com.nahid.kincount.data.local.OWNER_LOCAL
import com.nahid.kincount.data.local.toDomain
import com.nahid.kincount.data.local.toEntity
import com.nahid.kincount.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(private val dao: CategoryDao) {

    fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun get(id: String): Category? = dao.getById(id)?.toDomain()

    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() == 0) {
            val now = System.currentTimeMillis()
            dao.upsertAll(CategorySeed.defaults.map { it.toEntity(OWNER_LOCAL, now) })
        }
    }
}
