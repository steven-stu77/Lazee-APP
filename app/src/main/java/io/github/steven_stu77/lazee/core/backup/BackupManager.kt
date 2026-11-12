package io.github.steven_stu77.lazee.core.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.prefs.Settings
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.prefs.ThemeKey
import io.github.steven_stu77.lazee.core.repo.PhotoStore
import io.github.steven_stu77.lazee.core.security.KeystoreCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

class NoBackupFolderException : Exception("Backup folder missing or no longer accessible")
class NoBackupPasswordException : Exception("No backup password set")

/** Encrypted .lgb backups to a user-picked folder (spec §5.13). The app itself never goes online. */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val photos: PhotoStore,
) {
    /** Saves the password wrapped by the Keystore so automatic backups can use it. */
    suspend fun setPassword(password: CharArray) = settings.setBackupPasswordEncrypted(KeystoreCipher.encrypt(String(password).toByteArray()))

    private suspend fun storedPassword(): CharArray =
        settings.current().backupPasswordEncrypted?.let { String(KeystoreCipher.decrypt(it)).toCharArray() } ?: throw NoBackupPasswordException()

    suspend fun snapshot(): BackupData = db.withTransaction {
        val s = settings.current()
        BackupData(
            createdAt = System.currentTimeMillis(),
            accounts = db.accounts().all(), categories = db.categories().all(), transactions = db.transactions().all(),
            tags = db.tags().all(), transactionTags = db.tags().allLinks(), templates = db.templates().all(),
            budgets = db.budgets().all(), goals = db.goals().allGoals(), contributions = db.goals().allContributions(),
            bills = db.bills().all(), billPayments = db.bills().allPayments(), loans = db.loans().all(),
            repayments = db.loans().allRepayments(), pending = db.pending().all(), merchantRules = db.merchantRules().all(),
            settings = BackupSettings(
                s.language, s.theme.name, s.monthlyBudget, s.autoLogEnabled, s.enabledPackages, s.lazyQuickConfirm,
                s.lazyAutoFamiliar, s.nightlySummary, s.nightlySummaryMinutes, s.budgetAlerts, s.packageAccounts,
            ),
        )
    }

    private fun photoBytes(): Map<String, ByteArray> = photos.dir.listFiles().orEmpty().filter { it.isFile }.associate { it.name to it.readBytes() }

    suspend fun encryptedBytes(password: CharArray): ByteArray = withContext(Dispatchers.IO) {
        BackupCrypto.encrypt(BackupFormat.zip(snapshot(), photoBytes()), password)
    }

    fun fileName(now: LocalDateTime = LocalDateTime.now()) = "lazee-backup-${now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"))}.lgb"

    /** Writes a backup into the chosen folder and keeps only the newest 10. Returns the file name. */
    suspend fun backupNow(): String = withContext(Dispatchers.IO) {
        val s = settings.current()
        val tree = s.backupTreeUri?.let { DocumentFile.fromTreeUri(context, Uri.parse(it)) }
        if (tree == null || !tree.canWrite()) throw NoBackupFolderException()
        val bytes = encryptedBytes(storedPassword())
        val name = fileName()
        tree.findFile(name)?.delete()
        val doc = tree.createFile("application/octet-stream", name) ?: throw NoBackupFolderException()
        context.contentResolver.openOutputStream(doc.uri)?.use { it.write(bytes) } ?: throw NoBackupFolderException()
        tree.listFiles().filter { it.name?.startsWith("lazee-backup-") == true }.sortedByDescending { it.name }.drop(KEEP).forEach { it.delete() }
        settings.setLastBackupAt(System.currentTimeMillis())
        name
    }

    /** Checks the password and reads the file without changing anything. */
    suspend fun read(uri: Uri, password: CharArray): Pair<BackupData, Map<String, ByteArray>> = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw NotABackupException()
        BackupFormat.unzip(BackupCrypto.decrypt(bytes, password))
    }

    /** Replaces all data. A safety copy of the current data is saved first (same password). */
    suspend fun restore(uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val (data, photoFiles) = read(uri, password)
        File(context.filesDir, "before-restore.lgb").writeBytes(encryptedBytes(password))
        runCatching { if (settings.current().backupTreeUri != null && settings.current().backupPasswordEncrypted != null) backupNow() }
        apply(data, photoFiles)
    }

    /** Replaces every table, the photos and the user settings with [data]. */
    suspend fun apply(data: BackupData, photoFiles: Map<String, ByteArray>) {
        db.withTransaction {
            db.clearAllTables()
            val r = db.restore()
            r.accounts(data.accounts); r.categories(data.categories); r.transactions(data.transactions)
            r.tags(data.tags); r.transactionTags(data.transactionTags); r.templates(data.templates)
            r.budgets(data.budgets); r.goals(data.goals); r.contributions(data.contributions)
            r.bills(data.bills); r.billPayments(data.billPayments); r.loans(data.loans); r.repayments(data.repayments)
            r.pending(data.pending); r.merchantRules(data.merchantRules)
        }
        photos.dir.listFiles().orEmpty().forEach { it.delete() }
        photoFiles.forEach { (name, bytes) -> File(photos.dir, File(name).name).writeBytes(bytes) }
        val b = data.settings
        settings.restore(
            Settings(
                language = b.language, theme = runCatching { ThemeKey.valueOf(b.theme) }.getOrDefault(ThemeKey.green),
                monthlyBudget = b.monthlyBudget, autoLogEnabled = b.autoLogEnabled, enabledPackages = b.enabledPackages,
                lazyQuickConfirm = b.lazyQuickConfirm, lazyAutoFamiliar = b.lazyAutoFamiliar, nightlySummary = b.nightlySummary,
                nightlySummaryMinutes = b.nightlySummaryMinutes, budgetAlerts = b.budgetAlerts, packageAccounts = b.packageAccounts,
            ),
        )
    }

    companion object { const val KEEP = 10 }
}
