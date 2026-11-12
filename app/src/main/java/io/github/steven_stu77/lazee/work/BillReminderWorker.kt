package io.github.steven_stu77.lazee.work

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.BillMode
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.notify.Notifier
import io.github.steven_stu77.lazee.core.repo.BillRepository
import io.github.steven_stu77.lazee.core.repo.BillStatus
import io.github.steven_stu77.lazee.nav.Routes
import java.time.LocalDate
import java.time.YearMonth

/** Daily 09:00: reminders, auto-recorded bills, finished instalments (spec §7). */
@HiltWorker
class BillReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val bills: BillRepository,
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val month = YearMonth.from(today)
        val snoozed = inputData.getLong(KEY_SNOOZED_BILL, 0)
        val statuses = bills.statuses(dbBills(), dbPayments(month), month).filter { !it.paid }
        for (s in statuses) {
            val b = s.bill
            if (snoozed > 0) { if (b.id == snoozed) remind(s, today); continue }
            if (b.mode == BillMode.AUTO_RECORD && today == s.due) {
                bills.markPaid(b.id, month, auto = true)
                continue
            }
            if (BillMath.shouldRemind(today, b.dayOfMonth, b.remindDaysBefore)) remind(s, today)
        }
        if (snoozed == 0L) bills.retireFinished(month)
        return Result.success()
    }

    private suspend fun dbBills() = bills.allBills()
    private suspend fun dbPayments(month: YearMonth) = bills.payments(month)

    private fun remind(s: BillStatus, today: LocalDate) {
        val ctx = applicationContext
        val days = BillMath.daysUntil(today, s.due)
        val title = if (days <= 0) ctx.getString(R.string.wr_bill_today, s.bill.name) else ctx.getString(R.string.wr_bill_soon, s.bill.name, days)
        val id = Notifier.ID_BILL_BASE + s.bill.id.toInt() % 10_000
        fun action(a: String, code: Int) = PendingIntent.getBroadcast(
            ctx, code, Intent(ctx, BillActionReceiver::class.java).setAction(a).putExtra(BillActionReceiver.EXTRA_ID, s.bill.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        notifier.post(
            id,
            notifier.builder(Notifier.Channel.BILLS, title, Money.format(s.bill.amount), ctx.getString(R.string.wr_bill_public))
                .setContentIntent(notifier.openApp(Routes.BILLS, id))
                .addAction(0, ctx.getString(R.string.wr_paid), action(BillActionReceiver.PAID, id))
                .addAction(0, ctx.getString(R.string.wr_tomorrow), action(BillActionReceiver.SNOOZE, id + 1)),
        )
    }

    companion object {
        const val NAME = "bill-reminders"
        const val KEY_SNOOZED_BILL = "snoozedBill"
    }
}
