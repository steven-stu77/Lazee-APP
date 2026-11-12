package io.github.steven_stu77.lazee.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.StatsMath
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.notify.Notifier
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.repo.PendingRepository
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.feature.stats.StatsViewModel
import io.github.steven_stu77.lazee.nav.Routes
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/** Nightly: "Today: N records, RM x spent, M to confirm" — or a nudge when nothing was logged. */
@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ledger: LedgerRepository,
    private val pending: PendingRepository,
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val today = LocalDate.now()
        val txns = ledger.range(today, today)
        val waiting = pending.observeCount().first()
        val text = if (txns.isEmpty() && waiting == 0) ctx.getString(R.string.wr_daily_none)
        else ctx.getString(R.string.wr_daily, txns.size, Money.format(txns.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }), waiting)
        notifier.post(
            Notifier.ID_DAILY,
            notifier.builder(Notifier.Channel.SUMMARY, ctx.getString(R.string.wr_daily_title), text, ctx.getString(R.string.wr_daily_public))
                .setContentIntent(notifier.openApp(if (waiting > 0) Routes.PENDING else null, Notifier.ID_DAILY)),
        )
        return Result.success()
    }

    companion object { const val NAME = "daily-summary" }
}

/** On the 1st: "October: spent RM x, saved y%, most on Food". Runs daily, acts only on day 1. */
@HiltWorker
class MonthlySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val ledger: LedgerRepository,
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        if (today.dayOfMonth != 1) return Result.success()
        val ctx = applicationContext
        val last = YearMonth.from(today).minusMonths(1)
        val txns = ledger.range(last.atDay(1), last.atEndOfMonth())
        if (txns.isEmpty()) return Result.success()
        val spent = txns.filter { it.type == TxnType.EXPENSE }.sumOf { it.amount }
        val income = txns.filter { it.type == TxnType.INCOME }.sumOf { it.amount }
        val cats = ledger.allCategories().associateBy { it.id }
        val top = StatsViewModel.byTopCategory(txns, cats).maxByOrNull { it.value }?.key
        val text = ctx.getString(
            R.string.wr_monthly, Labels.monthShort(ctx, last), Money.format(spent),
            (StatsMath.savingsRate(income, spent) * 100).roundToInt(), Labels.categoryName(ctx, top),
        )
        notifier.post(
            Notifier.ID_MONTHLY,
            notifier.builder(Notifier.Channel.SUMMARY, ctx.getString(R.string.wr_monthly_title), text, ctx.getString(R.string.wr_monthly_public))
                .setContentIntent(notifier.openApp(Routes.STATS, Notifier.ID_MONTHLY)),
        )
        return Result.success()
    }

    companion object { const val NAME = "monthly-summary" }
}
