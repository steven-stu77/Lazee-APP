package io.github.steven_stu77.lazee.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.AccountType
import io.github.steven_stu77.lazee.core.ui.components.HeroCard
import io.github.steven_stu77.lazee.core.ui.components.SecondaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.home.DayGroupCard

@Composable
fun AccountDetailScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, onTxn: (Long) -> Unit, vm: AccountDetailViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val a = s.account ?: return
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(a.name, onBack, stringResource(R.string.back)) {
            TextButton({ onEdit(a.id) }) { Text(stringResource(R.string.edit)) }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                HeroCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(accountTypeName(a.type)) + if (a.archived) " · " + stringResource(R.string.acc_archived) else "", style = Type.small.copy(color = p.sub))
                        Text(moneyText(s.balance), style = Type.heroAmount.copy(color = Color.White))
                    }
                }
            }
            if (a.type == AccountType.CREDIT_CARD) item {
                SectionCard {
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.acc_statement_day), style = Type.small, modifier = Modifier.weight(1f))
                        Text(a.statementDay?.let { stringResource(R.string.acc_day_n, it) } ?: "—", style = Type.body)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.acc_due_day), style = Type.small, modifier = Modifier.weight(1f))
                        Text(a.dueDay?.let { stringResource(R.string.acc_day_n, it) } ?: "—", style = Type.body)
                    }
                    s.amountDue?.let {
                        Row(Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.acc_due_this_period), style = Type.section, modifier = Modifier.weight(1f))
                            Text(moneyText(it), style = Type.section)
                        }
                    }
                    if (a.statementDay == null || a.dueDay == null) Text(stringResource(R.string.acc_set_days_hint), style = Type.tiny)
                    else if (!s.hasBill) SecondaryButton(stringResource(R.string.acc_make_card_bill), vm::createCardBill, Modifier.fillMaxWidth())
                }
            }
            if (s.groups.isEmpty()) item { Text(stringResource(R.string.acc_no_records), style = Type.small) }
            items(s.groups, key = { it.date.toEpochDay() }) { g -> DayGroupCard(g, emptySet(), onTxn, null) }
        }
    }
}
