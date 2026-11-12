package io.github.steven_stu77.lazee.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.HeroCard
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import kotlin.math.roundToInt

@Composable
fun YearScreen(onBack: () -> Unit, vm: YearViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val p = LocalPalette.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.stats_year_title, s.year), onBack, stringResource(R.string.back))
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HeroCard {
                Text(stringResource(R.string.year_saved), style = Type.small.copy(color = p.sub))
                Text(moneyText(s.saved), style = Type.heroAmount.copy(color = Color.White))
                Row {
                    Text("${stringResource(R.string.income)} ${moneyText(s.income)}", style = Type.small.copy(color = p.sub2), modifier = Modifier.weight(1f))
                    Text("${stringResource(R.string.spent)} ${moneyText(s.expense)}", style = Type.small.copy(color = p.sub2))
                }
            }
            SectionCard(padding = PaddingValues(20.dp)) {
                Text(stringResource(R.string.year_by_month), style = Type.section)
                SixMonthBars(s.months, s.months.map { if (Labels.isZh(ctx)) "${it.month.monthValue}" else Labels.monthShort(ctx, it.month).take(1) }, -1)
            }
            SectionCard(padding = PaddingValues(20.dp)) {
                Text(stringResource(R.string.year_top_categories), style = Type.section)
                s.topCategories.forEachIndexed { i, c -> RankRow(i + 1, categoryName(c.category), "${(c.share * 100).roundToInt()}%", moneyText(c.amount)) }
                if (s.topCategories.isEmpty()) Text(stringResource(R.string.stats_no_data), style = Type.small)
            }
            SectionCard(padding = PaddingValues(20.dp)) {
                Text(stringResource(R.string.stats_merchants), style = Type.section)
                s.topMerchants.forEachIndexed { i, m -> RankRow(i + 1, m.name, stringResource(R.string.stats_times, m.count), moneyText(m.total)) }
                if (s.topMerchants.isEmpty()) Text(stringResource(R.string.stats_no_data), style = Type.small)
            }
        }
    }
}

@Composable
private fun RankRow(rank: Int, name: String, sub: String, amount: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Text("$rank", style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Base.textSub), modifier = Modifier.padding(end = 10.dp))
        Text("$name ", style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium))
        Text(sub, style = Type.small, modifier = Modifier.weight(1f))
        Text(amount, style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    }
}
