package io.github.steven_stu77.lazee.work

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.notify.Notifier
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.feature.autolog.listener.LazeeNotificationListener
import io.github.steven_stu77.lazee.nav.Routes

/**
 * Every 15 minutes while auto-log is on: make sure the notification listener is bound
 * (phones like Xiaomi drop it), and tell the user if the permission was taken away.
 */
@HiltWorker
class ListenerHealthWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: SettingsRepository,
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val s = settings.current()
        if (!s.autoLogEnabled) return Result.success()
        val ctx = applicationContext
        val now = System.currentTimeMillis()
        settings.setHealthCheckAt(now)
        val granted = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
        if (!granted) {
            // At most twice a day, so it reminds without nagging.
            if (now - settings.healthWarnedAt() > 12 * 3_600_000L) {
                settings.setHealthWarnedAt(now)
                notifier.post(
                    Notifier.ID_HEALTH,
                    notifier.builder(Notifier.Channel.AUTOLOG, ctx.getString(R.string.hc_lost_title), ctx.getString(R.string.hc_lost_body))
                        .setContentIntent(notifier.openApp(Routes.AUTOLOG, Notifier.ID_HEALTH)),
                )
            }
            return Result.success()
        }
        // Cheap and harmless when already bound; brings the listener back when the system dropped it.
        NotificationListenerService.requestRebind(ComponentName(ctx, LazeeNotificationListener::class.java))
        return Result.success()
    }

    companion object { const val NAME = "listener-health" }
}
