package io.github.steven_stu77.lazee.feature.stats

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.PlainBar
import io.github.steven_stu77.lazee.core.ui.components.CategoryBadge
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.roundToInt

private val cardPad = PaddingValues(horizontal = 20.dp, vertical = 18.dp)

@Composable
fun StatsScreen(onCategory: (Long, YearMonth) -> Unit, onYear: (Int) -> Unit, vm: StatsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.nav_stats), style = Type.pageTitle, modifier = Modifier.weight(1f))
            MonthSwitcher(Labels.monthTitle(ctx, s.month), vm::prev, if (s.canGoNext) vm::next else null)
        }
        Overview(s)
        Pace(s)
        Categories(s, onCategory)
        Merchants(s)
        DailyHeat(s)
        Habits(s)
        SixMonths(s)
        YearCard(s, onYear)
    }
}

@Composable
private fun MonthSwitcher(label: String, onPrev: () -> Unit, onNext: (() -> Unit)?) {
    Row(Modifier.clip(RoundedCornerShape(16.dp)).background(Base.card), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).clickable(role = Role.Button, onClickLabel = stringResource(R.string.stats_prev_month), onClick = onPrev), contentAlignment = Alignment.Center) {
            Icon(LazeeIcons.ChevronLeft, stringResource(R.string.stats_prev_month), tint = Base.text, modifier = Modifier.size(16.dp))
        }
        Text(label, style = Type.body.copy(fontSize = 14.sp))
        Box(Modifier.size(36.dp).clickable(enabled = onNext != null, role = Role.Button, onClickLabel = stringResource(R.string.stats_next_month)) { onNext?.invoke() }, contentAlignment = Alignment.Center) {
            Icon(LazeeIcons.ChevronRight, stringResource(R.string.stats_next_month), tint = if (onNext != null) Base.text else Base.switchOff, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun Overview(s: StatsUiState) {
    val p = LocalPalette.current
    SectionCard(padding = cardPad) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(stringResource(R.string.spent), moneyText(s.expense), Modifier.weight(1f))
            Stat(stringResource(R.string.income), moneyText(s.income), Modifier.weight(1f), p.tintText)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.stats_saved), style = Type.tiny)
                Text(moneyText(s.income - s.expense), style = Type.bodyStrong.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Text(stringResource(R.string.stats_rate, (s.savingsRate * 100).roundToInt()), style = Type.tiny.copy(color = p.tintText, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color = Base.text) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = Type.tiny)
        Text(value, style = Type.bodyStrong.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color))
    }
}

@Composable
private fun Pace(s: StatsUiState) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    SectionCard(padding = cardPad) {
        Text(stringResource(R.string.stats_pace), style = Type.section)
        val cmp = if (s.diffVsLast >= 0) stringResource(R.string.stats_more_than_last, moneyText(s.diffVsLast)) else stringResource(R.string.stats_less_than_last, moneyText(abs(s.diffVsLast)))
        val end = when {
            !s.isCurrentMonth -> ""
            s.budget <= 0 -> stringResource(R.string.stats_forecast, moneyText(s.forecast))
            s.overBudget -> stringResource(R.string.stats_forecast_over, moneyText(s.forecast))
            else -> stringResource(R.string.stats_forecast_ok, moneyText(s.forecast))
        }
        Text("$cmp$end", style = Type.small.copy(color = if (s.overBudget && s.isCurrentMonth) Base.warnStrong else Base.textSub, fontWeight = if (s.overBudget) FontWeight.SemiBold else FontWeight.Normal))
        PaceChart(s.paceThis, s.paceLast, s.budget, s.month.lengthOfMonth())
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Legend(p.accent, Labels.monthShort(ctx, s.month), dashed = false)
            Legend(Base.disabled, Labels.monthShort(ctx, s.month.minusMonths(1)), dashed = true)
            if (s.budget > 0) Legend(Base.danger, stringResource(R.string.nav_budget), dashed = true)
        }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(16.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(if (dashed) color.copy(alpha = 0.6f) else color))
        Text(label, style = Type.tiny)
    }
}

@Composable
private fun Categories(s: StatsUiState, onCategory: (Long, YearMonth) -> Unit) {
    val p = LocalPalette.current
    SectionCard(padding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.txn_category), style = Type.section, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.stats_tap_hint), style = Type.tiny)
        }
        if (s.categories.isEmpty()) Text(stringResource(R.string.stats_no_data), style = Type.small)
        else ShareBar(s.categories.map { it.share }, p.chart)
        s.categories.forEach { c ->
            Row(
                Modifier.fillMaxWidth().clickable { onCategory(c.category.id, s.month) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CategoryBadge(categoryName(c.category), c.category.colorIndex, size = 34.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text("${categoryName(c.category)} ", style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium))
                        Text("${(c.share * 100).roundToInt()}%", style = Type.small, modifier = Modifier.weight(1f))
                        Text(moneyText(c.amount), style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                    }
                    PlainBar(c.share / s.categories.first().share)
                }
            }
        }
    }
}

@Composable
private fun Merchants(s: StatsUiState) {
    val p = LocalPalette.current
    SectionCard(padding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)) {
        Text(stringResource(R.string.stats_merchants), style = Type.section)
        Text(stringResource(R.string.stats_merchants_hint), style = Type.tiny)
        if (s.merchants.isEmpty()) Text(stringResource(R.string.stats_no_data), style = Type.small)
        val top = s.merchants.firstOrNull()?.total ?: 1
        s.merchants.forEachIndexed { i, m ->
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Base.textSub), modifier = Modifier.width(20.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text("${m.name} ", style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium))
                        Text(stringResource(R.string.stats_times, m.count), style = Type.small, modifier = Modifier.weight(1f))
                        Text(moneyText(m.total), style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
                    }
                    PlainBar(m.total.toFloat() / top)
                }
            }
        }
    }
}
