package io.github.steven_stu77.lazee.core.repo

import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.PendingTxn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Notification-parsed records waiting for the user. Write side lives in feature/autolog. */
@Singleton
class PendingRepository @Inject constructor(db: AppDatabase) {
    private val dao = db.pending()
    fun observePending(): Flow<List<PendingTxn>> = dao.observePending()
    fun observeCount(): Flow<Int> = dao.observePending().map { it.size }
}
