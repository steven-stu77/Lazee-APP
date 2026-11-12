package io.github.steven_stu77.lazee.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.Chip
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.components.LazeeSwitch
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.MenuItem
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.add.DatePickDialog
import io.github.steven_stu77.lazee.feature.home.DayGroupCard
import java.time.LocalDate

private enum class Picker { CATEGORY, ACCOUNT, FROM, TO }

@Composable
fun SearchScreen(onBack: () -> Unit, onTxn: (Long) -> Unit, vm: SearchViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val f = s.filter
    val ctx = LocalContext.current
    var picker by remember { mutableStateOf<Picker?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.search_title), onBack, stringResource(R.string.back))
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LazeeTextField(f.query, { q -> vm.update { it.copy(query = q) } }, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.search_hint),
                trailing = { Icon(LazeeIcons.Search, null, tint = Base.textSub) })
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.search_filters), showFilters || f.active, onClick = { showFilters = !showFilters })
                listOf(null to R.string.search_all, TxnType.EXPENSE to R.string.expense, TxnType.INCOME to R.string.income, TxnType.TRANSFER to R.string.transfer).forEach { (t, label) ->
                    Chip(stringResource(label), f.type == t, onClick = { vm.update { it.copy(type = t) } })
                }
            }
            if (showFilters) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(f.categoryId?.let { id -> categoryName(s.categories.firstOrNull { it.id == id }) } ?: stringResource(R.string.search_any_category), f.categoryId != null, onClick = { picker = Picker.CATEGORY })
                    Chip(f.accountId?.let { id -> s.accounts.firstOrNull { it.id == id }?.name } ?: stringResource(R.string.search_any_account), f.accountId != null, onClick = { picker = Picker.ACCOUNT })
                    Chip(f.from?.let { Labels.shortDate(ctx, it) } ?: stringResource(R.string.search_from), f.from != null, onClick = { picker = Picker.FROM })
                    Chip(f.to?.let { Labels.shortDate(ctx, it) } ?: stringResource(R.string.search_to), f.to != null, onClick = { picker = Picker.TO })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazeeTextField(f.minAmount, { v -> vm.update { it.copy(minAmount = v) } }, Modifier.weight(1f), placeholder = stringResource(R.string.search_min), prefix = "RM", keyboardType = KeyboardType.Decimal)
                    LazeeTextField(f.maxAmount, { v -> vm.update { it.copy(maxAmount = v) } }, Modifier.weight(1f), placeholder = stringResource(R.string.search_max), prefix = "RM", keyboardType = KeyboardType.Decimal)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(stringResource(R.string.search_auto_only), style = Type.body, modifier = Modifier.weight(1f))
                    LazeeSwitch(f.autoOnly, { v -> vm.update { it.copy(autoOnly = v) } })
                }
                if (f.active) TextButton(vm::clear) { Text(stringResource(R.string.search_clear)) }
            }
            if (f.query.isNotBlank() || f.active) Text(
                stringResource(R.string.search_summary, s.count, moneyText(s.totalExpense), moneyText(s.totalIncome)), style = Type.small,
            )
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(s.groups, key = { it.date.toEpochDay() }) { g -> DayGroupCard(g, emptySet(), onTxn, null) }
        }
    }

    when (picker) {
        Picker.CATEGORY -> ListPicker(stringResource(R.string.txn_category), listOf(null to stringResource(R.string.search_any_category)) + s.categories.map { it.id to categoryName(it) }, { picker = null }) { id -> vm.update { it.copy(categoryId = id) } }
        Picker.ACCOUNT -> ListPicker(stringResource(R.string.txn_account), listOf(null to stringResource(R.string.search_any_account)) + s.accounts.map { it.id to it.name }, { picker = null }) { id -> vm.update { it.copy(accountId = id) } }
        Picker.FROM -> DatePickDialog(f.from ?: LocalDate.now().withDayOfMonth(1), { d -> vm.update { it.copy(from = d) }; picker = null }, { vm.update { it.copy(from = null) }; picker = null })
        Picker.TO -> DatePickDialog(f.to ?: LocalDate.now(), { d -> vm.update { it.copy(to = d) }; picker = null }, { vm.update { it.copy(to = null) }; picker = null })
        null -> {}
    }
}

@Composable
fun ListPicker(title: String, options: List<Pair<Long?, String>>, onDismiss: () -> Unit, onPick: (Long?) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { LazyColumn { items(options) { (id, label) -> MenuItem(label) { onPick(id); onDismiss() } } } },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
