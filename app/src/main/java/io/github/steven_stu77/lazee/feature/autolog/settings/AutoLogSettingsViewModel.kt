package io.github.steven_stu77.lazee.feature.autolog.settings

import android.content.Context
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.prefs.Settings
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.feature.autolog.AutoLogConfig
import io.github.steven_stu77.lazee.feature.autolog.AutoLogRepository
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppRow(val config: ParserConfig, val enabled: Boolean, val account: Account?)

data class AutoLogSettingsUiState(
    val settings: Settings = Settings(),
    val listenerGranted: Boolean = false,
    val batteryOk: Boolean = true,
    val apps: List<AppRow> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val connectedAt: Long = 0,
    /** Last notification from a supported app: (app name, time, outcome). Never its text. */
    val lastSeen: Triple<String, Long, String>? = null,
)

@HiltViewModel
class AutoLogSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val config: AutoLogConfig,
    private val repo: AutoLogRepository,
    ledger: LedgerRepository,
) : ViewModel() {
    private val system = MutableStateFlow(false to true)

    val state: StateFlow<AutoLogSettingsUiState> = combine(settings.settings, system, ledger.observeAccounts(), settings.autoLogStatus) { s, (granted, battery), accs, (connected, last) ->
        AutoLogSettingsUiState(
            connectedAt = connected,
            lastSeen = last?.split('|')?.takeIf { it.size == 3 }?.let { (pkg, at, outcome) ->
                Triple(config.engine.config(pkg)?.displayName ?: pkg, at.toLongOrNull() ?: 0, outcome)
            },
            settings = s,
            listenerGranted = granted,
            batteryOk = battery,
            apps = config.parserConfigs.map { c -> AppRow(c, c.`package` in s.enabledPackages, repo.accountFor(c.`package`)) },
            accounts = accs.filter { !it.archived },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AutoLogSettingsUiState())

    /** Re-checked every time the page resumes (the user comes back from system settings). */
    fun refreshSystemState() {
        val granted = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        val battery = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
        system.value = granted to battery
    }

    fun reconnect() {
        io.github.steven_stu77.lazee.feature.autolog.listener.LazeeNotificationListener.reconnect(context)
        refreshSystemState()
    }

    fun setEnabled(on: Boolean) = viewModelScope.launch {
        settings.setAutoLog(on)
        // First time on: tick every supported app so it works straight away.
        if (on && settings.current().enabledPackages.isEmpty()) settings.setEnabledPackages(config.parserConfigs.map { it.`package` }.toSet())
    }

    fun setApp(pkg: String, on: Boolean) = viewModelScope.launch {
        val cur = settings.current().enabledPackages
        settings.setEnabledPackages(if (on) cur + pkg else cur - pkg)
    }

    fun setAccount(pkg: String, accountId: Long) = viewModelScope.launch { settings.setPackageAccount(pkg, accountId) }
    fun setQuickConfirm(on: Boolean) = viewModelScope.launch { settings.setQuickConfirm(on) }
    fun setAutoFamiliar(on: Boolean) = viewModelScope.launch { settings.setAutoFamiliar(on) }
    fun setNightly(on: Boolean) = viewModelScope.launch { settings.setNightly(on) }
}
