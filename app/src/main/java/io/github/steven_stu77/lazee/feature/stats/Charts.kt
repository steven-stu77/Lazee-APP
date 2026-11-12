package io.github.steven_stu77.lazee.feature.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.steven_stu77.lazee.core.logic.StatsMath
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import java.time.LocalDate
import java.time.YearMonth

/** Cumulative spend: solid this month, dashed last month, red dashed budget line. */
@Composable
fun PaceChart(thisMonth: List<Long>, lastMonth: List<Long>, budget: Long, days: Int, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val maxY = maxOf(thisMonth.maxOrNull() ?: 0, lastMonth.maxOrNull() ?: 0, budget, 1L).toFloat() * 1.08f
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width
        val h = size.height
        fun x(i: Int) = if (days <= 1) 0f else w * i / (days - 1)
        fun y(v: Long) = h - h * (v / maxY)
        // Baseline grid
        drawLine(Base.divider, Offset(0f, h), Offset(w, h), strokeWidth = 1.dp.toPx())
        if (budget > 0) drawLine(
            Base.danger, Offset(0f, y(budget)), Offset(w, y(budget)), strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        )
        fun line(values: List<Long>, color: Color, dashed: Boolean, width: Float) {
            if (values.isEmpty()) return
            val path = Path().apply {
                moveTo(x(0), y(values[0]))
                values.forEachIndexed { i, v -> if (i > 0) lineTo(x(i), y(v)) }
            }
            drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null))
        }
        line(lastMonth.take(days), Base.disabled, dashed = true, width = 2.dp.toPx())
        line(thisMonth, p.accent, dashed = false, width = 3.dp.toPx())
        thisMonth.lastOrNull()?.let { drawCircle(p.accent, 4.dp.toPx(), Offset(x(thisMonth.size - 1), y(it))) }
    }
}

/** Thin bar split into shares; colours follow the theme's chart scale. */
@Composable
fun ShareBar(shares: List<Float>, colors: List<Color>, height: Int = 12) {
    Row(Modifier.fillMaxWidth().height(height.dp).clip(RoundedCornerShape((height / 2).dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        shares.forEachIndexed { i, s ->
            if (s > 0f) Box(Modifier.weight(s).fillMaxHeight().background(colors[i.coerceAtMost(colors.size - 1)]))
        }
    }
}

/** Month calendar, Monday first; 5 heat levels; ring on today; dashed border for future days. */
@Composable
fun HeatCalendar(month: YearMonth, daily: Map<Int, Long>, today: LocalDate, weekLabels: List<String>) {
    val p = LocalPalette.current
    val heat = listOf(Base.divider) + p.heat
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = offset + month.lengthOfMonth()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            weekLabels.forEach { Text(it, style = Type.tiny.copy(fontSize = 11.sp), textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        (0 until (cells + 6) / 7).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                (0 until 7).forEach { col ->
                    val day = row * 7 + col - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            val future = date.isAfter(today)
                            val level = StatsMath.heatLevel(daily[day] ?: 0)
                            val shape = RoundedCornerShape(6.dp)
                            Box(
                                Modifier.fillMaxWidth().fillMaxHeight().clip(shape)
                                    .background(if (future) Color.Transparent else heat[level])
                                    .let {
                                        when {
                                            date == today -> it.border(2.dp, p.accent, shape)
                                            future -> it.drawBehind {
                                                drawRoundRect(Base.border, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                                                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))))
                                            }
                                            else -> it
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(day.toString(), style = Type.tiny.copy(fontSize = 11.sp, color = if (level == 4 && !future) Color.White else Base.textSub))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Two bars per month: income (light) and spending (dark); the unfinished month's spending is translucent. */
@Composable
fun SixMonthBars(months: List<io.github.steven_stu77.lazee.feature.stats.MonthPair>, labels: List<String>, currentIndex: Int) {
    val p = LocalPalette.current
    val max = months.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            months.forEachIndexed { i, m ->
                Row(Modifier.weight(1f).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.width(12.dp).fillMaxHeight(m.income.toFloat() / max).clip(RoundedCornerShape(4.dp)).background(p.soft))
                    Box(Modifier.width(12.dp).fillMaxHeight(m.expense.toFloat() / max).clip(RoundedCornerShape(4.dp)).background(p.deep.copy(alpha = if (i == currentIndex) 0.5f else 1f)))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEach { Text(it, style = Type.tiny.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal), textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
    }
}
