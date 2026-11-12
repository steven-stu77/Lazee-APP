package io.github.steven_stu77.lazee.android

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.steven_stu77.lazee.core.backup.BackupCrypto
import io.github.steven_stu77.lazee.core.backup.BackupFormat
import io.github.steven_stu77.lazee.core.backup.BackupManager
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.repo.PhotoStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import javax.inject.Inject

/** Back up → wipe → restore gives back the same records, settings and photos. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [35])
class BackupRestoreTest {
    @get:Rule val hilt = HiltAndroidRule(this)
    @Inject lateinit var db: AppDatabase
    @Inject lateinit var ledger: LedgerRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var backup: BackupManager
    @Inject lateinit var photos: PhotoStore

    @Before fun setUp() = hilt.inject()

    @Test fun roundTrip() = runBlocking {
        SampleData.load(db, ledger, settings)
        File(photos.dir, "receipt.jpg").writeBytes(byteArrayOf(7, 7, 7))
        val before = backup.snapshot()
        val file = backup.encryptedBytes("hunter22".toCharArray())

        db.clearAllTables()
        photos.dir.listFiles()!!.forEach { it.delete() }
        settings.setMonthlyBudget(0)
        assertThat(ledger.allTransactions()).isEmpty()

        val (data, files) = BackupFormat.unzip(BackupCrypto.decrypt(file, "hunter22".toCharArray()))
        backup.apply(data, files)

        val after = backup.snapshot()
        assertThat(after.copy(createdAt = 0)).isEqualTo(before.copy(createdAt = 0))
        assertThat(File(photos.dir, "receipt.jpg").readBytes()).isEqualTo(byteArrayOf(7, 7, 7))
        assertThat(settings.current().monthlyBudget).isEqualTo(150_000)
    }
}
