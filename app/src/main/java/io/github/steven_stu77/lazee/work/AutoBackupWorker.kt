package io.github.steven_stu77.lazee.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.backup.BackupManager
import io.github.steven_stu77.lazee.core.backup.NoBackupFolderException
import io.github.steven_stu77.lazee.core.notify.Notifier
import io.github.steven_stu77.lazee.nav.Routes

/** Daily or weekly at 03:00 when the battery is not low (spec §7). */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backup: BackupManager,
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        backup.backupNow()
        Result.success()
    } catch (e: NoBackupFolderException) {
        val ctx = applicationContext
        notifier.post(
            Notifier.ID_BACKUP,
            notifier.builder(Notifier.Channel.BACKUP, ctx.getString(R.string.bk_folder_lost_title), ctx.getString(R.string.bk_folder_lost))
                .setContentIntent(notifier.openApp(Routes.BACKUP, Notifier.ID_BACKUP)),
        )
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    companion object { const val NAME = "auto-backup" }
}
