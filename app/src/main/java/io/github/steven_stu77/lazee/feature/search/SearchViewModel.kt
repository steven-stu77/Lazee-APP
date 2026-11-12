package io.github.steven_stu77.lazee.feature.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.LedgerMath
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.feature.home.TxnRow
import io.github.steven_stu77.lazee.feature.home.toRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

data class SearchFilter(
    val query: String = "",
    val type: TxnType? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val minAmount: String = "",
    val maxAmount: String = "",
    val autoOnly: Boolean = false,
) {
    val active: Boolean get() = this != SearchFilter(query = query)
}

data class SearchUiState(
    val filter: SearchFilter = SearchFilter(),
    val groups: List<LedgerMath.DayGroup<TxnRow>> = emptyList(),
    val count: Int = 0,
    val totalExpense: Long = 0,
    val totalIncome: Long = 0,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    ledger: LedgerRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(SearchFilter())

    val state: StateFlow<SearchUiState> = combine(
        filter, ledger.observeAllTransactions(), ledger.observeCategories(), ledger.observeAccounts(), ledger.observeTagNames(),
    ) { f, txns, cats, accs, tags ->
        val catMap = cats.associateBy { it.id }
        val accMap = accs.associateBy { it.id }
        val hits = txns.filter { matches(it, f, catMap, tags[it.id].orEmpty()) }
        SearchUiState(
            filter = f,
            groups = LedgerMath.groupByDay(hits.map { it.toRow(context, catMap, accMap) }, { it.date }, { if (it.type == TxnType.EXPENSE) it.amount else 0L }),
            count = hits.size,
            totalExpense = hits.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount },
            totalIncome = hits.filter { it.type == TxnType.INCOME }.sumOf { it.amount },
            categories = cats.filter { !it.archived },
            accounts = accs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    private fun matches(t: Transaction, f: SearchFilter, cats: Map<Long, Category>, tags: List<String>): Boolean {
        if (f.query.isNotBlank()) {
            val q = f.query.trim().removePrefix("#").lowercase()
            val hay = listOfNotNull(t.note, t.merchant).map { it.lowercase() } + tags.map { it.lowercase() }
            if (hay.none { it.contains(q) }) return false
        }
        if (f.type != null && t.type != f.type) return false
        // A parent category also matches its sub-categories.
        if (f.categoryId != null && t.categoryId != f.categoryId && cats[t.categoryId]?.parentId != f.categoryId) return false
        if (f.accountId != null && t.accountId != f.accountId && t.toAccountId != f.accountId) return false
        if (f.from != null && t.date < f.from) return false
        if (f.to != null && t.date > f.to) return false
        Money.parse(f.minAmount)?.let { if (t.amount < it) return false }
        Money.parse(f.maxAmount)?.let { if (t.amount > it) return false }
        if (f.autoOnly && !t.isAuto) return false
        return f.query.isNotBlank() || f.active
    }

    fun update(block: (SearchFilter) -> SearchFilter) = filter.update(block)
    fun clear() = filter.update { SearchFilter(query = it.query) }
}
