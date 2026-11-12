package io.github.steven_stu77.lazee.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.prefs.BackupFrequency
import io.github.steven_stu77.lazee.core.ui.components.MenuItem
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SecondaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SegmentedControl
import io.github.steven_stu77.lazee.core.ui.components.SettingRow
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type
import java.text.DateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Date

@Composable
fun BackupScreen(onBack: () -> Unit, vm: BackupViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val st = s.settings
    var askPassword by remember { mutableStateOf(false) }
    var csvRange by remember { mutableStateOf<Pair<LocalDate, LocalDate>?>(null) }
    var pickRange by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restorePw by remember { mutableStateOf("") }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let(vm::setFolder) }
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val r = csvRange
        if (uri != null && r != null) vm.exportCsv(uri, r.first, r.second)
    }
    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { restoreUri = uri; restorePw = "" } }

    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.backup_title), onBack, stringResource(R.string.back))
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard(padding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
                SettingRow(stringResource(R.string.bk_last), status = if (st.lastBackupAt > 0) DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(st.lastBackupAt)) + stringResource(R.string.bk_encrypted) else stringResource(R.string.bk_never))
                HorizontalDivider(color = Base.divider)
                SettingRow(stringResource(R.string.bk_auto), checked = st.autoBackup, onCheckedChange = { on ->
                    if (on && (st.backupTreeUri == null || st.backupPasswordEncrypted == null)) { if (st.backupPasswordEncrypted == null) askPassword = true else pickFolder.launch(null) }
                    vm.setAuto(on)
                })
                if (st.autoBackup) {
                    Text(stringResource(R.string.bk_frequency), style = Type.small)
                    SegmentedControl(listOf(stringResource(R.string.bk_daily), stringResource(R.string.bk_weekly)), if (st.backupFrequency == BackupFrequency.DAILY) 0 else 1,
                        { vm.setFrequency(if (it == 0) BackupFrequency.DAILY else BackupFrequency.WEEKLY) }, Modifier.fillMaxWidth().padding(bottom = 10.dp))
                }
                HorizontalDivider(color = Base.divider)
                SettingRow(stringResource(R.string.bk_location), description = s.folderName ?: stringResource(R.string.bk_location_hint), status = stringResource(R.string.bk_change)) { pickFolder.launch(null) }
                HorizontalDivider(color = Base.divider)
                SettingRow(stringResource(R.string.bk_password), description = stringResource(R.string.bk_password_sub),
                    status = stringResource(if (st.backupPasswordEncrypted != null) R.string.bk_set else R.string.bk_not_set), statusOn = st.backupPasswordEncrypted != null) { askPassword = true }
            }
            PrimaryButton(stringResource(R.string.bk_now), {
                when {
                    st.backupPasswordEncrypted == null -> askPassword = true
                    st.backupTreeUri == null -> pickFolder.launch(null)
                    else -> vm.backupNow()
                }
            }, Modifier.fillMaxWidth(), enabled = !s.busy)
            SecondaryButton(stringResource(R.string.bk_csv), { pickRange = true }, Modifier.fillMaxWidth())
            SecondaryButton(stringResource(R.string.bk_restore), { openBackup.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth())
            Text(stringResource(R.string.bk_note), style = Type.tiny, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }

    if (askPassword) PasswordDialog(onDismiss = { askPassword = false }) { vm.setPassword(it); askPassword = false; if (st.backupTreeUri == null) pickFolder.launch(null) }

    if (pickRange) {
        val now = LocalDate.now()
        val ym = YearMonth.now()
        val options = listOf(
            R.string.bk_range_this_month to (ym.atDay(1) to ym.atEndOfMonth()),
            R.string.bk_range_last_month to (ym.minusMonths(1).atDay(1) to ym.minusMonths(1).atEndOfMonth()),
            R.string.bk_range_this_year to (LocalDate.of(now.year, 1, 1) to LocalDate.of(now.year, 12, 31)),
            R.string.bk_range_all to (LocalDate.of(2000, 1, 1) to LocalDate.of(2100, 1, 1)),
        )
        AlertDialog(
            onDismissRequest = { pickRange = false },
            title = { Text(stringResource(R.string.bk_csv)) },
            text = { Column { options.forEach { (label, range) -> MenuItem(stringResource(label)) { csvRange = range; pickRange = false; createCsv.launch(vm.csvName()) } } } },
            confirmButton = { TextButton({ pickRange = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    restoreUri?.let { uri ->
        if (s.restoreChecked == uri) AlertDialog(
            onDismissRequest = { vm.clearRestore(); restoreUri = null },
            title = { Text(stringResource(R.string.bk_restore_confirm_title)) },
            text = { Text(stringResource(R.string.bk_restore_confirm)) },
            confirmButton = { TextButton({ vm.restore(uri, restorePw); restoreUri = null }) { Text(stringResource(R.string.bk_restore), color = Base.dangerText) } },
            dismissButton = { TextButton({ vm.clearRestore(); restoreUri = null }) { Text(stringResource(R.string.cancel)) } },
        ) else AlertDialog(
            onDismissRequest = { vm.clearRestore(); restoreUri = null },
            title = { Text(stringResource(R.string.bk_restore)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.bk_enter_password), style = Type.small)
                    PasswordField(restorePw, { restorePw = it })
                    s.restoreError?.let { Text(stringResource(it), style = Type.small.copy(color = Base.dangerText)) }
                }
            },
            confirmButton = { TextButton({ vm.checkRestore(uri, restorePw) }, enabled = restorePw.isNotEmpty() && !s.busy) { Text(stringResource(R.string.next)) } },
            dismissButton = { TextButton({ vm.clearRestore(); restoreUri = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String? = null) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), singleLine = true, label = label?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bk_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.bk_password_warn), style = Type.small)
                PasswordField(a, { a = it }, stringResource(R.string.bk_password_new))
                PasswordField(b, { b = it }, stringResource(R.string.bk_password_again))
                if (b.isNotEmpty() && a != b) Text(stringResource(R.string.bk_password_mismatch), style = Type.small.copy(color = Base.dangerText))
            }
        },
        confirmButton = { TextButton({ onSave(a) }, enabled = a.length >= 6 && a == b) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

