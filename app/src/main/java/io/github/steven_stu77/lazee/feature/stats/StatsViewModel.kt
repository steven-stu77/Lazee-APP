package io.github.steven_stu77.lazee.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.BuiltIns
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TxnSource
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.Spend
import io.github.steven_stu77.lazee.core.logic.StatsMath
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CatShare(val category: Category, val amount: Long, val share: Float)
data class MerchantRow(val name: String, val count: Int, val total: Long)
data class MonthPair(val month: YearMonth, val income: Long, val expense: Long)

data class StatsUiState(
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val canGoNext: Boolean = false,
    val expense: Long = 0,
    val income: Long = 0,
    val savingsRate: Float = 0f,
    val paceThis: List<Long> = emptyList(),
    val paceLast: List<Long> = emptyList(),
    val budget: Long = 0,
    val diffVsLast: Long = 0,
    val forecast: Long = 0,
    val categories: List<CatShare> = emptyList(),
    val merchants: List<MerchantRow> = emptyList(),
    val daily: Map<Int, Long> = emptyMap(),
    val noSpendDays: Int = 0,
    val payShares: List<Pair<String, Float>> = emptyList(),
    val week: StatsMath.WeekSplit = StatsMath.WeekSplit(0, 0),
    val fixed: Long = 0,
    val small: StatsMath.Small = StatsMath.Small(0, 0, 0f),
    val sixMonths: List<MonthPair> = emptyList(),
    val yearOpen: Boolean = false,
) {
    val isCurrentMonth: Boolean get() = month == YearMonth.from(today)
    val overBudget: Boolean get() = budget > 0 && forecast > budget
}

@HiltViewModel
class StatsViewModel @Inject constructor(ledger: LedgerRepository, settings: SettingsRepository) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())

    val state: StateFlow<StatsUiState> = combine(
        month, ledger.observeAllTransactions(), ledger.observeCategories(), ledger.observeAccounts(), settings.settings,
    ) { ym, txns, cats, accs, s -> build(ym, txns, cats, accs, s.monthlyBudget) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun prev() = month.update { it.minusMonths(1) }
    fun next() = month.update { if (it < YearMonth.now()) it.plusMonths(1) else it }

    companion object {
        /** Bills category (and its children) or anything logged from a bill counts as fixed spending. */
        fun spends(txns: List<Transaction>, cats: Map<Long, Category>): List<Spend> {
            val billsIds = cats.values.filter { it.builtInKey == BuiltIns.BILLS }.map { it.id }.toSet()
            return txns.filter { it.type == TxnType.EXPENSE }.map { t ->
                val c = cats[t.categoryId]
                val fixed = t.source == TxnSource.BILL || c?.id in billsIds || c?.parentId in billsIds
                Spend(t.date, t.amount, fixed, t.accountId)
            }
        }

        /** Expense totals per top-level category (sub-categories roll up). */
        fun byTopCategory(txns: List<Transaction>, cats: Map<Long, Category>): Map<Category, Long> =
            txns.filter { it.type == TxnType.EXPENSE }.groupBy { t -> cats[t.categoryId]?.let { c -> c.parentId?.let(cats::get) ?: c } }
                .filterKeys { it != null }.mapKeys { it.key!! }.mapValues { (_, l) -> l.sumOf { it.amount } }
    }

    private fun build(ym: YearMonth, txns: List<Transaction>, catList: List<Category>, accs: List<Account>, budget: Long): StatsUiState {
        val today = LocalDate.now()
        val cats = catList.associateBy { it.id }
        val inMonth = txns.filter { YearMonth.from(it.date) == ym }
        val spends = spends(txns, cats)
        val expense = inMonth.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }
        val income = inMonth.filter { it.type == TxnType.INCOME }.sumOf { it.amount }
        val current = ym == YearMonth.from(today)
        val day = if (current) today.dayOfMonth else ym.lengthOfMonth()
        val last = ym.minusMonths(1)
        val paceThis = StatsMath.cumulative(spends, ym, day)
        val paceLast = StatsMath.cumulative(spends, last)
        val lastSameDay = paceLast.getOrElse((day - 1).coerceAtMost(paceLast.size - 1)) { 0 }
        val byCat = byTopCategory(inMonth, cats)
        val catTotal = byCat.values.filter { it > 0 }.sum()
        val monthSpends = spends.filter { YearMonth.from(it.date) == ym }
        val accNames = accs.associate { it.id to it.name }
        return StatsUiState(
            month = ym,
            today = today,
            canGoNext = ym < YearMonth.from(today),
            expense = expense,
            income = income,
            savingsRate = StatsMath.savingsRate(income, expense),
            paceThis = paceThis,
            paceLast = paceLast,
            budget = budget,
            diffVsLast = (paceThis.lastOrNull() ?: 0) - lastSameDay,
            forecast = if (current) StatsMath.forecast(spends, ym, today) else expense,
            categories = byCat.filter { it.value > 0 }.entries.sortedByDescending { it.value }
                .map { CatShare(it.key, it.value, if (catTotal > 0) it.value.toFloat() / catTotal else 0f) },
            merchants = inMonth.filter { it.type == TxnType.EXPENSE && !it.merchant.isNullOrBlank() }
                .groupBy { it.merchant!!.trim().uppercase() }
                .map { (_, l) -> MerchantRow(l.first().merchant!!.trim(), l.size, l.sumOf { it.amount }) }
                .sortedByDescending { it.total }.take(5),
            daily = StatsMath.dailyTotals(spends, ym),
            noSpendDays = StatsMath.noSpendDays(spends, ym, today),
            payShares = StatsMath.shares(monthSpends.groupBy { it.accountId }.mapValues { (_, l) -> l.sumOf { it.amount } })
                .map { (id, share) -> (accNames[id] ?: "?") to share },
            week = StatsMath.weekdayWeekend(spends, ym, today),
            fixed = monthSpends.filter { it.fixed }.sumOf { it.amount },
            small = StatsMath.smallSpends(spends, budget, ym),
            sixMonths = (5 downTo 0).map { ym.minusMonths(it.toLong()) }.map { m ->
                val l = txns.filter { YearMonth.from(it.date) == m }
                MonthPair(m, l.filter { it.type == TxnType.INCOME }.sumOf { it.amount }, l.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount })
            },
            // The year review opens on 31 December; past years are always open.
            yearOpen = !today.isBefore(LocalDate.of(ym.year, 12, 31)),
        )
    }
}
