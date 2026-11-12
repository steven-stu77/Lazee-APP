package io.github.steven_stu77.lazee.feature.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.SavingGoal
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.components.BudgetBar
import io.github.steven_stu77.lazee.core.ui.components.Chip
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.MoneyDialog
import io.github.steven_stu77.lazee.core.ui.components.SecondaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.add.DatePickDialog
import io.github.steven_stu77.lazee.feature.onboarding.filterMoney
import java.time.LocalDate

/** 存钱目标 (spec §5.6). Deposits are only records, never expenses. */
@Composable
fun GoalsSection(goals: List<GoalRow>, vm: BudgetViewModel) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf<SavingGoal?>(null) }
    var move by remember { mutableStateOf<Pair<SavingGoal, Int>?>(null) }

    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(stringResource(R.string.goals_title), style = Type.section, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.goals_add), style = Type.small.copy(color = p.accent, fontWeight = FontWeight.Medium),
            modifier = Modifier.clickable { editing = SavingGoal(name = "", target = 0) })
    }
    if (goals.isEmpty()) Text(stringResource(R.string.goals_empty), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
    goals.forEach { row ->
        val g = row.goal
        SectionCard {
            Row(Modifier.clickable { editing = g }) {
                Text(g.name, style = Type.section, modifier = Modifier.weight(1f))
                Text("${moneyText(row.view.saved)} / ${moneyText(g.target)}", style = Type.small)
            }
            BudgetBar(row.view.saved, g.target, normal = p.accent)
            val info = listOfNotNull(
                g.deadline?.let { stringResource(R.string.goals_deadline, Labels.shortDate(ctx, it) + if (it.year != LocalDate.now().year) " ${it.year}" else "") },
                row.perMonth?.takeIf { it > 0 }?.let { stringResource(R.string.goals_per_month, moneyText(it)) },
                if (row.view.saved >= g.target && g.target > 0) stringResource(R.string.goals_reached) else null,
            )
            if (info.isNotEmpty()) Text(info.joinToString(" · "), style = Type.tiny)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.goals_deposit), { move = g to 1 }, Modifier.weight(1f))
                SecondaryButton(stringResource(R.string.goals_withdraw), { move = g to -1 }, Modifier.weight(1f))
            }
        }
    }

    move?.let { (g, sign) ->
        MoneyDialog(stringResource(if (sign > 0) R.string.goals_deposit else R.string.goals_withdraw) + " · " + g.name, 0, { move = null },
            message = stringResource(R.string.goals_not_expense)) { cents -> if (cents > 0) vm.contribute(g.id, cents * sign) }
    }
    editing?.let { g -> GoalDialog(g, onDismiss = { editing = null }, onArchive = { vm.archiveGoal(g); editing = null }) { vm.saveGoal(it); editing = null } }
}

@Composable
private fun GoalDialog(initial: SavingGoal, onDismiss: () -> Unit, onArchive: () -> Unit, onSave: (SavingGoal) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var target by remember { mutableStateOf(Money.toInput(initial.target)) }
    var deadline by remember { mutableStateOf(initial.deadline) }
    var pickDate by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.goals_new else R.string.goals_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazeeTextField(name, { name = it.take(20) }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.goals_name_hint))
                LazeeTextField(target, { target = it.filterMoney() }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.goals_target), prefix = "RM", keyboardType = KeyboardType.Decimal)
                Chip(deadline?.let { Labels.shortDate(ctx, it) + " ${it.year}" } ?: stringResource(R.string.goals_pick_deadline), deadline != null, onClick = { pickDate = true })
                if (initial.id != 0L) TextButton(onArchive) { Text(stringResource(R.string.cats_archive)) }
            }
        },
        confirmButton = {
            val cents = Money.parse(target) ?: 0
            TextButton({ onSave(initial.copy(name = name.trim(), target = cents, deadline = deadline)) }, enabled = name.isNotBlank() && cents > 0) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (pickDate) DatePickDialog(deadline ?: LocalDate.now().plusMonths(6), { deadline = it; pickDate = false }, { deadline = null; pickDate = false })
}
