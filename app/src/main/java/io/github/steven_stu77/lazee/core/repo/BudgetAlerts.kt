package io.github.steven_stu77.lazee.core.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.BudgetMath
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.notify.Notifier
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.nav.Routes
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** After each new expense: alert once per month per budget at 80% and at 100% (spec §5.3). */
@Singleton
class BudgetAlerts @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val notifier: Notifier,
) {
    suspend fun check(date: LocalDate) {
        val s = settings.current()
        if (!s.budgetAlerts) return
        val ym = YearMonth.from(date)
        if (ym != YearMonth.now()) return
        val month = db.transactions().range(ym.atDay(1), ym.atEndOfMonth()).filter { it.type == TxnType.EXPENSE }
        val key = ym.year * 100 + ym.monthValue
        val categories = db.categories().all().associateBy { it.id }

        val checks = mutableListOf<Triple<Long, String, Pair<Long, Long>>>()
        if (s.monthlyBudget > 0) checks += Triple(0L, context.getString(R.string.budget_total), month.sumOf { it.amount } to s.monthlyBudget)
        for (b in db.budgets().all()) {
            // A parent category's budget also counts its sub-categories.
            val spent = month.filter { it.categoryId == b.categoryId || categories[it.categoryId]?.parentId == b.categoryId }.sumOf { it.amount }
            checks += Triple(b.categoryId, Labels.categoryName(context, categories[b.categoryId]), spent to b.amountLimit)
        }

        for ((id, name, sl) in checks) {
            val level = BudgetMath.levelsReached(sl.first, sl.second).maxOrNull() ?: continue
            val tag = "$key:$id:$level"
            if (tag in s.budgetAlertsSent) continue
            settings.addBudgetAlertSent(tag)
            if (level == 100) settings.addBudgetAlertSent("$key:$id:80")
            val text = if (level == 100) context.getString(R.string.budget_alert_over, name, Money.format(sl.first - sl.second))
            else context.getString(R.string.budget_alert_80, name, Money.format(sl.second - sl.first))
            notifier.post(
                Notifier.ID_BUDGET + id.toInt() % 1000,
                notifier.builder(Notifier.Channel.BUDGET, context.getString(R.string.budget_alert_title), text, context.getString(R.string.budget_alert_public))
                    .setContentIntent(notifier.openApp(Routes.BUDGET, 2)),
            )
        }
    }
}
