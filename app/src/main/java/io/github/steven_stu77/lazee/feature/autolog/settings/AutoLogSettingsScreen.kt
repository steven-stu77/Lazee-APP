package io.github.steven_stu77.lazee.feature.autolog.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.BuildConfig
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.ui.components.BannerKind
import io.github.steven_stu77.lazee.core.ui.components.InfoBanner
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.components.SecondaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SettingRow
import io.github.steven_stu77.lazee.core.ui.components.StatusTag
import io.github.steven_stu77.lazee.core.ui.components.LazeeSwitch
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.TagStyle
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.search.ListPicker

private val rowPad = PaddingValues(horizontal = 20.dp, vertical = 4.dp)

@Composable
fun AutoLogSettingsScreen(onBack: () -> Unit, onParserTest: () -> Unit, vm: AutoLogSettingsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var consent by remember { mutableStateOf(false) }
    var accountFor by remember { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshSystemState() }
    val on = s.settings.autoLogEnabled

    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.autolog_title), onBack, stringResource(R.string.back))
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard(padding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                SettingRow(stringResource(R.string.al_enable), description = stringResource(R.string.al_enable_sub), checked = on,
                    onCheckedChange = { if (it) consent = true else vm.setEnabled(false) })
            }
            if (on && s.listenerGranted) StatusCard(s, vm::reconnect)
            if (on && !s.listenerGranted) PermissionHelp(ctx)
            if (on && isXiaomi()) XiaomiHelp(ctx)
            if (on && s.listenerGranted && !s.batteryOk) BatteryHelp(ctx)

            Text(stringResource(R.string.al_lazy), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
            SectionCard(padding = rowPad) {
                SettingRow(stringResource(R.string.al_quick), description = stringResource(R.string.al_quick_sub), checked = s.settings.lazyQuickConfirm, onCheckedChange = vm::setQuickConfirm)
                HorizontalDivider(color = Base.divider)
                SettingRow(stringResource(R.string.al_familiar), description = stringResource(R.string.al_familiar_sub), checked = s.settings.lazyAutoFamiliar, onCheckedChange = vm::setAutoFamiliar)
                HorizontalDivider(color = Base.divider)
                SettingRow(stringResource(R.string.al_nightly), description = stringResource(R.string.al_nightly_sub), checked = s.settings.nightlySummary, onCheckedChange = vm::setNightly)
            }

            Text(stringResource(R.string.al_preview_label), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
            NotificationPreview()

            Text(stringResource(R.string.al_apps), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
            SectionCard(padding = rowPad) {
                s.apps.forEachIndexed { i, a ->
                    if (i > 0) HorizontalDivider(color = Base.divider)
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(a.config.displayName, style = Type.body)
                            Text(stringResource(R.string.al_into_account, a.account?.name ?: "—") + " ›", style = Type.tiny.copy(color = LocalPalette.current.accent),
                                modifier = Modifier.clickable { accountFor = a.config.`package` })
                        }
                        if (a.config.preview) StatusTag(stringResource(R.string.al_coming_soon), style = TagStyle.WARN)
                        LazeeSwitch(a.enabled, { vm.setApp(a.config.`package`, it) }, a.config.displayName)
                    }
                }
            }
            if (s.apps.any { it.config.preview }) Text(stringResource(R.string.al_preview_rules_note), style = Type.tiny, modifier = Modifier.padding(horizontal = 4.dp))

            Text(stringResource(R.string.al_safety), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
            SectionCard(padding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                listOf(R.string.al_safe_1, R.string.al_safe_2, R.string.al_safe_3, R.string.al_safe_4, R.string.al_safe_5).forEach {
                    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(LazeeIcons.Shield, null, tint = LocalPalette.current.accent, modifier = Modifier.size(20.dp))
                        Text(stringResource(it), style = Type.body.copy(fontSize = 14.sp))
                    }
                }
            }
            if (BuildConfig.DEBUG) SecondaryButton(stringResource(R.string.pt_title), onParserTest, Modifier.fillMaxWidth())
        }
    }

    if (consent) AlertDialog(
        onDismissRequest = { consent = false },
        title = { Text(stringResource(R.string.al_consent_title)) },
        text = { Text(stringResource(R.string.al_consent_body)) },
        confirmButton = {
            TextButton({
                consent = false
                vm.setEnabled(true)
                ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }) { Text(stringResource(R.string.al_consent_ok)) }
        },
        dismissButton = { TextButton({ consent = false }) { Text(stringResource(R.string.ob_later)) } },
    )
    accountFor?.let { pkg ->
        ListPicker(stringResource(R.string.txn_account), s.accounts.map { it.id to it.name }, { accountFor = null }) { id -> id?.let { vm.setAccount(pkg, it) } }
    }
}

@Composable
private fun PermissionHelp(ctx: Context) {
    SectionCard {
        InfoBanner(stringResource(R.string.al_not_granted), BannerKind.WARN)
        SecondaryButton(stringResource(R.string.al_open_listener_settings), { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, Modifier.fillMaxWidth())
        if (Build.VERSION.SDK_INT >= 33) {
            Text(stringResource(R.string.al_restricted_title), style = Type.section)
            Text(stringResource(R.string.al_restricted_steps), style = Type.small.copy(color = Base.text))
            SecondaryButton(stringResource(R.string.al_open_app_info), {
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
            }, Modifier.fillMaxWidth())
        }
    }
}

@SuppressLint("BatteryLife") // Needed so the listener is not killed; Play policy allows it for this use.
@Composable
private fun BatteryHelp(ctx: Context) {
    SectionCard {
        InfoBanner(stringResource(R.string.al_battery), BannerKind.INFO)
        SecondaryButton(stringResource(R.string.al_battery_button), {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))) }
                .onFailure { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }, Modifier.fillMaxWidth())
        Text(stringResource(R.string.al_autostart_note), style = Type.tiny)
    }
}

private fun isXiaomi() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

@Composable
private fun StatusCard(s: AutoLogSettingsUiState, onReconnect: () -> Unit) {
    val fmt = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
    SectionCard {
        Text(stringResource(R.string.al_status), style = Type.section)
        Text(
            if (s.connectedAt > 0) stringResource(R.string.al_status_connected, fmt.format(java.util.Date(s.connectedAt)))
            else stringResource(R.string.al_status_not_connected),
            style = Type.small.copy(color = if (s.connectedAt > 0) LocalPalette.current.tintText else Base.warnStrong),
        )
        val last = s.lastSeen
        Text(
            if (last == null) stringResource(R.string.al_status_none_yet)
            else stringResource(R.string.al_status_last, last.first, fmt.format(java.util.Date(last.second)), stringResource(outcomeText(last.third))),
            style = Type.small.copy(color = Base.text),
        )
        SecondaryButton(stringResource(R.string.al_reconnect), onReconnect, Modifier.fillMaxWidth())
    }
}

private fun outcomeText(o: String) = when (o) {
    "PENDING" -> R.string.al_out_pending
    "AUTO_LOGGED" -> R.string.al_out_auto
    "DUPLICATE" -> R.string.al_out_duplicate
    "NOT_PAYMENT" -> R.string.al_out_not_payment
    "APP_OFF" -> R.string.al_out_app_off
    else -> R.string.al_out_error
}

@Composable
private fun XiaomiHelp(ctx: Context) {
    SectionCard {
        Text(stringResource(R.string.al_xiaomi_title), style = Type.section)
        Text(stringResource(R.string.al_xiaomi_steps), style = Type.small.copy(color = Base.text))
        SecondaryButton(stringResource(R.string.al_open_app_info), {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
        }, Modifier.fillMaxWidth())
    }
}

/** Dark mock of the system notification, as in the prototype. */
@Composable
private fun NotificationPreview() {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF2A2D33)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.al_preview_app), style = Type.tiny.copy(color = Color(0xFFC3C7CE)))
        Text(stringResource(R.string.al_preview_line), style = Type.body.copy(color = Color.White, fontWeight = FontWeight.SemiBold))
        Text(stringResource(R.string.an_from_app, "TNG eWallet"), style = Type.small.copy(color = Color(0xFFC3C7CE)))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(R.string.confirm, R.string.an_change, R.string.an_ignore).forEach {
                Text(stringResource(it), style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = p.glow))
            }
        }
    }
}
