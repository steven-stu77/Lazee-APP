package io.github.steven_stu77.lazee.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.AccountType
import io.github.steven_stu77.lazee.core.ui.components.Chip
import io.github.steven_stu77.lazee.core.ui.components.LazeeSwitch
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type

fun accountTypeName(t: AccountType): Int = when (t) {
    AccountType.CASH -> R.string.acc_type_cash
    AccountType.BANK -> R.string.acc_type_bank
    AccountType.EWALLET -> R.string.acc_type_ewallet
    AccountType.CREDIT_CARD -> R.string.acc_type_card
}

@Composable
fun AccountEditScreen(onBack: () -> Unit, vm: AccountEditViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.saved) { if (s.saved) onBack() }
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(if (s.original == null) R.string.acc_new else R.string.acc_edit), onBack, stringResource(R.string.back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                Text(stringResource(R.string.acc_name), style = Type.small)
                LazeeTextField(s.name, vm::setName, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.acc_name_hint))
                Text(stringResource(R.string.cat_type), style = Type.small)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccountType.entries.forEach { t -> Chip(stringResource(accountTypeName(t)), s.type == t, onClick = { vm.setType(t) }) }
                }
                Text(stringResource(if (s.type == AccountType.CREDIT_CARD) R.string.acc_opening_card else R.string.acc_opening), style = Type.small)
                LazeeTextField(s.opening, vm::setOpening, Modifier.fillMaxWidth(), placeholder = "0.00", prefix = "RM", keyboardType = KeyboardType.Decimal)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(if (s.type == AccountType.CREDIT_CARD) R.string.acc_owed else R.string.acc_negative), style = Type.body, modifier = Modifier.weight(1f))
                    LazeeSwitch(s.negativeOpening, vm::setNegative)
                }
            }
            if (s.type == AccountType.CREDIT_CARD) SectionCard {
                Text(stringResource(R.string.acc_statement_day), style = Type.small)
                LazeeTextField(s.statementDay, vm::setStatementDay, Modifier.fillMaxWidth(), placeholder = "25", keyboardType = KeyboardType.Number)
                Text(stringResource(R.string.acc_due_day), style = Type.small)
                LazeeTextField(s.dueDay, vm::setDueDay, Modifier.fillMaxWidth(), placeholder = "15", keyboardType = KeyboardType.Number)
            }
            if (s.original != null) TextButton({ vm.save(archive = !(s.original?.archived ?: false)) }) {
                Text(stringResource(if (s.original?.archived == true) R.string.acc_unarchive else R.string.acc_archive), color = Base.dangerText)
            }
        }
        PrimaryButton(stringResource(R.string.save), { vm.save() }, Modifier.fillMaxWidth().padding(20.dp), enabled = s.canSave)
    }
}
