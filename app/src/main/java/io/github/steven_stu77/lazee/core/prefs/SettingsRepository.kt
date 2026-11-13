package io.github.steven_stu77.lazee.core.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {
    private val store = context.dataStore

    private object K {
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val hideAmounts = booleanPreferencesKey("hideAmounts")
        val appLock = booleanPreferencesKey("appLockEnabled")
        val monthlyBudget = longPreferencesKey("monthlyBudget")
        val autoLog = booleanPreferencesKey("autoLogEnabled")
        val packages = stringSetPreferencesKey("enabledPackages")
        val quickConfirm = booleanPreferencesKey("lazyQuickConfirm")
        val autoFamiliar = booleanPreferencesKey("lazyAutoFamiliar")
        val nightly = booleanPreferencesKey("nightlySummary")
        val nightlyTime = intPreferencesKey("nightlySummaryTime")
        val budgetAlerts = booleanPreferencesKey("budgetAlerts")
        val autoBackup = booleanPreferencesKey("autoBackup")
        val backupFreq = stringPreferencesKey("backupFrequency")
        val backupUri = stringPreferencesKey("backupTreeUri")
        val lastBackup = longPreferencesKey("lastBackupAt")
        val onboarding = booleanPreferencesKey("onboardingDone")
        val backupPw = stringPreferencesKey("backupPasswordEncrypted")
        val lastAccount = longPreferencesKey("lastAccountId")
        val alertsSent = stringSetPreferencesKey("budgetAlertsSent")
        val pkgAccounts = stringSetPreferencesKey("packageAccounts")
        val dbPass = stringPreferencesKey("dbPassphraseEncrypted")
        val listenerAt = longPreferencesKey("listenerConnectedAt")
        val lastSeen = stringPreferencesKey("lastNotificationSeen")
    }

    val settings: Flow<Settings> = store.data.map(::toSettings)

    suspend fun current(): Settings = settings.first()

    private fun toSettings(p: Preferences) = Settings(
        language = p[K.language] ?: "",
        theme = p[K.theme]?.let { runCatching { ThemeKey.valueOf(it) }.getOrNull() } ?: ThemeKey.green,
        hideAmounts = p[K.hideAmounts] ?: false,
        appLockEnabled = p[K.appLock] ?: false,
        monthlyBudget = p[K.monthlyBudget] ?: 0,
        autoLogEnabled = p[K.autoLog] ?: false,
        enabledPackages = p[K.packages] ?: emptySet(),
        lazyQuickConfirm = p[K.quickConfirm] ?: true,
        lazyAutoFamiliar = p[K.autoFamiliar] ?: false,
        nightlySummary = p[K.nightly] ?: true,
        nightlySummaryMinutes = p[K.nightlyTime] ?: (21 * 60),
        budgetAlerts = p[K.budgetAlerts] ?: true,
        autoBackup = p[K.autoBackup] ?: false,
        backupFrequency = p[K.backupFreq]?.let { runCatching { BackupFrequency.valueOf(it) }.getOrNull() }
            ?: BackupFrequency.DAILY,
        backupTreeUri = p[K.backupUri],
        lastBackupAt = p[K.lastBackup] ?: 0,
        onboardingDone = p[K.onboarding] ?: false,
        backupPasswordEncrypted = p[K.backupPw],
        lastAccountId = p[K.lastAccount] ?: 0,
        budgetAlertsSent = p[K.alertsSent] ?: emptySet(),
        packageAccounts = p[K.pkgAccounts] ?: emptySet(),
    )

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    suspend fun setLanguage(v: String) = edit { it[K.language] = v }
    suspend fun setTheme(v: ThemeKey) = edit { it[K.theme] = v.name }
    suspend fun setHideAmounts(v: Boolean) = edit { it[K.hideAmounts] = v }
    suspend fun setAppLock(v: Boolean) = edit { it[K.appLock] = v }
    suspend fun setMonthlyBudget(v: Long) = edit { it[K.monthlyBudget] = v }
    suspend fun setAutoLog(v: Boolean) = edit { it[K.autoLog] = v }
    suspend fun setEnabledPackages(v: Set<String>) = edit { it[K.packages] = v }
    suspend fun setQuickConfirm(v: Boolean) = edit { it[K.quickConfirm] = v }
    suspend fun setAutoFamiliar(v: Boolean) = edit { it[K.autoFamiliar] = v }
    suspend fun setNightly(v: Boolean) = edit { it[K.nightly] = v }
    suspend fun setNightlyMinutes(v: Int) = edit { it[K.nightlyTime] = v }
    suspend fun setBudgetAlerts(v: Boolean) = edit { it[K.budgetAlerts] = v }
    suspend fun setAutoBackup(v: Boolean) = edit { it[K.autoBackup] = v }
    suspend fun setBackupFrequency(v: BackupFrequency) = edit { it[K.backupFreq] = v.name }
    suspend fun setBackupTreeUri(v: String?) = edit { if (v == null) it.remove(K.backupUri) else it[K.backupUri] = v }
    suspend fun setLastBackupAt(v: Long) = edit { it[K.lastBackup] = v }
    suspend fun setOnboardingDone(v: Boolean) = edit { it[K.onboarding] = v }
    suspend fun setBackupPasswordEncrypted(v: String?) = edit { if (v == null) it.remove(K.backupPw) else it[K.backupPw] = v }
    suspend fun setLastAccountId(v: Long) = edit { it[K.lastAccount] = v }
    suspend fun addBudgetAlertSent(v: String) = edit { it[K.alertsSent] = (it[K.alertsSent] ?: emptySet()) + v }
    suspend fun setPackageAccount(pkg: String, accountId: Long) = edit {
        val rest = (it[K.pkgAccounts] ?: emptySet()).filterNot { e -> e.startsWith("$pkg=") }.toSet()
        it[K.pkgAccounts] = rest + "$pkg=$accountId"
    }

    /** Auto-log health, no notification text: "package|epochMillis|OUTCOME". */
    val autoLogStatus: Flow<Pair<Long, String?>> = store.data.map { (it[K.listenerAt] ?: 0L) to it[K.lastSeen] }
    suspend fun setListenerConnectedAt(v: Long) = edit { it[K.listenerAt] = v }
    suspend fun setLastSeen(pkg: String, at: Long, outcome: String) = edit { it[K.lastSeen] = "$pkg|$at|$outcome" }

    suspend fun dbPassphraseEncrypted(): String? = store.data.first()[K.dbPass]
    suspend fun setDbPassphraseEncrypted(v: String) = edit { it[K.dbPass] = v }

    /** Used by restore: write every user-facing setting back in one go. */
    suspend fun restore(s: Settings) = edit {
        it[K.language] = s.language
        it[K.theme] = s.theme.name
        it[K.hideAmounts] = s.hideAmounts
        it[K.monthlyBudget] = s.monthlyBudget
        it[K.autoLog] = s.autoLogEnabled
        it[K.packages] = s.enabledPackages
        it[K.quickConfirm] = s.lazyQuickConfirm
        it[K.autoFamiliar] = s.lazyAutoFamiliar
        it[K.nightly] = s.nightlySummary
        it[K.nightlyTime] = s.nightlySummaryMinutes
        it[K.budgetAlerts] = s.budgetAlerts
        it[K.pkgAccounts] = s.packageAccounts
        it[K.onboarding] = true
    }
}
