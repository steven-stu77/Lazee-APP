package io.github.steven_stu77.lazee.feature.loans

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.LoanDirection
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.repo.LoanView
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.components.Chip
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.MoneyDialog
import io.github.steven_stu77.lazee.core.ui.components.PlainBar
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SecondaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SegmentedControl
import io.github.steven_stu77.lazee.core.ui.components.StatusTag
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.add.DatePickDialog
import io.github.steven_stu77.lazee.feature.onboarding.filterMoney
import java.time.LocalDate

@Composable
fun LoansScreen(onBack: () -> Unit, vm: LoansViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var repaying by remember { mutableStateOf<LoanView?>(null) }
    val lent = s.direction == LoanDirection.LENT
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.loans_title), onBack, stringResource(R.string.back))
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SegmentedControl(listOf(stringResource(R.string.loans_lent), stringResource(R.string.loans_borrowed)), if (lent) 0 else 1,
                    { vm.setDirection(if (it == 0) LoanDirection.LENT else LoanDirection.BORROWED) }, Modifier.fillMaxWidth())
            }
            item { Text(stringResource(if (lent) R.string.loans_owed_to_you else R.string.loans_you_owe, moneyText(s.outstanding)), style = Type.section) }
            item { Text(stringResource(R.string.loans_note), style = Type.tiny) }
            items(s.loans, key = { it.loan.id }) { v -> LoanCard(v) { repaying = v } }
            item { PrimaryButton(stringResource(R.string.loans_new), { creating = true }, Modifier.fillMaxWidth()) }
        }
    }
    if (creating) NewLoanDialog(s.accounts, onDismiss = { creating = false }) { person, amount, rate, due, acc -> vm.create(person, amount, rate, due, acc); creating = false }
    repaying?.let { v ->
        MoneyDialog(stringResource(R.string.loans_repay) + " · " + v.loan.person, v.outstanding, { repaying = null },
            message = stringResource(R.string.loans_repay_hint)) { if (it > 0) vm.repay(v.loan.id, it) }
    }
}

@Composable
private fun LoanCard(v: LoanView, onRepay: () -> Unit) {
    val ctx = LocalContext.current
    val l = v.loan
    SectionCard {
        Row {
            Text(l.person, style = Type.section, modifier = Modifier.weight(1f))
            if (l.settled) StatusTag(stringResource(R.string.loans_settled), highlighted = true)
            else Text(moneyText(v.outstanding), style = Type.body.copy(fontWeight = FontWeight.Bold))
        }
        val total = l.principal + v.interest
        PlainBar(if (total > 0) v.repaid.toFloat() / total else 0f)
        Text(
            listOfNotNull(
                stringResource(R.string.loans_principal, moneyText(l.principal)),
                l.annualRatePercent?.let { stringResource(R.string.loans_rate, "%.2f".format(it)) },
                if (v.interest > 0) stringResource(R.string.loans_interest, moneyText(v.interest)) else null,
                stringResource(R.string.loans_repaid, moneyText(v.repaid)),
            ).joinToString(" · "),
            style = Type.tiny,
        )
        Text(
            stringResource(R.string.loans_dates, Labels.shortDate(ctx, l.startDate), l.dueDate?.let { Labels.shortDate(ctx, it) } ?: "—"),
            style = Type.tiny,
        )
        if (!l.settled) SecondaryButton(stringResource(R.string.loans_repay), onRepay, Modifier.fillMaxWidth())
    }
}

@Composable
private fun NewLoanDialog(accounts: List<Account>, onDismiss: () -> Unit, onSave: (String, Long, Double?, LocalDate?, Long) -> Unit) {
    var person by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var due by remember { mutableStateOf<LocalDate?>(null) }
    var account by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var pickDate by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.loans_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LazeeTextField(person, { person = it.take(24) }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.loans_person))
                LazeeTextField(amount, { amount = it.filterMoney() }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.loans_amount), prefix = "RM", keyboardType = KeyboardType.Decimal)
                LazeeTextField(rate, { rate = it.filter { c -> c.isDigit() || c == '.' }.take(5) }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.loans_rate_hint), keyboardType = KeyboardType.Decimal)
                Chip(due?.let { Labels.shortDate(ctx, it) + " ${it.year}" } ?: stringResource(R.string.loans_due_optional), due != null, onClick = { pickDate = true })
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { a -> Chip(a.name, a.id == account, onClick = { account = a.id }) }
                }
            }
        },
        confirmButton = {
            val cents = Money.parse(amount) ?: 0
            TextButton({ onSave(person, cents, rate.toDoubleOrNull(), due, account!!) }, enabled = person.isNotBlank() && cents > 0 && account != null) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (pickDate) DatePickDialog(due ?: LocalDate.now().plusMonths(1), { due = it; pickDate = false }, { due = null; pickDate = false })
}
