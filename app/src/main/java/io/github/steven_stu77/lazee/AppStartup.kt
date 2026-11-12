package io.github.steven_stu77.lazee

import io.github.steven_stu77.lazee.core.notify.Notifier
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Once per process: notification channels and periodic jobs. */
@Singleton
class AppStartup @Inject constructor(
    private val notifier: Notifier,
    private val scheduler: WorkScheduler,
    private val settings: SettingsRepository,
) {
    fun run() {
        notifier.createChannels()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { scheduler.scheduleAll(settings.current()) }
    }
}
