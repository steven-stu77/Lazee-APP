package io.github.steven_stu77.lazee.feature.autolog.listener

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.feature.autolog.AutoLogProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Spec §6 step 1. Reads only title / text / big text, in memory. Never logs or stores the text.
 */
@AndroidEntryPoint
class LazeeNotificationListener : NotificationListenerService() {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var processor: AutoLogProcessor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return // our own bill reminders are not payments
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = n.extras ?: return
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT),
        ).joinToString(" ")
        if (text.isBlank()) return
        val pkg = sbn.packageName
        val posted = Instant.ofEpochMilli(sbn.postTime)
        scope.launch {
            val s = settings.current()
            if (!s.autoLogEnabled || pkg !in s.enabledPackages) return@launch
            processor.process(pkg, text, posted)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
