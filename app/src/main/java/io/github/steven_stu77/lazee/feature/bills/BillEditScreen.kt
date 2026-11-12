package io.github.steven_stu77.lazee.feature.bills

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
import io.github.steven_stu77.lazee.core.data.BillMode
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.Chip
import io.github.steven_stu77.lazee.core.ui.components.LazeeSwitch
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SegmentedControl
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type

@Composable
fun BillEditScreen(onBack: () -> Unit, vm: BillEditViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.done) { if (s.done) onBack() }
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(if (s.original == null) R.string.bill_new else R.string.bill_edit), onBack, stringResource(R.string.back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionCard {
                Label(R.string.acc_name)
                LazeeTextField(s.name, { v -> vm.update { it.copy(name = v.take(24)) } }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.bill_name_hint))
                Label(R.string.bill_amount)
                LazeeTextField(s.amount, vm::setAmount, Modifier.fillMaxWidth(), placeholder = "0.00", prefix = "RM", keyboardType = KeyboardType.Decimal)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Label(R.string.bill_day)
                        LazeeTextField(s.day, vm::setDay, Modifier.fillMaxWidth(), placeholder = "15", keyboardType = KeyboardType.Number)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Label(R.string.bill_remind)
                        LazeeTextField(s.remind, vm::setRemind, Modifier.fillMaxWidth(), placeholder = "2", keyboardType = KeyboardType.Number)
                    }
                }
                Text(stringResource(R.string.bill_day_hint), style = Type.tiny)
            }
            SectionCard {
                Label(R.string.txn_category)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.categories.forEach { c -> Chip(categoryName(c), c.id == s.categoryId, onClick = { vm.update { it.copy(categoryId = c.id) } }) }
                }
                Label(R.string.bill_account)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(stringResource(R.string.bill_account_any), s.accountId == null, onClick = { vm.update { it.copy(accountId = null) } })
                    s.accounts.forEach { a -> Chip(a.name, a.id == s.accountId, onClick = { vm.update { it.copy(accountId = a.id) } }) }
                }
                Label(R.string.bill_mode)
                SegmentedControl(listOf(stringResource(R.string.bill_mode_remind), stringResource(R.string.bill_mode_auto)), if (s.mode == BillMode.REMIND) 0 else 1,
                    { i -> vm.update { it.copy(mode = if (i == 0) BillMode.REMIND else BillMode.AUTO_RECORD) } }, Modifier.fillMaxWidth())
                Text(stringResource(if (s.mode == BillMode.REMIND) R.string.bill_mode_remind_hint else R.string.bill_mode_auto_hint), style = Type.tiny)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.bill_is_subscription), style = Type.body, modifier = Modifier.weight(1f))
                    LazeeSwitch(s.subscription, { v -> vm.update { it.copy(subscription = v) } })
                }
                Label(R.string.bill_installments)
                LazeeTextField(s.installments, vm::setInstallments, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.bill_installments_hint), keyboardType = KeyboardType.Number)
            }
            if (s.original != null) TextButton(vm::delete) { Text(stringResource(R.string.delete), color = Base.dangerText) }
        }
        PrimaryButton(stringResource(R.string.save), vm::save, Modifier.fillMaxWidth().padding(20.dp), enabled = s.canSave)
    }
}

@Composable
private fun Label(id: Int) = Text(stringResource(id), style = Type.small)
