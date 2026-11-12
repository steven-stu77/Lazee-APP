package io.github.steven_stu77.lazee.feature.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.CategoryKind
import io.github.steven_stu77.lazee.core.data.SavingGoal
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.BudgetMath
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.BudgetRepository
import io.github.steven_stu77.lazee.core.repo.GoalView
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class CategoryBudgetRow(val category: Category, val limit: Long, val used: Long)

data class GoalRow(val view: GoalView, val perMonth: Long?) {
    val goal: SavingGoal get() = view.goal
}

data class BudgetUiState(
    val total: Long = 0,
    val spent: Long = 0,
    val daysLeft: Int = 0,
    val perDay: Long = 0,
    val rows: List<CategoryBudgetRow> = emptyList(),
    val unbudgeted: List<Category> = emptyList(),
    val goals: List<GoalRow> = emptyList(),
) {
    val left: Long get() = total - spent
}

@HiltViewModel
class BudgetViewModel @Inject constructor(
    ledger: LedgerRepository,
    private val budgets: BudgetRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<BudgetUiState> = combine(
        ledger.observeMonth(YearMonth.now()), ledger.observeCategories(), budgets.observeBudgets(), budgets.observeGoals(), settings.settings,
    ) { txns, cats, bs, goals, s ->
        val today = LocalDate.now()
        val expenses = txns.filter { it.type == TxnType.EXPENSE }
        val spent = expenses.sumOf { it.amount }
        val catMap = cats.associateBy { it.id }
        // A parent's budget counts its sub-categories too.
        fun usedBy(id: Long) = expenses.filter { it.categoryId == id || catMap[it.categoryId]?.parentId == id }.sumOf { it.amount }
        val rows = bs.mapNotNull { b -> catMap[b.categoryId]?.let { CategoryBudgetRow(it, b.amountLimit, usedBy(it.id)) } }
        BudgetUiState(
            total = s.monthlyBudget,
            spent = spent,
            daysLeft = BudgetMath.daysLeft(today),
            perDay = BudgetMath.perDay(s.monthlyBudget, spent, today),
            rows = rows,
            unbudgeted = cats.filter { c -> c.kind == CategoryKind.EXPENSE && c.parentId == null && !c.archived && rows.none { it.category.id == c.id } },
            goals = goals.map { g -> GoalRow(g, perMonth(g, today)) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetUiState())

    /** "每月还要存 RM x": what is left ÷ whole months until the deadline (at least 1). */
    private fun perMonth(g: GoalView, today: LocalDate): Long? {
        val deadline = g.goal.deadline ?: return null
        val left = g.goal.target - g.saved
        if (left <= 0) return 0
        val months = ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(deadline)).coerceAtLeast(1)
        return Math.round(left.toDouble() / months)
    }

    fun setTotal(cents: Long) = viewModelScope.launch { settings.setMonthlyBudget(cents.coerceAtLeast(0)) }
    fun setCategoryBudget(categoryId: Long, cents: Long) = viewModelScope.launch { budgets.setBudget(categoryId, cents) }
    fun saveGoal(g: SavingGoal) = viewModelScope.launch { budgets.saveGoal(g) }
    fun contribute(goalId: Long, cents: Long) = viewModelScope.launch { budgets.contribute(goalId, cents) }
    fun archiveGoal(g: SavingGoal) = viewModelScope.launch { budgets.saveGoal(g.copy(archived = true)) }
}
