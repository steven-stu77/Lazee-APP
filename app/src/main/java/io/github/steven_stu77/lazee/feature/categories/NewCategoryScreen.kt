package io.github.steven_stu77.lazee.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.CategoryKind
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.CategoryBadge
import io.github.steven_stu77.lazee.core.ui.components.Chip
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SegmentedControl
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.CategoryColors
import io.github.steven_stu77.lazee.core.ui.theme.Type

/** 新增分类 (spec §5.12). Also used to rename / recolour (edit mode). [onSaved] returns the category id. */
@Composable
fun NewCategoryScreen(onBack: () -> Unit, onSaved: (Long) -> Unit, vm: NewCategoryViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.savedId) { s.savedId?.let(onSaved) }
    val dup = s.name.isNotBlank() && vm.isDuplicate(s)
    val colorNames = listOf(R.string.color_orange, R.string.color_blue, R.string.color_purple, R.string.color_pink, R.string.color_green, R.string.color_yellow, R.string.color_red, R.string.color_grey)

    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(if (s.isEdit) R.string.cat_edit_title else R.string.cat_new_title), onBack, stringResource(R.string.back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryBadge(s.name.ifBlank { "?" }, s.colorIndex, size = 72.dp)
                Text(s.name.ifBlank { stringResource(R.string.cat_preview) }, style = Type.small)
            }
            SectionCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                Text(stringResource(R.string.cat_name_label), style = Type.small)
                LazeeTextField(s.name, vm::setName, Modifier.fillMaxWidth(), placeholder = stringResource(R.string.cat_name_hint))
                if (dup) Text(stringResource(R.string.cat_duplicate), style = Type.small.copy(color = Base.dangerText))
                if (!s.isEdit) {
                    Text(stringResource(R.string.cat_type), style = Type.small)
                    SegmentedControl(listOf(stringResource(R.string.expense), stringResource(R.string.income)), if (s.kind == CategoryKind.EXPENSE) 0 else 1,
                        { vm.setKind(if (it == 0) CategoryKind.EXPENSE else CategoryKind.INCOME) }, Modifier.fillMaxWidth())
                }
                Text(stringResource(R.string.cat_color), style = Type.small)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(8) { i ->
                        val label = stringResource(colorNames[i])
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(CategoryColors.bg(i))
                                .border(if (i == s.colorIndex) 2.5.dp else 1.dp, if (i == s.colorIndex) CategoryColors.fg(i) else Base.border, CircleShape)
                                .clickable(role = Role.RadioButton) { vm.setColor(i) }
                                .semantics { contentDescription = label },
                        )
                    }
                }
                if (!s.isEdit && s.parents.isNotEmpty()) {
                    Text(stringResource(R.string.cat_parent), style = Type.small)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(stringResource(R.string.cat_no_parent), s.parentId == null, onClick = { vm.setParent(null) })
                        s.parents.forEach { p -> Chip(categoryName(p), p.id == s.parentId, onClick = { vm.setParent(p.id) }) }
                    }
                }
            }
        }
        PrimaryButton(stringResource(R.string.cat_save), vm::save, Modifier.fillMaxWidth().padding(20.dp), enabled = s.name.isNotBlank() && !dup)
    }
}

