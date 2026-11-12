package io.github.steven_stu77.lazee.feature.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.CategoryBadge
import io.github.steven_stu77.lazee.core.ui.components.MenuItem
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type

@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    onNew: (kind: String, parentId: Long) -> Unit,
    onEdit: (Long) -> Unit,
    vm: CategoriesViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var menuFor by remember { mutableStateOf<Category?>(null) }
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.cats_title), onBack, stringResource(R.string.back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Group(stringResource(R.string.cats_expense), s.expense, s.subs) { menuFor = it }
            Group(stringResource(R.string.cats_income), s.income, s.subs) { menuFor = it }
            if (s.archived.isNotEmpty()) Group(stringResource(R.string.cats_archived), s.archived, emptyMap()) { menuFor = it }
            Text(stringResource(R.string.cats_long_press), style = Type.tiny, modifier = Modifier.padding(horizontal = 4.dp))
        }
        PrimaryButton(stringResource(R.string.cats_new), { onNew("EXPENSE", 0) }, Modifier.fillMaxWidth().padding(20.dp))
    }
    menuFor?.let { c ->
        val close = { menuFor = null }
        AlertDialog(
            onDismissRequest = close,
            title = { Text(categoryName(c)) },
            text = {
                Column {
                    if (c.archived) MenuItem(stringResource(R.string.cats_restore)) { vm.setArchived(c, false); close() }
                    else {
                        MenuItem(stringResource(R.string.cats_rename_recolor)) { onEdit(c.id); close() }
                        if (c.parentId == null) {
                            MenuItem(stringResource(R.string.cats_add_sub)) { onNew(c.kind.name, c.id); close() }
                            MenuItem(stringResource(R.string.cats_move_earlier)) { vm.move(c, -1); close() }
                            MenuItem(stringResource(R.string.cats_move_later)) { vm.move(c, 1); close() }
                        }
                        MenuItem(stringResource(R.string.cats_archive)) { vm.setArchived(c, true); close() }
                    }
                }
            },
            confirmButton = { TextButton(close) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun Group(title: String, items: List<Category>, subs: Map<Long, List<Category>>, onLongPress: (Category) -> Unit) {
    Text(title, style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
    SectionCard {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(14.dp), maxItemsInEachRow = 4) {
            (items + items.flatMap { subs[it.id].orEmpty() }).forEach { c ->
                Column(
                    Modifier.width(72.dp).combinedClickable(onClick = { onLongPress(c) }, onLongClick = { onLongPress(c) }),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CategoryBadge(categoryName(c), c.colorIndex, size = if (c.parentId != null) 32.dp else 40.dp)
                    val parent = c.parentId?.let { p -> items.firstOrNull { it.id == p } }
                    Text(if (parent != null) "${categoryName(parent)} › ${categoryName(c)}" else categoryName(c), style = Type.tiny.copy(color = Base.text), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
