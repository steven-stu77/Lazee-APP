package io.github.steven_stu77.lazee.feature.me

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.prefs.BackupFrequency
import io.github.steven_stu77.lazee.core.ui.components.HeroCard
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SettingRow
import io.github.steven_stu77.lazee.core.ui.components.TagStyle
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.nav.Routes

@Composable
fun MeScreen(onNavigate: (String) -> Unit, onLockToggle: (Boolean) -> Unit, vm: MeViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.nav_me), style = Type.pageTitle)
        HeroCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.me_net_worth), style = Type.small.copy(color = p.sub))
                Text(moneyText(s.netWorth), style = Type.heroAmount.copy(color = Color.White))
            }
            Row(Modifier.fillMaxWidth()) {
                Text("${stringResource(R.string.me_assets)} ${moneyText(s.assets)}", style = Type.small.copy(color = p.sub2), modifier = Modifier.weight(1f))
                Text("${stringResource(R.string.me_liabilities)} ${moneyText(s.liabilities)}", style = Type.small.copy(color = p.sub2))
            }
        }
        Text(stringResource(R.string.me_accounts), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
        SectionCard(padding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
            s.accounts.forEachIndexed { i, a ->
                if (i > 0) HorizontalDivider(color = io.github.steven_stu77.lazee.core.ui.theme.Base.divider)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onNavigate(Routes.account(a.account.id)) }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(a.account.name, style = Type.body, modifier = Modifier.weight(1f))
                    Text(moneyText(a.balance), style = Type.body.copy(fontWeight = FontWeight.SemiBold))
                }
            }
            HorizontalDivider(color = io.github.steven_stu77.lazee.core.ui.theme.Base.divider)
            Text(
                stringResource(R.string.me_add_account), style = Type.body.copy(color = p.accent, fontWeight = FontWeight.Medium),
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(Routes.accountEdit()) }.padding(vertical = 14.dp),
            )
        }
        Text(stringResource(R.string.me_settings), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
        val st = s.settings
        SectionCard(padding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
            SettingRow(stringResource(R.string.autolog_title), status = stringResource(if (st.autoLogEnabled) R.string.on else R.string.off), statusOn = st.autoLogEnabled) { onNavigate(Routes.AUTOLOG) }
            SettingRow(
                stringResource(R.string.bills_title),
                status = if (s.unpaidBills > 0) stringResource(R.string.me_bills_due, s.unpaidBills) else stringResource(R.string.me_all_paid),
                statusStyle = if (s.unpaidBills > 0) TagStyle.WARN else TagStyle.ON,
            ) { onNavigate(Routes.BILLS) }
            SettingRow(stringResource(R.string.loans_title), status = if (s.openLoans > 0) s.openLoans.toString() else null) { onNavigate(Routes.LOANS) }
            SettingRow(stringResource(R.string.cats_title), status = s.categoryCount.toString()) { onNavigate(Routes.CATEGORIES) }
            SettingRow(
                stringResource(R.string.backup_title),
                status = stringResource(if (!st.autoBackup) R.string.backup_manual else if (st.backupFrequency == BackupFrequency.DAILY) R.string.backup_daily_auto else R.string.backup_weekly_auto),
                statusOn = st.autoBackup,
            ) { onNavigate(Routes.BACKUP) }
            SettingRow(stringResource(R.string.look_title), status = stringResource(themeName(st.theme)) + " · " + stringResource(R.string.lang_short), statusOn = true) { onNavigate(Routes.LOOK) }
            SettingRow(stringResource(R.string.reminders_title)) { onNavigate(Routes.REMINDERS) }
            SettingRow(stringResource(R.string.app_lock), checked = st.appLockEnabled, onCheckedChange = onLockToggle)
            SettingRow(stringResource(R.string.about_title)) { onNavigate(Routes.ABOUT) }
        }
    }
}

fun themeName(k: io.github.steven_stu77.lazee.core.prefs.ThemeKey): Int = when (k) {
    io.github.steven_stu77.lazee.core.prefs.ThemeKey.green -> R.string.theme_green
    io.github.steven_stu77.lazee.core.prefs.ThemeKey.blue -> R.string.theme_blue
    io.github.steven_stu77.lazee.core.prefs.ThemeKey.terra -> R.string.theme_terra
    io.github.steven_stu77.lazee.core.prefs.ThemeKey.grape -> R.string.theme_grape
    io.github.steven_stu77.lazee.core.prefs.ThemeKey.ink -> R.string.theme_ink
}

