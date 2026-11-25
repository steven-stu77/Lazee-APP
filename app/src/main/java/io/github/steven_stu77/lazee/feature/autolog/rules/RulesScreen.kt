package io.github.steven_stu77.lazee.feature.autolog.rules

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.ui.components.BannerKind
import io.github.steven_stu77.lazee.core.ui.components.InfoBanner
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SecondaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.ToastBus
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.autolog.AutoLogConfig
import io.github.steven_stu77.lazee.feature.autolog.parser.ParserConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(private val config: AutoLogConfig, private val settings: SettingsRepository) : ViewModel() {
    val custom: StateFlow<List<ParserConfig>> = config.custom
    val builtInApps: List<String> = config.builtIn.map { it.displayName }

    fun delete(pkg: String, index: Int) = viewModelScope.launch(Dispatchers.IO) {
        config.saveCustom(config.custom.value.map { c -> if (c.`package` == pkg) c.copy(rules = c.rules.filterIndexed { i, _ -> i != index }) else c })
    }

    fun export(): String = config.exportCustom()

    /** Returns the number of rules imported, or null when the text is not valid rules. */
    suspend fun import(text: String): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val n = config.importCustom(text)
            val enabled = settings.current().enabledPackages
            settings.setEnabledPackages(enabled + config.custom.value.map { it.`package` })
            n
        }.getOrNull()
    }
}

/** 自定义识别规则: teach new notification formats without a new APK. */
@Composable
fun RulesScreen(onBack: () -> Unit, onEdit: (pkg: String, index: Int) -> Unit, vm: RulesViewModel = hiltViewModel()) {
    val custom by vm.custom.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var importing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Pair<String, Int>?>(null) }
    val copied = stringResource(R.string.rules_copied)

    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.rules_title), onBack, stringResource(R.string.back))
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { InfoBanner(stringResource(R.string.rules_info), BannerKind.INFO) }
            item { PrimaryButton(stringResource(R.string.rules_new), { onEdit("", -1) }, Modifier.fillMaxWidth()) }
            if (custom.isEmpty()) item { Text(stringResource(R.string.rules_empty), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp)) }
            items(custom, key = { it.`package` }) { app ->
                SectionCard {
                    Text(app.displayName, style = Type.section)
                    app.rules.forEachIndexed { i, r ->
                        if (i > 0) HorizontalDivider(color = Base.divider)
                        Row(Modifier.fillMaxWidth().clickable { onEdit(app.`package`, i) }.heightIn(min = 48.dp).padding(vertical = 8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(directionLabel(r.direction)), style = Type.body)
                                Text(r.sample ?: r.pattern, style = Type.tiny, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton({ deleting = app.`package` to i }) { Text(stringResource(R.string.delete), color = Base.dangerText) }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(stringResource(R.string.rules_import), { importing = true }, Modifier.weight(1f))
                    SecondaryButton(stringResource(R.string.rules_export), { copy(ctx, vm.export()); ToastBus.show(copied) }, Modifier.weight(1f))
                }
            }
            item { Text(stringResource(R.string.rules_builtin, vm.builtInApps.joinToString("、")), style = Type.tiny, modifier = Modifier.padding(horizontal = 4.dp)) }
        }
    }

    deleting?.let { (pkg, i) ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.rules_delete_confirm)) },
            confirmButton = { TextButton({ vm.delete(pkg, i); deleting = null }) { Text(stringResource(R.string.delete), color = Base.dangerText) } },
            dismissButton = { TextButton({ deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (importing) ImportDialog(vm) { importing = false }
}

@Composable
private fun ImportDialog(vm: RulesViewModel, onClose: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.rules_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.rules_import_hint), style = Type.small)
                LazeeTextField(text, { text = it; failed = false }, Modifier.fillMaxWidth().heightIn(min = 120.dp), singleLine = false)
                if (failed) Text(stringResource(R.string.rules_import_bad), style = Type.small.copy(color = Base.dangerText))
            }
        },
        confirmButton = {
            TextButton({
                scope.launch {
                    val n = vm.import(text)
                    if (n == null) failed = true else { ToastBus.show(ctx.getString(R.string.rules_imported, n)); onClose() }
                }
            }, enabled = text.isNotBlank()) { Text(stringResource(R.string.rules_import)) }
        },
        dismissButton = { TextButton(onClose) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun copy(ctx: Context, text: String) {
    ctx.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("lazee-rules", text))
}
