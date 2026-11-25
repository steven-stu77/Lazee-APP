package io.github.steven_stu77.lazee.feature.autolog.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.ui.components.Chip
import io.github.steven_stu77.lazee.core.ui.components.LazeeSwitch
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.MenuItem
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.autolog.parser.Direction
import io.github.steven_stu77.lazee.feature.autolog.parser.RuleBuilder

fun directionLabel(d: Direction): Int = when (d) {
    Direction.OUT -> R.string.re_dir_out
    Direction.IN -> R.string.re_dir_in
    Direction.REFUND -> R.string.re_dir_refund
    Direction.PERSON_TRANSFER -> R.string.re_dir_person
    Direction.OWN_TRANSFER -> R.string.re_dir_own
    Direction.IGNORE -> R.string.re_dir_ignore
}

/** Teach Lazee a new notification format from one example — no new APK needed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuleEditorScreen(onBack: () -> Unit, vm: RuleEditorViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var pickApp by remember { mutableStateOf(false) }
    LaunchedEffect(s.saved) { if (s.saved) onBack() }

    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(if (s.editIndex >= 0) R.string.re_edit_title else R.string.re_new_title), onBack, stringResource(R.string.back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                Step(1, R.string.re_step_app)
                Chip(s.appName.ifBlank { stringResource(R.string.re_pick_app) }, s.pkg.isNotBlank(), onClick = { pickApp = true })
            }
            SectionCard {
                Step(2, R.string.re_step_sample)
                Text(stringResource(R.string.re_sample_hint), style = Type.tiny)
                LazeeTextField(s.sample, vm::setSample, Modifier.fillMaxWidth().heightIn(min = 96.dp), placeholder = stringResource(R.string.re_sample_ph), singleLine = false)
            }
            SectionCard {
                Step(3, R.string.re_step_type)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Direction.OUT, Direction.IN, Direction.REFUND, Direction.PERSON_TRANSFER, Direction.OWN_TRANSFER, Direction.IGNORE).forEach { d ->
                        Chip(stringResource(directionLabel(d)), s.direction == d, onClick = { vm.setDirection(d) })
                    }
                }
                if (s.direction == Direction.OWN_TRANSFER) Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.re_own_incoming), style = Type.body, modifier = Modifier.weight(1f))
                    LazeeSwitch(s.incoming, vm::setIncoming)
                }
            }
            if (s.direction != Direction.IGNORE) SectionCard {
                Step(4, R.string.re_step_mark)
                Text(stringResource(R.string.re_amount), style = Type.small)
                LazeeTextField(s.amountText, vm::setAmount, Modifier.fillMaxWidth(), placeholder = "RM4.20")
                if (s.needsMerchant) {
                    Text(stringResource(R.string.re_merchant), style = Type.small)
                    LazeeTextField(s.merchantText, vm::setMerchant, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.re_merchant_ph))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.re_ignore_numbers), style = Type.body)
                        Text(stringResource(R.string.re_ignore_numbers_sub), style = Type.tiny)
                    }
                    LazeeSwitch(s.ignoreNumbers, vm::setIgnoreNumbers)
                }
                s.error?.let { Text(stringResource(errorText(it)), style = Type.small.copy(color = Base.dangerText)) }
            }
            SectionCard {
                Text(stringResource(R.string.re_test_title), style = Type.section)
                TestResult(vm.test(s.sample))
                Text(stringResource(R.string.re_test_more), style = Type.small)
                LazeeTextField(s.testText, vm::setTestText, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.re_test_more_ph), singleLine = false)
                if (s.testText.isNotBlank()) TestResult(vm.test(s.testText))
            }
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.re_advanced), style = Type.body, modifier = Modifier.weight(1f))
                    LazeeSwitch(s.advanced, vm::setAdvanced)
                }
                if (s.advanced) {
                    Text(stringResource(R.string.re_advanced_hint), style = Type.tiny)
                    LazeeTextField(s.pattern, vm::setPattern, Modifier.fillMaxWidth(), singleLine = false)
                    if (s.badPattern) Text(stringResource(R.string.re_bad_pattern), style = Type.small.copy(color = Base.dangerText))
                } else if (s.pattern.isNotBlank()) {
                    Text(s.pattern, style = Type.tiny.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                }
            }
        }
        PrimaryButton(stringResource(R.string.re_save), vm::save, Modifier.fillMaxWidth().padding(20.dp), enabled = s.canSave)
    }

    if (pickApp) AlertDialog(
        onDismissRequest = { pickApp = false },
        title = { Text(stringResource(R.string.re_pick_app)) },
        text = { LazyColumn { items(s.apps, key = { it.pkg }) { a -> MenuItem("${a.name}\n${a.pkg}") { vm.pickApp(a); pickApp = false } } } },
        confirmButton = { TextButton({ pickApp = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Step(n: Int, title: Int) {
    Text("$n. " + stringResource(title), style = Type.section)
}

private fun errorText(r: RuleBuilder.Reason) = when (r) {
    RuleBuilder.Reason.NO_AMOUNT -> R.string.re_err_no_amount
    RuleBuilder.Reason.AMOUNT_NOT_IN_SAMPLE -> R.string.re_err_amount_missing
    RuleBuilder.Reason.MERCHANT_NOT_IN_SAMPLE -> R.string.re_err_merchant_missing
    RuleBuilder.Reason.OVERLAP -> R.string.re_err_overlap
}

@Composable
private fun TestResult(t: RuleEditorViewModel.Test) {
    val p = LocalPalette.current
    val (line, ok) = when (t) {
        RuleEditorViewModel.Test.Empty -> stringResource(R.string.re_test_empty) to false
        RuleEditorViewModel.Test.Ignored -> stringResource(R.string.re_test_ignored) to true
        RuleEditorViewModel.Test.NoMatch -> stringResource(R.string.re_test_no_match) to false
        is RuleEditorViewModel.Test.Match -> stringResource(R.string.re_test_ok, Money.format(t.parsed.amount), t.parsed.merchant, stringResource(directionLabel(t.parsed.direction))) to true
    }
    Text(line, style = Type.body.copy(color = if (ok) p.tintText else Base.warnStrong))
}
