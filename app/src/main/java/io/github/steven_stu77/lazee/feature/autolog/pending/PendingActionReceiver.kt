package io.github.steven_stu77.lazee.feature.autolog.pending

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.steven_stu77.lazee.feature.autolog.AutoLogNotifications
import io.github.steven_stu77.lazee.feature.autolog.AutoLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Handles the notification buttons, so "Confirm" logs it without opening the app. */
@AndroidEntryPoint
class PendingActionReceiver : BroadcastReceiver() {
    @Inject lateinit var repo: AutoLogRepository
    @Inject lateinit var notifications: AutoLogNotifications

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, 0)
        if (id == 0L) return
        val done = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    CONFIRM -> { repo.confirm(id, null); notifications.cancelPending(id) }
                    IGNORE -> { repo.ignore(id); notifications.cancelPending(id) }
                    UNDO -> { repo.undoAuto(id); notifications.cancelAuto(id) }
                }
            } finally {
                done.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val CONFIRM = "io.github.steven_stu77.lazee.CONFIRM"
        const val IGNORE = "io.github.steven_stu77.lazee.IGNORE"
        const val UNDO = "io.github.steven_stu77.lazee.UNDO"
    }
}
