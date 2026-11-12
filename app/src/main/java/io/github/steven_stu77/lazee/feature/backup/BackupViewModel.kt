package io.github.steven_stu77.lazee.feature.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.backup.BackupManager
import io.github.steven_stu77.lazee.core.backup.NoBackupFolderException
import io.github.steven_stu77.lazee.core.backup.NotABackupException
import io.github.steven_stu77.lazee.core.backup.WrongPasswordException
import io.github.steven_stu77.lazee.core.csv.CsvService
import io.github.steven_stu77.lazee.core.prefs.BackupFrequency
import io.github.steven_stu77.lazee.core.prefs.Settings
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.ui.components.ToastBus
import io.github.steven_stu77.lazee.work.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BackupUiState(
    val settings: Settings = Settings(),
    val folderName: String? = null,
    val busy: Boolean = false,
    val restoreError: Int? = null,
    val restoreChecked: Uri? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backup: BackupManager,
    private val csv: CsvService,
    private val settings: SettingsRepository,
    private val scheduler: WorkScheduler,
) : ViewModel() {
    private val local = MutableStateFlow(BackupUiState())

    val state: StateFlow<BackupUiState> = combine(settings.settings, local) { s, l ->
        l.copy(settings = s, folderName = s.backupTreeUri?.let { runCatching { DocumentFile.fromTreeUri(context, Uri.parse(it))?.name }.getOrNull() })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    private fun toast(id: Int, vararg args: Any) = ToastBus.show(context.getString(id, *args))

    fun setFolder(uri: Uri) = viewModelScope.launch {
        // Keep access across reboots; the picker only grants it for this session otherwise.
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        settings.setBackupTreeUri(uri.toString())
        scheduler.scheduleBackup(settings.current())
    }

    fun setPassword(pw: String) = viewModelScope.launch { backup.setPassword(pw.toCharArray()); toast(R.string.bk_password_saved) }

    fun setAuto(on: Boolean) = viewModelScope.launch { settings.setAutoBackup(on); scheduler.scheduleBackup(settings.current()) }
    fun setFrequency(f: BackupFrequency) = viewModelScope.launch { settings.setBackupFrequency(f); scheduler.scheduleBackup(settings.current()) }

    fun backupNow() = viewModelScope.launch {
        local.update { it.copy(busy = true) }
        try {
            backup.backupNow()
            toast(R.string.bk_done)
        } catch (e: NoBackupFolderException) {
            toast(R.string.bk_folder_lost)
        } catch (e: Exception) {
            toast(R.string.bk_failed)
        } finally {
            local.update { it.copy(busy = false) }
        }
    }

    fun exportCsv(uri: Uri, from: LocalDate, to: LocalDate) = viewModelScope.launch {
        runCatching { csv.export(uri, from, to) }.onSuccess { toast(R.string.bk_csv_done, it) }.onFailure { toast(R.string.bk_failed) }
    }

    /** Step 1: check the password without touching any data. */
    fun checkRestore(uri: Uri, pw: String) = viewModelScope.launch {
        local.update { it.copy(busy = true, restoreError = null) }
        val err = try { backup.read(uri, pw.toCharArray()); null } catch (e: WrongPasswordException) { R.string.bk_wrong_password } catch (e: NotABackupException) { R.string.bk_not_backup } catch (e: Exception) { R.string.bk_failed }
        local.update { it.copy(busy = false, restoreError = err, restoreChecked = if (err == null) uri else null) }
    }

    /** Step 2 (after "this replaces everything" is confirmed). */
    fun restore(uri: Uri, pw: String) = viewModelScope.launch {
        local.update { it.copy(busy = true, restoreChecked = null) }
        runCatching { backup.restore(uri, pw.toCharArray()) }
            .onSuccess { toast(R.string.bk_restored) }.onFailure { toast(R.string.bk_failed) }
        local.update { it.copy(busy = false) }
    }

    fun clearRestore() = local.update { it.copy(restoreError = null, restoreChecked = null) }
    fun csvName() = "lazee-${LocalDate.now()}.csv"
}
