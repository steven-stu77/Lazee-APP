package io.github.steven_stu77.lazee.feature.autolog.listener

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.feature.autolog.AutoLogConfig
import io.github.steven_stu77.lazee.feature.autolog.AutoLogProcessor
import io.github.steven_stu77.lazee.feature.autolog.AutoLogProcessor.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Spec §6 step 1. Reads only title / text / big text, in memory. Never logs or stores the text.
 * For supported apps it records only "when, which app, what happened" so the settings page can
 * show whether auto-log is working.
 */
@AndroidEntryPoint
class LazeeNotificationListener : NotificationListenerService() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var processor: AutoLogProcessor
    @Inject lateinit var config: AutoLogConfig

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onListenerConnected() {
        scope.launch { settings.setListenerConnectedAt(System.currentTimeMillis()) }
    }

    /** Some phones (Xiaomi HyperOS especially) drop the connection; ask to be bound again. */
    override fun onListenerDisconnected() {
        scope.launch { settings.setListenerConnectedAt(0) }
        requestRebind(ComponentName(this, LazeeNotificationListener::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName
        if (pkg == packageName) return // our own bill reminders are not payments
        if (pkg !in config.engine.packages) return // unsupported apps are never looked at
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = n.extras ?: return
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT),
        ).joinToString(" ")
        if (text.isBlank()) return
        val posted = Instant.ofEpochMilli(sbn.postTime)
        scope.launch {
            val s = settings.current()
            val outcome = when {
                !s.autoLogEnabled || pkg !in s.enabledPackages -> Outcome.APP_OFF
                // One bad notification must never take the listener down.
                else -> runCatching { processor.process(pkg, text, posted) }.getOrDefault(Outcome.ERROR)
            }
            settings.setLastSeen(pkg, System.currentTimeMillis(), outcome.name)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Forces Android to re-bind the listener by switching the component off and on. */
        fun reconnect(context: Context) {
            val c = ComponentName(context, LazeeNotificationListener::class.java)
            val pm = context.packageManager
            pm.setComponentEnabledSetting(c, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(c, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            requestRebind(c)
        }
    }
}
