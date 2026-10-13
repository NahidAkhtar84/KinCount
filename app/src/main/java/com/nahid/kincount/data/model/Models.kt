package com.nahid.kincount.data.model

enum class TxType { EXPENSE, INCOME }

data class Transaction(
    val id: String,
    val type: TxType,
    val amount: Double,
    val categoryId: String,
    val note: String,
    val rawInput: String?,
    val owner: String,
    val ownerName: String,
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
)

data class Category(
    val id: String,
    val name: String,
    val type: TxType,
    val emoji: String,
    val isDefault: Boolean,
    val sortOrder: Int,
)

data class DayTotals(val spent: Double, val earned: Double)

/** Sums a list of transactions into day totals (expense / income separately). */
fun List<Transaction>.dayTotals(): DayTotals = fold(DayTotals(0.0, 0.0)) { acc, tx ->
    when (tx.type) {
        TxType.EXPENSE -> acc.copy(spent = acc.spent + tx.amount)
        TxType.INCOME -> acc.copy(earned = acc.earned + tx.amount)
    }
}
