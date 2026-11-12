package io.github.steven_stu77.lazee.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.ui.components.BannerKind
import io.github.steven_stu77.lazee.core.ui.components.InfoBanner
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SecondaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type

/** First-run flow (spec §5.1). [onDone] gets true when the user chose to turn on auto-log. */
@Composable
fun OnboardingScreen(onDone: (openAutoLog: Boolean) -> Unit, vm: OnboardingViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var wantsAutoLog by remember { mutableStateOf(false) }
    val names = mapOf(
        "cash" to stringResource(R.string.cash),
        "bank" to stringResource(R.string.ob_acc_bank),
        "card" to stringResource(R.string.ob_acc_card),
    )
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.finish(names) }
    fun finish(autoLog: Boolean) {
        wantsAutoLog = autoLog
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.finish(names)
    }
    LaunchedEffect(s.finished) { if (s.finished) onDone(wantsAutoLog) }

    Column(Modifier.fillMaxSize().background(Base.page).padding(20.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StepDots(s.step)
            when (s.step) {
                0 -> LanguageStep(s.language, vm::setLanguage)
                1 -> AccountsStep(s, names, vm::toggle, vm::setBalance)
                2 -> BudgetStep(s.budget, vm::setBudget)
                else -> AutoLogStep()
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when (s.step) {
                0 -> PrimaryButton(stringResource(R.string.next), vm::next, Modifier.weight(1f))
                1 -> {
                    SecondaryButton(stringResource(R.string.back), vm::back, Modifier.weight(1f))
                    PrimaryButton(stringResource(R.string.next), vm::next, Modifier.weight(2f), enabled = s.options.any { it.checked })
                }
                2 -> {
                    SecondaryButton(stringResource(R.string.skip), { vm.setBudget(""); vm.next() }, Modifier.weight(1f))
                    PrimaryButton(stringResource(R.string.next), vm::next, Modifier.weight(2f))
                }
                else -> {
                    SecondaryButton(stringResource(R.string.ob_later), { finish(false) }, Modifier.weight(1f))
                    PrimaryButton(stringResource(R.string.ob_turn_on), { finish(true) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StepDots(step: Int) {
    val p = LocalPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
        repeat(OnboardingUiState.STEPS) { i ->
            Box(Modifier.size(width = if (i == step) 20.dp else 6.dp, height = 6.dp).clip(CircleShape).background(if (i <= step) p.accent else Base.switchOff))
        }
    }
}

@Composable
private fun LanguageStep(selected: String, onSelect: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(R.drawable.logo), null, Modifier.size(96.dp))
        Text(stringResource(R.string.app_name), style = Type.pageTitle.copy(fontSize = 28.sp))
        Text(stringResource(R.string.app_tagline), style = Type.small)
    }
    Text(stringResource(R.string.ob_choose_language), style = Type.section, modifier = Modifier.padding(top = 16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BigChoice("中文", selected == "zh", Modifier.weight(1f)) { onSelect("zh") }
        BigChoice("English", selected == "en", Modifier.weight(1f)) { onSelect("en") }
    }
}

@Composable
fun BigChoice(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier.heightIn(min = 64.dp).clip(RoundedCornerShape(16.dp)).background(if (selected) p.tint else Base.card)
            .border(if (selected) 2.dp else 1.dp, if (selected) p.accent else Base.border, RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = Type.section.copy(color = if (selected) p.tintText else Base.text)) }
}

@Composable
private fun AccountsStep(s: OnboardingUiState, names: Map<String, String>, onToggle: (Int) -> Unit, onBalance: (Int, String) -> Unit) {
    Text(stringResource(R.string.ob_accounts_title), style = Type.pageTitle)
    Text(stringResource(R.string.ob_accounts_desc), style = Type.small)
    SectionCard {
        s.options.forEachIndexed { i, o ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CheckBox(o.checked) { onToggle(i) }
                Text(names[o.key] ?: o.key, style = Type.body, modifier = Modifier.weight(1f).clickable { onToggle(i) })
                if (o.checked) LazeeTextField(
                    o.balance, { onBalance(i, it) }, Modifier.width(140.dp),
                    placeholder = stringResource(R.string.ob_balance), prefix = "RM", keyboardType = KeyboardType.Decimal,
                )
            }
        }
    }
}

@Composable
fun CheckBox(checked: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(if (checked) p.accent else Base.card)
            .border(1.5.dp, if (checked) p.accent else Base.border, RoundedCornerShape(8.dp))
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { if (checked) Icon(LazeeIcons.Check, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun BudgetStep(value: String, onChange: (String) -> Unit) {
    Text(stringResource(R.string.ob_budget_title), style = Type.pageTitle)
    Text(stringResource(R.string.ob_budget_desc), style = Type.small)
    LazeeTextField(value, onChange, Modifier.fillMaxWidth(), placeholder = "1500", prefix = "RM", keyboardType = KeyboardType.Decimal)
}

@Composable
private fun AutoLogStep() {
    Text(stringResource(R.string.ob_autolog_title), style = Type.pageTitle)
    Text(stringResource(R.string.ob_autolog_desc), style = Type.body)
    SectionCard {
        Text(stringResource(R.string.ob_reads), style = Type.section)
        Text(stringResource(R.string.ob_reads_body), style = Type.small)
        Spacer(Modifier.heightIn(4.dp))
        Text(stringResource(R.string.ob_not_reads), style = Type.section)
        Text(stringResource(R.string.ob_not_reads_body), style = Type.small)
    }
    InfoBanner(stringResource(R.string.ob_local_only), BannerKind.THEME, icon = LazeeIcons.Shield)
}
