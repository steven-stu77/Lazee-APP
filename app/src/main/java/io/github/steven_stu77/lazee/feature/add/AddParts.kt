package io.github.steven_stu77.lazee.feature.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.CategoryBadge
import io.github.steven_stu77.lazee.core.ui.components.Chip
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.components.MenuItem
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun CategoryGrid(s: AddUiState, vm: AddViewModel, onNewCategory: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier) {
        LazyVerticalGrid(GridCells.Fixed(5), Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.categories, key = { it.id }) { c ->
                val selected = c.id == s.categoryId
                Column(
                    Modifier.clickable(role = Role.RadioButton) { vm.pickCategory(c.id) }.padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(Modifier.size(50.dp).let { if (selected) it.border(2.dp, p.accent, CircleShape) else it }, contentAlignment = Alignment.Center) {
                        CategoryBadge(categoryName(c), c.colorIndex, size = 44.dp)
                    }
                    Text(categoryName(c), style = Type.tiny.copy(color = Base.text), textAlign = TextAlign.Center, maxLines = 2)
                }
            }
            item(key = "new") {
                Column(
                    Modifier.clickable(role = Role.Button, onClick = onNewCategory).padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.size(44.dp).drawBehind {
                                drawCircle(Base.disabled, style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))))
                            },
                            contentAlignment = Alignment.Center,
                        ) { Icon(LazeeIcons.Plus, null, tint = Base.textSub, modifier = Modifier.size(20.dp)) }
                    }
                    Text(stringResource(R.string.add_new_short), style = Type.tiny)
                }
            }
        }
        if (s.subCategories.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                s.subCategories.forEach { sub -> Chip(categoryName(sub), sub.id == s.subCategoryId, onClick = { vm.pickSub(sub.id) }) }
            }
        }
    }
}

@Composable
fun TransferPicker(s: AddUiState, vm: AddViewModel, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.add_from), style = Type.section)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.accounts.forEach { a -> AccountChip(a.name, a.id == s.accountId) { vm.pickAccount(a.id) } }
        }
        Text(stringResource(R.string.add_to), style = Type.section)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            s.accounts.filter { it.id != s.accountId }.forEach { a -> AccountChip(a.name, a.id == s.toAccountId) { vm.pickToAccount(a.id) } }
        }
        if (s.accounts.size < 2) Text(stringResource(R.string.add_transfer_need_two), style = Type.small)
    }
}

@Composable
fun NoteField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value, onChange, modifier, singleLine = true, textStyle = Type.body.copy(fontSize = Type.small.fontSize, color = Base.text),
        cursorBrush = SolidColor(LocalPalette.current.accent),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(Base.page).padding(horizontal = 14.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(stringResource(R.string.add_note_hint), style = Type.small.copy(color = Base.disabled))
                inner()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // DatePicker works in UTC millis; convert at the edges only.
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss()
            }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    ) { DatePicker(state) }
}

@Composable
fun PhotoMenu(hasPhoto: Boolean, onCamera: () -> Unit, onGallery: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_photo)) },
        text = {
            Column {
                MenuItem(stringResource(R.string.add_photo_camera), onCamera)
                MenuItem(stringResource(R.string.add_photo_gallery), onGallery)
                if (hasPhoto) MenuItem(stringResource(R.string.add_photo_remove), onRemove)
            }
        },
        confirmButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
