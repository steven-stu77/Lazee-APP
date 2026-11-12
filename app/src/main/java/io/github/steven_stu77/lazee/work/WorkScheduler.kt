package io.github.steven_stu77.lazee.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.core.prefs.BackupFrequency
import io.github.steven_stu77.lazee.core.prefs.Settings
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** All periodic jobs (spec §7). Times are the phone's local time; no network constraints. */
@Singleton
class WorkScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val wm get() = WorkManager.getInstance(context)

    /** Delay until the next [time] today or tomorrow. */
    fun delayUntil(time: LocalTime, now: LocalDateTime = LocalDateTime.now()): Duration {
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    fun scheduleAll(s: Settings) {
        daily<BillReminderWorker>(BillReminderWorker.NAME, LocalTime.of(9, 0), ExistingPeriodicWorkPolicy.KEEP)
        daily<MonthlySummaryWorker>(MonthlySummaryWorker.NAME, LocalTime.of(9, 5), ExistingPeriodicWorkPolicy.KEEP)
        scheduleNightly(s, ExistingPeriodicWorkPolicy.KEEP)
        scheduleBackup(s, ExistingPeriodicWorkPolicy.KEEP)
    }

    fun scheduleNightly(s: Settings, policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE) {
        if (!s.nightlySummary) { wm.cancelUniqueWork(DailySummaryWorker.NAME); return }
        daily<DailySummaryWorker>(DailySummaryWorker.NAME, LocalTime.of(s.nightlySummaryMinutes / 60, s.nightlySummaryMinutes % 60), policy)
    }

    fun scheduleBackup(s: Settings, policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE) {
        if (!s.autoBackup || s.backupTreeUri == null) { wm.cancelUniqueWork(AutoBackupWorker.NAME); return }
        val days = if (s.backupFrequency == BackupFrequency.DAILY) 1L else 7L
        val req = PeriodicWorkRequestBuilder<AutoBackupWorker>(days, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(LocalTime.of(3, 0)).toMinutes(), TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        wm.enqueueUniquePeriodicWork(AutoBackupWorker.NAME, policy, req)
    }

    /** "Remind me tomorrow" on a bill reminder. */
    fun snoozeBill(billId: Long) {
        wm.enqueue(
            OneTimeWorkRequestBuilder<BillReminderWorker>()
                .setInitialDelay(1, TimeUnit.DAYS)
                .setInputData(workDataOf(BillReminderWorker.KEY_SNOOZED_BILL to billId))
                .build(),
        )
    }

    private inline fun <reified W : androidx.work.ListenableWorker> daily(name: String, at: LocalTime, policy: ExistingPeriodicWorkPolicy) {
        val req = PeriodicWorkRequestBuilder<W>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(at).toMinutes(), TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(name, policy, req)
    }
}
