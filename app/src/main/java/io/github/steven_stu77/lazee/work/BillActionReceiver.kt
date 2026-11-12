package io.github.steven_stu77.lazee.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.steven_stu77.lazee.core.notify.Notifier
import io.github.steven_stu77.lazee.core.repo.BillRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "Paid" and "Remind me tomorrow" buttons on bill reminders. */
@AndroidEntryPoint
class BillActionReceiver : BroadcastReceiver() {
    @Inject lateinit var bills: BillRepository
    @Inject lateinit var notifier: Notifier
    @Inject lateinit var scheduler: WorkScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, 0)
        if (id == 0L) return
        notifier.cancel(Notifier.ID_BILL_BASE + id.toInt() % 10_000)
        val done = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    PAID -> bills.markPaid(id)
                    SNOOZE -> scheduler.snoozeBill(id)
                }
            } finally {
                done.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "billId"
        const val PAID = "io.github.steven_stu77.lazee.BILL_PAID"
        const val SNOOZE = "io.github.steven_stu77.lazee.BILL_SNOOZE"
    }
}
