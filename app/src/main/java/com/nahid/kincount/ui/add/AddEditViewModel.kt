package com.nahid.kincount.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nahid.kincount.data.local.OWNER_LOCAL
import com.nahid.kincount.data.model.Category
import com.nahid.kincount.data.model.Transaction
import com.nahid.kincount.data.model.TxType
import com.nahid.kincount.data.repo.CategoryRepository
import com.nahid.kincount.data.repo.TransactionRepository
import com.nahid.kincount.util.Dates
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class AddEditFormState(
    val type: TxType = TxType.EXPENSE,
    val amountText: String = "",
    val categoryId: String? = null,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val loaded: Boolean = true,
) {
    val amount: Double? get() = amountText.replace(',', '.').toDoubleOrNull()
    val isValid: Boolean get() = (amount ?: 0.0) > 0.0 && categoryId != null
}

class AddEditViewModel(
    private val transactions: TransactionRepository,
    categoryRepository: CategoryRepository,
    private val editingId: String?,
) : ViewModel() {

    private val _form = MutableStateFlow(AddEditFormState(loaded = editingId == null))
    val form: StateFlow<AddEditFormState> = _form

    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _saved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val saved: SharedFlow<Unit> = _saved

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val tx = transactions.get(editingId) ?: return@launch
                _form.value = AddEditFormState(
                    type = tx.type,
                    amountText = if (tx.amount == tx.amount.toInt().toDouble()) {
                        tx.amount.toInt().toString()
                    } else {
                        tx.amount.toString()
                    },
                    categoryId = tx.categoryId,
                    note = tx.note,
                    date = Dates.localDateOf(tx.timestamp),
                    time = Dates.localTimeOf(tx.timestamp),
                    loaded = true,
                )
            }
        }
    }

    fun setType(type: TxType) {
        // Categories are type-specific; dropping the stale pick avoids saving an income entry
        // with an expense category.
        _form.value = _form.value.copy(type = type, categoryId = null)
    }

    fun setAmount(text: String) {
        _form.value = _form.value.copy(amountText = text.filter { it.isDigit() || it == '.' || it == ',' })
    }

    fun setCategory(id: String) {
        _form.value = _form.value.copy(categoryId = id)
    }

    fun setNote(text: String) {
        _form.value = _form.value.copy(note = text)
    }

    fun setDate(date: LocalDate) {
        _form.value = _form.value.copy(date = date)
    }

    fun setTime(time: LocalTime) {
        _form.value = _form.value.copy(time = time)
    }

    fun save() {
        val state = _form.value
        val amount = state.amount
        val categoryId = state.categoryId
        if (amount == null || amount <= 0.0 || categoryId == null) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = editingId?.let { transactions.get(it) }
            val tx = Transaction(
                id = existing?.id ?: UUID.randomUUID().toString(),
                type = state.type,
                amount = amount,
                categoryId = categoryId,
                note = state.note.trim(),
                rawInput = existing?.rawInput,
                owner = existing?.owner ?: OWNER_LOCAL,
                ownerName = existing?.ownerName ?: "Me",
                timestamp = Dates.timestampOf(state.date, state.time),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                deleted = false,
            )
            transactions.save(tx)
            _saved.tryEmit(Unit)
        }
    }
}
