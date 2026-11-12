package io.github.steven_stu77.lazee

import javax.inject.Inject
import javax.inject.Singleton

/** Work done once per process start: notification channels, periodic jobs. Filled in by later milestones. */
@Singleton
class AppStartup @Inject constructor() {
    fun run() {}
}
