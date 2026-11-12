package io.github.steven_stu77.lazee.feature.stats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import kotlin.math.roundToInt

@Composable
fun DailyHeat(s: StatsUiState) {
    val p = LocalPalette.current
    SectionCard(padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.stats_daily), style = Type.section, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.stats_no_spend_days, s.noSpendDays), style = Type.small.copy(color = p.tintText, fontWeight = FontWeight.SemiBold))
        }
        val labels = listOf(R.string.wd_mon, R.string.wd_tue, R.string.wd_wed, R.string.wd_thu, R.string.wd_fri, R.string.wd_sat, R.string.wd_sun).map { stringResource(it) }
        HeatCalendar(s.month, s.daily, s.today, labels)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.stats_less), style = Type.tiny.copy(fontSize = 11.sp))
            ShareBar(listOf(1f, 1f, 1f, 1f, 1f), listOf(Base.divider) + p.heat, height = 10)
            Text(stringResource(R.string.stats_more), style = Type.tiny.copy(fontSize = 11.sp))
        }
    }
}

@Composable
fun Habits(s: StatsUiState) {
    val p = LocalPalette.current
    Text(stringResource(R.string.stats_habits), style = Type.section, modifier = Modifier.padding(horizontal = 4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HabitCard(stringResource(R.string.stats_paid_with), Modifier.weight(1f)) {
            if (s.payShares.isEmpty()) Text("—", style = Type.small)
            else {
                ShareBar(s.payShares.map { it.second }, listOf(p.deep, p.accent, p.soft) + p.chart.drop(3), height = 8)
                Text(s.payShares.take(3).joinToString("\n") { "${it.first} ${(it.second * 100).roundToInt()}%" }, style = Type.tiny.copy(color = Base.text))
            }
        }
        HabitCard(stringResource(R.string.stats_week_vs), Modifier.weight(1f)) {
            val w = s.week
            val headline = when {
                w.weekdayAvg == 0L && w.weekendAvg == 0L -> "—"
                w.weekdayAvg == 0L -> stringResource(R.string.stats_weekend_only)
                else -> {
                    val pct = ((w.weekendAvg - w.weekdayAvg) * 100.0 / w.weekdayAvg).roundToInt()
                    if (pct >= 0) stringResource(R.string.stats_weekend_more, pct) else stringResource(R.string.stats_weekend_less, -pct)
                }
            }
            Text(headline, style = Type.section.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Base.warnStrong))
            Text(stringResource(R.string.stats_week_detail, moneyText(w.weekdayAvg), moneyText(w.weekendAvg)), style = Type.tiny.copy(color = Base.text))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HabitCard(stringResource(R.string.stats_fixed_vs), Modifier.weight(1f)) {
            val pct = if (s.expense > 0) (s.fixed * 100.0 / s.expense).roundToInt() else 0
            Text(stringResource(R.string.stats_fixed_pct, pct), style = Type.section.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
            Text(stringResource(R.string.stats_fixed_detail, moneyText(s.fixed), moneyText(s.expense - s.fixed)), style = Type.tiny.copy(color = Base.text))
        }
        HabitCard(stringResource(R.string.stats_small), Modifier.weight(1f)) {
            Text(moneyText(s.small.total), style = Type.section.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
            val days = "%.1f".format(s.small.budgetDays)
            Text(
                if (s.budget > 0) stringResource(R.string.stats_small_detail, s.small.count, days) else stringResource(R.string.stats_small_count, s.small.count),
                style = Type.tiny.copy(color = Base.text),
            )
        }
    }
}

@Composable
private fun HabitCard(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    SectionCard(modifier, padding = PaddingValues(14.dp)) {
        Text(title, style = Type.tiny)
        content()
    }
}

@Composable
fun SixMonths(s: StatsUiState) {
    val ctx = LocalContext.current
    SectionCard(padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
        Text(stringResource(R.string.stats_six_months), style = Type.section)
        SixMonthBars(s.sixMonths, s.sixMonths.map { Labels.monthShort(ctx, it.month) }, if (s.isCurrentMonth) s.sixMonths.lastIndex else -1)
        val p = LocalPalette.current
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(p.soft, stringResource(R.string.income))
            LegendDot(p.deep, stringResource(if (s.isCurrentMonth) R.string.stats_spent_so_far else R.string.spent))
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ShareBar(listOf(1f), listOf(color), height = 8)
        Text(label, style = Type.tiny)
    }
}

@Composable
fun YearCard(s: StatsUiState, onYear: (Int) -> Unit) {
    val p = LocalPalette.current
    SectionCard(
        Modifier.let { if (s.yearOpen) it.clickable { onYear(s.month.year) } else it },
        padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.stats_year_title, s.month.year), style = Type.section)
                Text(stringResource(if (s.yearOpen) R.string.stats_year_open else R.string.stats_year_locked), style = Type.small)
            }
            if (s.yearOpen) Icon(LazeeIcons.ChevronRight, null, tint = p.accent)
        }
    }
}

