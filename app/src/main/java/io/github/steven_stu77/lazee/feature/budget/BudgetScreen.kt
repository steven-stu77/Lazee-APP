package io.github.steven_stu77.lazee.feature.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.BudgetBar
import io.github.steven_stu77.lazee.core.ui.components.MoneyDialog
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.search.ListPicker

@Composable
fun BudgetScreen(vm: BudgetViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val p = LocalPalette.current
    var editTotal by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<Category?>(null) }
    var pickCategory by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.nav_budget), style = Type.pageTitle)
        SectionCard(padding = PaddingValues(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(if (s.left >= 0) R.string.budget_left_to_spend else R.string.over_budget), style = Type.small)
                    Text(moneyText(kotlin.math.abs(s.left)), style = Type.pageTitle.copy(fontSize = 28.sp, color = if (s.left < 0) Base.dangerText else Base.text))
                }
                Text(
                    if (s.total > 0) "${stringResource(R.string.budget_total)} ${moneyText(s.total)}" else stringResource(R.string.home_set_budget),
                    style = Type.small.copy(color = p.accent, fontWeight = FontWeight.Medium),
                    modifier = Modifier.clickable(role = Role.Button) { editTotal = true }.padding(4.dp),
                )
            }
            BudgetBar(s.spent, s.total, Modifier.height(10.dp))
            if (s.total > 0) Text(
                stringResource(R.string.budget_per_day, s.daysLeft, moneyText(s.perDay)),
                style = Type.small.copy(color = p.tintText),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.tint).padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }

        Text(stringResource(R.string.budget_categories), style = Type.section, modifier = Modifier.padding(horizontal = 4.dp))
        SectionCard(padding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
            s.rows.forEachIndexed { i, r ->
                if (i > 0) HorizontalDivider(color = Base.divider)
                CategoryBudgetRowView(r) { editCategory = r.category }
            }
            if (s.rows.isNotEmpty()) HorizontalDivider(color = Base.divider)
            Text(
                stringResource(R.string.budget_add_category), style = Type.body.copy(color = p.accent, fontWeight = FontWeight.Medium),
                modifier = Modifier.fillMaxWidth().clickable { pickCategory = true }.padding(vertical = 14.dp),
            )
        }

        GoalsSection(s.goals, vm)
    }

    if (editTotal) MoneyDialog(stringResource(R.string.budget_total), s.total, { editTotal = false }) { vm.setTotal(it) }
    editCategory?.let { c ->
        MoneyDialog(categoryName(c), s.rows.firstOrNull { it.category.id == c.id }?.limit ?: 0, { editCategory = null },
            message = stringResource(R.string.budget_zero_removes)) { vm.setCategoryBudget(c.id, it) }
    }
    if (pickCategory) ListPicker(stringResource(R.string.txn_category), s.unbudgeted.map { it.id to categoryName(it) }, { pickCategory = false }) { id ->
        editCategory = s.unbudgeted.firstOrNull { it.id == id }
    }
}

@Composable
private fun CategoryBudgetRowView(r: CategoryBudgetRow, onClick: () -> Unit) {
    val over = r.used > r.limit
    val warn = r.limit > 0 && r.used * 100 >= r.limit * 80
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Text(categoryName(r.category), style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
            Text(
                if (over) "${stringResource(R.string.over_budget)} ${moneyText(r.used - r.limit)}" else stringResource(R.string.budget_left_n, moneyText(r.limit - r.used)),
                style = Type.body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (over) Base.dangerText else if (warn) Base.warnStrong else Base.text),
            )
        }
        BudgetBar(r.used, r.limit)
        Text(stringResource(R.string.budget_used_of, moneyText(r.used), moneyText(r.limit)), style = Type.tiny)
    }
}
