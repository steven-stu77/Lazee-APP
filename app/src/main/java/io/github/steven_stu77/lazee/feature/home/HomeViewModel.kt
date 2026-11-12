package io.github.steven_stu77.lazee.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.LedgerMath
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.BillRepository
import io.github.steven_stu77.lazee.core.repo.BillStatus
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.repo.PendingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    val weekDots: List<Pair<LocalDate, Boolean>> = emptyList(),
    val monthExpense: Long = 0,
    val monthIncome: Long = 0,
    val budget: Long = 0,
    val pendingCount: Int = 0,
    val nextBill: BillStatus? = null,
    val groups: List<LedgerMath.DayGroup<TxnRow>> = emptyList(),
    val oldest: YearMonth = YearMonth.now(),
    val canLoadEarlier: Boolean = false,
    val selection: Set<Long> = emptySet(),
    val hideAmounts: Boolean = false,
    val categories: List<Category> = emptyList(),
) {
    val budgetLeft: Long get() = budget - monthExpense
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ledger: LedgerRepository,
    private val settings: SettingsRepository,
    pending: PendingRepository,
    bills: BillRepository,
) : ViewModel() {
    private val oldest = MutableStateFlow(YearMonth.now())
    private val selection = MutableStateFlow<Set<Long>>(emptySet())

    private val rows = oldest.flatMapLatest { from ->
        val today = LocalDate.now()
        combine(ledger.observeRange(from.atDay(1), YearMonth.from(today).atEndOfMonth()), ledger.observeCategories(), ledger.observeAccounts()) { t, c, a ->
            Triple(t, c, a)
        }
    }

    val state: StateFlow<HomeUiState> = combine(
        rows, settings.settings, pending.observeCount(), bills.observeMonth(), combine(oldest, selection) { o, s -> o to s },
    ) { (txns, cats, accs), s, pendingCount, billList, (from, sel) ->
        val today = LocalDate.now()
        val thisMonth = YearMonth.from(today)
        val catMap = cats.associateBy { it.id }
        val accMap = accs.associateBy { it.id }
        val month = txns.filter { YearMonth.from(it.date) == thisMonth }
        val first = ledger.firstDate()
        HomeUiState(
            today = today,
            weekDots = LedgerMath.weekDots(today, txns.map { it.date }.toSet()),
            monthExpense = month.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount },
            monthIncome = month.filter { it.type == TxnType.INCOME }.sumOf { it.amount },
            budget = s.monthlyBudget,
            pendingCount = pendingCount,
            nextBill = billList.firstOrNull { !it.paid && !it.due.isBefore(today) } ?: billList.firstOrNull { !it.paid },
            groups = LedgerMath.groupByDay(
                txns.map { it.toRow(context, catMap, accMap) }, { it.date },
                { if (it.type == TxnType.EXPENSE) it.amount else 0L },
            ),
            oldest = from,
            canLoadEarlier = first != null && YearMonth.from(first) < from,
            selection = sel,
            hideAmounts = s.hideAmounts,
            categories = cats.filter { !it.archived },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleHide() = viewModelScope.launch { settings.setHideAmounts(!state.value.hideAmounts) }
    fun loadEarlier() = oldest.update { it.minusMonths(1) }

    fun toggleSelect(id: Long) = selection.update { if (id in it) it - id else it + id }
    fun clearSelection() = selection.update { emptySet() }

    fun deleteSelected() = viewModelScope.launch {
        ledger.deleteTransactions(selection.value.toList())
        clearSelection()
    }

    fun setCategoryForSelected(categoryId: Long) = viewModelScope.launch {
        ledger.setCategory(selection.value.toList(), categoryId)
        clearSelection()
    }
}
