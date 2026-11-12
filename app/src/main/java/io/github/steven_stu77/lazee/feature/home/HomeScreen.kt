package io.github.steven_stu77.lazee.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.components.BannerKind
import io.github.steven_stu77.lazee.core.ui.components.HeroCard
import io.github.steven_stu77.lazee.core.ui.components.InfoBanner
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.components.MenuItem
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import java.time.YearMonth

@Composable
fun HomeScreen(
    onSearch: () -> Unit,
    onPending: () -> Unit,
    onBills: () -> Unit,
    onBudget: () -> Unit,
    onTxn: (Long) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var pickCategory by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        if (s.selection.isNotEmpty()) SelectionBar(s.selection.size, vm::clearSelection, vm::deleteSelected) { pickCategory = true }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Header(s, onSearch, vm::toggleHide) }
            item { Hero(s, onBudget) }
            if (s.pendingCount > 0) item {
                InfoBanner(stringResource(R.string.home_pending, s.pendingCount), BannerKind.WARN, icon = LazeeIcons.Bell, onClick = onPending)
            }
            s.nextBill?.let { b ->
                item {
                    val days = io.github.steven_stu77.lazee.core.logic.BillMath.daysUntil(s.today, b.due)
                    val whenText = when {
                        days < 0 -> stringResource(R.string.bill_overdue)
                        days == 0L -> stringResource(R.string.bill_due_today)
                        else -> stringResource(R.string.bill_due_in, days)
                    }
                    InfoBanner("$whenText · ${b.bill.name}", BannerKind.THEME, icon = LazeeIcons.Calendar, trailing = moneyText(b.bill.amount), onClick = onBills)
                }
            }
            if (s.groups.isEmpty()) item { EmptyState() }
            items(s.groups, key = { it.date.toEpochDay() }) { g ->
                DayGroupCard(g, s.selection, onClick = { id -> if (s.selection.isNotEmpty()) vm.toggleSelect(id) else onTxn(id) }, onLongClick = vm::toggleSelect)
            }
            if (s.canLoadEarlier) item {
                val ctx = LocalContext.current
                Text(
                    stringResource(R.string.home_load_earlier, Labels.monthShort(ctx, s.oldest.minusMonths(1))),
                    style = Type.small.copy(color = LocalPalette.current.accent, fontWeight = FontWeight.Medium),
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = vm::loadEarlier).padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
    if (pickCategory) AlertDialog(
        onDismissRequest = { pickCategory = false },
        title = { Text(stringResource(R.string.home_change_category)) },
        text = {
            LazyColumn {
                items(s.categories) { c ->
                    MenuItem(categoryName(c)) { vm.setCategoryForSelected(c.id); pickCategory = false }
                }
            }
        },
        confirmButton = { TextButton({ pickCategory = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun Header(s: HomeUiState, onSearch: () -> Unit, onToggleHide: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val ym = YearMonth.from(s.today)
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val zh = Labels.isZh(ctx)
                Text(if (zh) ym.monthValue.toString() else Labels.monthShort(ctx, ym), style = Type.bigMonth)
                if (zh) Text("月", style = Type.section.copy(fontSize = 16.sp), modifier = Modifier.padding(bottom = 6.dp))
                Box(Modifier.padding(bottom = 12.dp).size(4.dp).clip(CircleShape).background(p.accent))
                Text(ym.year.toString(), style = Type.small, modifier = Modifier.padding(bottom = 6.dp))
            }
            // Decorative: Mon–Sun, filled when something was logged, ring on today.
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                s.weekDots.forEach { (d, logged) ->
                    Box(
                        Modifier.size(8.dp).clip(CircleShape)
                            .background(if (logged) p.accent else Base.switchOff)
                            .let { if (d == s.today) it.border(1.5.dp, p.deep, CircleShape) else it },
                    )
                }
            }
        }
        RoundIconButton(LazeeIcons.Search, stringResource(R.string.search_title), onSearch)
        Box(Modifier.size(8.dp))
        RoundIconButton(if (s.hideAmounts) LazeeIcons.EyeOff else LazeeIcons.Eye, stringResource(R.string.hide_amounts), onToggleHide)
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Base.card).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Base.text, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun Hero(s: HomeUiState, onBudget: () -> Unit) {
    val p = LocalPalette.current
    HeroCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.home_month_spend), style = Type.small.copy(color = p.sub))
            Text(moneyText(s.monthExpense), style = Type.heroAmount.copy(color = Color.White))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (s.budget > 0) {
                val ratio = (s.monthExpense.toFloat() / s.budget).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))) {
                    Box(Modifier.fillMaxWidth(ratio).height(8.dp).clip(CircleShape).background(if (s.budgetLeft < 0) Base.danger else p.glow))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                if (s.budget > 0) {
                    val left = if (s.budgetLeft >= 0) "${stringResource(R.string.home_budget_left)} ${moneyText(s.budgetLeft)}"
                    else "${stringResource(R.string.over_budget)} ${moneyText(-s.budgetLeft)}"
                    Text(left, style = Type.small.copy(color = p.sub2), modifier = Modifier.weight(1f))
                } else {
                    Text(
                        stringResource(R.string.home_set_budget), style = Type.small.copy(color = p.glow, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f).clickable(role = Role.Button, onClick = onBudget),
                    )
                }
                Text("${stringResource(R.string.income)} ${moneyText(s.monthIncome)}", style = Type.small.copy(color = p.sub2))
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.home_empty_title), style = Type.section)
        Text(stringResource(R.string.home_empty_body), style = Type.small)
    }
}

@Composable
private fun SelectionBar(count: Int, onCancel: () -> Unit, onDelete: () -> Unit, onCategory: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().background(Base.card).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(LazeeIcons.Close, stringResource(R.string.cancel), onCancel)
        Text(stringResource(R.string.home_selected, count), style = Type.section, modifier = Modifier.weight(1f).padding(start = 8.dp))
        TextButton(onCategory) { Text(stringResource(R.string.home_change_category)) }
        TextButton({ confirm = true }) { Text(stringResource(R.string.delete), color = Base.dangerText) }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.home_delete_confirm, count)) },
        confirmButton = { TextButton({ confirm = false; onDelete() }) { Text(stringResource(R.string.delete), color = Base.dangerText) } },
        dismissButton = { TextButton({ confirm = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Format helper for headers. */
@Composable
fun spentLabel(cents: Long): String = "${stringResource(R.string.spent)} ${moneyText(cents)}"
