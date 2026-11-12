package io.github.steven_stu77.lazee.feature.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class YearUiState(
    val year: Int = LocalDate.now().year,
    val income: Long = 0,
    val expense: Long = 0,
    val months: List<MonthPair> = emptyList(),
    val topCategories: List<CatShare> = emptyList(),
    val topMerchants: List<MerchantRow> = emptyList(),
) {
    val saved: Long get() = income - expense
}

/** 年度回顾 (spec §5.4 #8). */
@HiltViewModel
class YearViewModel @Inject constructor(handle: SavedStateHandle, ledger: LedgerRepository) : ViewModel() {
    private val year = (handle.get<Int>("year") ?: 0).takeIf { it > 0 } ?: LocalDate.now().year

    val state: StateFlow<YearUiState> = combine(ledger.observeAllTransactions(), ledger.observeCategories()) { all, cats ->
        val txns = all.filter { it.date.year == year }
        val catMap = cats.associateBy { it.id }
        val exp = txns.filter { it.type == TxnType.EXPENSE }
        val byCat = StatsViewModel.byTopCategory(txns, catMap).filter { it.value > 0 }
        val catTotal = byCat.values.sum()
        YearUiState(
            year = year,
            income = txns.filter { it.type == TxnType.INCOME }.sumOf { it.amount },
            expense = exp.sumOf { it.amount },
            months = (1..12).map { m ->
                val ym = YearMonth.of(year, m)
                val l = txns.filter { YearMonth.from(it.date) == ym }
                MonthPair(ym, l.filter { it.type == TxnType.INCOME }.sumOf { it.amount }, l.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount })
            },
            topCategories = byCat.entries.sortedByDescending { it.value }.take(5).map { CatShare(it.key, it.value, if (catTotal > 0) it.value.toFloat() / catTotal else 0f) },
            topMerchants = exp.filter { !it.merchant.isNullOrBlank() }.groupBy { it.merchant!!.trim().uppercase() }
                .map { (_, l) -> MerchantRow(l.first().merchant!!.trim(), l.size, l.sumOf { it.amount }) }.sortedByDescending { it.total }.take(5),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearUiState())
}
