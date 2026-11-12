package io.github.steven_stu77.lazee.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.prefs.Settings
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SettingRow
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.work.WorkScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(private val settings: SettingsRepository, private val scheduler: WorkScheduler) : ViewModel() {
    val state: StateFlow<Settings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    fun setNightly(on: Boolean) = viewModelScope.launch { settings.setNightly(on); scheduler.scheduleNightly(settings.current()) }
    fun setTime(minutes: Int) = viewModelScope.launch { settings.setNightlyMinutes(minutes); scheduler.scheduleNightly(settings.current()) }
    fun setBudgetAlerts(on: Boolean) = viewModelScope.launch { settings.setBudgetAlerts(on) }
}

/** 提醒设置 (原型外): nightly summary time and budget alerts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(onBack: () -> Unit, vm: RemindersViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var pickTime by remember { mutableStateOf(false) }
    val time = "%02d:%02d".format(s.nightlySummaryMinutes / 60, s.nightlySummaryMinutes % 60)
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.reminders_title), onBack, stringResource(R.string.back))
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard(padding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
                SettingRow(stringResource(R.string.al_nightly), description = stringResource(R.string.al_nightly_sub), checked = s.nightlySummary, onCheckedChange = vm::setNightly)
                HorizontalDivider(color = Base.divider)
                SettingRow(stringResource(R.string.rem_time), status = time, statusOn = true) { pickTime = true }
                HorizontalDivider(color = Base.divider)
                SettingRow(stringResource(R.string.rem_budget), description = stringResource(R.string.rem_budget_sub), checked = s.budgetAlerts, onCheckedChange = vm::setBudgetAlerts)
            }
            SectionCard { SettingRow(stringResource(R.string.bills_title), description = stringResource(R.string.rem_bills_sub)) }
        }
    }
    if (pickTime) {
        val st = rememberTimePickerState(s.nightlySummaryMinutes / 60, s.nightlySummaryMinutes % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(st) },
            confirmButton = { TextButton({ vm.setTime(st.hour * 60 + st.minute); pickTime = false }) { Text(stringResource(R.string.ok)) } },
            dismissButton = { TextButton({ pickTime = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
