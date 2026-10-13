package com.nahid.kincount.data.repo

import com.nahid.kincount.data.local.TransactionDao
import com.nahid.kincount.data.local.toDomain
import com.nahid.kincount.data.local.toEntity
import com.nahid.kincount.data.model.Transaction
import com.nahid.kincount.data.model.TxType
import com.nahid.kincount.util.Dates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TransactionRepository(private val dao: TransactionDao) {

    fun observeDay(day: LocalDate): Flow<List<Transaction>> {
        val (from, to) = Dates.dayRangeMillis(day)
        return dao.observeDay(from, to).map { list -> list.map { it.toDomain() } }
    }

    suspend fun save(tx: Transaction) = dao.upsert(tx.toEntity())

    suspend fun get(id: String): Transaction? = dao.getById(id)?.toDomain()

    suspend fun softDelete(id: String) = dao.setDeleted(id, true, System.currentTimeMillis())

    suspend fun restore(id: String) = dao.setDeleted(id, false, System.currentTimeMillis())
}
