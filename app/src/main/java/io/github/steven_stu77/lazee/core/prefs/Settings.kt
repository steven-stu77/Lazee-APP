package io.github.steven_stu77.lazee.core.prefs

enum class ThemeKey { green, blue, terra, grape, ink }
enum class BackupFrequency { DAILY, WEEKLY }

/** Snapshot of everything kept in DataStore. Money is in cents. */
data class Settings(
    val language: String = "",
    val theme: ThemeKey = ThemeKey.green,
    val hideAmounts: Boolean = false,
    val appLockEnabled: Boolean = false,
    val monthlyBudget: Long = 0,
    val autoLogEnabled: Boolean = false,
    val enabledPackages: Set<String> = emptySet(),
    val lazyQuickConfirm: Boolean = true,
    val lazyAutoFamiliar: Boolean = false,
    val nightlySummary: Boolean = true,
    val nightlySummaryMinutes: Int = 21 * 60,
    val budgetAlerts: Boolean = true,
    val autoBackup: Boolean = false,
    val backupFrequency: BackupFrequency = BackupFrequency.DAILY,
    val backupTreeUri: String? = null,
    val lastBackupAt: Long = 0,
    val onboardingDone: Boolean = false,
    val backupPasswordEncrypted: String? = null,
    val lastAccountId: Long = 0,
    /** "yyyyMM:<categoryId or 0 for total>:<80|100>" — alerts already sent. */
    val budgetAlertsSent: Set<String> = emptySet(),
    /** "package=accountId" — which account a source app pays from. */
    val packageAccounts: Set<String> = emptySet(),
) {
    fun accountForPackage(pkg: String): Long? =
        packageAccounts.firstOrNull { it.startsWith("$pkg=") }?.substringAfter('=')?.toLongOrNull()
}
