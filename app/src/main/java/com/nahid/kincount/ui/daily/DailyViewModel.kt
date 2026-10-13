package com.nahid.kincount.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nahid.kincount.data.model.Category
import com.nahid.kincount.data.model.Transaction
import com.nahid.kincount.data.repo.CategoryRepository
import com.nahid.kincount.data.repo.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DailyViewModel(
    private val transactions: TransactionRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val categories: StateFlow<List<Category>> = categoryRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val entries: StateFlow<List<Transaction>> = _selectedDate
        .flatMapLatest { transactions.observeDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _deleteEvents = MutableSharedFlow<Transaction>(extraBufferCapacity = 4)
    val deleteEvents: SharedFlow<Transaction> = _deleteEvents

    fun selectDate(day: LocalDate) {
        _selectedDate.value = day
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun goToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun delete(tx: Transaction) {
        viewModelScope.launch {
            transactions.softDelete(tx.id)
            _deleteEvents.emit(tx)
        }
    }

    fun undoDelete(tx: Transaction) {
        viewModelScope.launch { transactions.restore(tx.id) }
    }
}
