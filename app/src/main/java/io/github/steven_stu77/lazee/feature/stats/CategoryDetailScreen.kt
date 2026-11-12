package io.github.steven_stu77.lazee.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.BudgetBar
import io.github.steven_stu77.lazee.core.ui.components.CategoryBadge
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.home.DayGroupCard
import kotlin.math.roundToInt

@Composable
fun CategoryDetailScreen(onBack: () -> Unit, onTxn: (Long) -> Unit, vm: CategoryDetailViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = s.category ?: return
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(Labels.monthTitle(ctx, s.month), onBack, stringResource(R.string.back))
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                SectionCard(padding = PaddingValues(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryBadge(categoryName(c), c.colorIndex, size = 48.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(categoryName(c), style = Type.section)
                            Text(stringResource(R.string.cd_share, (s.share * 100).roundToInt()), style = Type.small)
                        }
                        Text(moneyText(s.amount), style = Type.pageTitle.copy(fontSize = Type.section.fontSize * 1.4f))
                    }
                    if (s.budget > 0) {
                        BudgetBar(s.amount, s.budget)
                        Text(stringResource(R.string.budget_used_of, moneyText(s.amount), moneyText(s.budget)), style = Type.tiny)
                    }
                }
            }
            item {
                SectionCard(padding = PaddingValues(20.dp)) {
                    Text(stringResource(R.string.stats_six_months), style = Type.section)
                    SixMonthBars(s.sixMonths, s.sixMonths.map { Labels.monthShort(ctx, it.month) }, -1)
                }
            }
            if (s.groups.isEmpty()) item { Text(stringResource(R.string.stats_no_data), style = Type.small, modifier = Modifier.fillMaxWidth()) }
            items(s.groups, key = { it.date.toEpochDay() }) { g -> DayGroupCard(g, emptySet(), onTxn, null) }
        }
    }
}
