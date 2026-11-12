package io.github.steven_stu77.lazee.feature.stats

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.core.logic.LedgerMath
import io.github.steven_stu77.lazee.core.repo.BudgetRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.feature.home.TxnRow
import io.github.steven_stu77.lazee.feature.home.toRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class CategoryDetailUiState(
    val category: Category? = null,
    val month: YearMonth = YearMonth.now(),
    val amount: Long = 0,
    val share: Float = 0f,
    val budget: Long = 0,
    val sixMonths: List<MonthPair> = emptyList(),
    val groups: List<LedgerMath.DayGroup<TxnRow>> = emptyList(),
)

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    ledger: LedgerRepository,
    budgets: BudgetRepository,
) : ViewModel() {
    private val id = handle.get<Long>("id") ?: 0L
    private val month = (handle.get<Int>("ym") ?: 0).takeIf { it > 0 }?.let(BillMath::ym) ?: YearMonth.now()

    val state: StateFlow<CategoryDetailUiState> = combine(
        ledger.observeAllTransactions(), ledger.observeCategories(), ledger.observeAccounts(), budgets.observeBudgets(),
    ) { txns, cats, accs, bs ->
        val catMap = cats.associateBy { it.id }
        // The category itself plus its sub-categories.
        val ids = cats.filter { it.id == id || it.parentId == id }.map { it.id }.toSet()
        val mine = txns.filter { it.type == TxnType.EXPENSE && it.categoryId in ids }
        val inMonth = mine.filter { YearMonth.from(it.date) == month }
        val monthTotal = txns.filter { it.type == TxnType.EXPENSE && YearMonth.from(it.date) == month }.sumOf { it.amount }
        val amount = inMonth.sumOf { it.amount }
        CategoryDetailUiState(
            category = catMap[id],
            month = month,
            amount = amount,
            share = if (monthTotal > 0) amount.toFloat() / monthTotal else 0f,
            budget = bs.firstOrNull { it.categoryId == id }?.amountLimit ?: 0,
            sixMonths = (5 downTo 0).map { month.minusMonths(it.toLong()) }.map { m -> MonthPair(m, 0, mine.filter { YearMonth.from(it.date) == m }.sumOf { it.amount }) },
            groups = LedgerMath.groupByDay(inMonth.map { it.toRow(context, catMap, accs.associateBy { a -> a.id }) }, { it.date }, { it.amount }),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryDetailUiState())
}
