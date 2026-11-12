package io.github.steven_stu77.lazee.core.repo

import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.CategoryBudget
import io.github.steven_stu77.lazee.core.data.GoalContribution
import io.github.steven_stu77.lazee.core.data.SavingGoal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class GoalView(val goal: SavingGoal, val saved: Long)

/** Category budgets and saving goals. The monthly total budget lives in DataStore. */
@Singleton
class BudgetRepository @Inject constructor(db: AppDatabase) {
    private val budgets = db.budgets()
    private val goals = db.goals()

    fun observeBudgets(): Flow<List<CategoryBudget>> = budgets.observeAll()
    suspend fun setBudget(categoryId: Long, limit: Long) =
        if (limit <= 0) budgets.delete(categoryId) else budgets.upsert(
            CategoryBudget(id = budgets.all().firstOrNull { it.categoryId == categoryId }?.id ?: 0, categoryId = categoryId, amountLimit = limit),
        )

    fun observeGoals(): Flow<List<GoalView>> = combine(goals.observeGoals(), goals.observeContributions()) { g, c ->
        g.map { goal -> GoalView(goal, c.filter { it.goalId == goal.id }.sumOf { it.amount }) }
    }

    suspend fun saveGoal(g: SavingGoal): Long = if (g.id == 0L) goals.insert(g) else { goals.update(g); g.id }

    /** Deposits are positive, withdrawals negative. Not an expense — only a record. */
    suspend fun contribute(goalId: Long, amount: Long) = goals.contribute(GoalContribution(goalId = goalId, amount = amount, date = LocalDate.now()))
}
