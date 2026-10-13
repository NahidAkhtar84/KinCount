package com.nahid.kincount.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nahid.kincount.data.model.Category
import com.nahid.kincount.data.model.Transaction
import com.nahid.kincount.data.model.TxType

// Owner stays "local" until Google sign-in lands in M2; existing rows get backfilled then.
const val OWNER_LOCAL = "local"

const val SYNC_PENDING = "PENDING"
const val SYNC_SYNCED = "SYNCED"

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amount: Double,
    val categoryId: String,
    val note: String,
    val rawInput: String?,
    val owner: String,
    val ownerName: String,
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val syncState: String,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val emoji: String,
    val isDefault: Boolean,
    val sortOrder: Int,
    val createdBy: String,
    val updatedAt: Long,
    val deleted: Boolean,
    val syncState: String,
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    type = TxType.valueOf(type),
    amount = amount,
    categoryId = categoryId,
    note = note,
    rawInput = rawInput,
    owner = owner,
    ownerName = ownerName,
    timestamp = timestamp,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deleted = deleted,
)

fun Transaction.toEntity(syncState: String = SYNC_PENDING) = TransactionEntity(
    id = id,
    type = type.name,
    amount = amount,
    categoryId = categoryId,
    note = note,
    rawInput = rawInput,
    owner = owner,
    ownerName = ownerName,
    timestamp = timestamp,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deleted = deleted,
    syncState = syncState,
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    type = TxType.valueOf(type),
    emoji = emoji,
    isDefault = isDefault,
    sortOrder = sortOrder,
)

fun Category.toEntity(createdBy: String, updatedAt: Long, syncState: String = SYNC_PENDING) =
    CategoryEntity(
        id = id,
        name = name,
        type = type.name,
        emoji = emoji,
        isDefault = isDefault,
        sortOrder = sortOrder,
        createdBy = createdBy,
        updatedAt = updatedAt,
        deleted = false,
        syncState = syncState,
    )
