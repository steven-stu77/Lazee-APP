package io.github.steven_stu77.lazee.feature.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.components.CalcKeypad
import io.github.steven_stu77.lazee.core.ui.components.Chip
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SegmentedControl
import io.github.steven_stu77.lazee.core.ui.components.ToastBus
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import java.time.LocalDate

/** 记一笔 (spec §5.3). [newCategoryId] is set when the new-category page returns. */
@Composable
fun AddScreen(
    onClose: () -> Unit,
    onNewCategory: (kind: String) -> Unit,
    newCategoryId: Long?,
    vm: AddViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showPhotoMenu by remember { mutableStateOf(false) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::attachPhoto) }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) cameraUri?.let(vm::attachPhoto) }

    LaunchedEffect(newCategoryId) { newCategoryId?.let(vm::categoryCreated) }
    LaunchedEffect(s.savedAmount) {
        s.savedAmount?.let {
            ToastBus.show(context.getString(R.string.add_saved_toast, Money.format(it)))
            onClose()
        }
    }
    val divideMsg = stringResource(R.string.add_divide_zero)
    LaunchedEffect(s.divideByZero) { if (s.divideByZero) ToastBus.show(divideMsg) }

    Column(Modifier.fillMaxSize().background(Base.card).navigationBarsPadding()) {
        Header(s.type, onClose, vm::setType)
        AmountRow(s, onTap = vm::cycleDate, onLongPress = { showDatePicker = true })
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (s.templates.isNotEmpty()) TemplateRow(s, vm)
            if (s.type == TxnType.TRANSFER) TransferPicker(s, vm, Modifier.weight(1f))
            else CategoryGrid(s, vm, onNewCategory = { onNewCategory(if (s.type == TxnType.INCOME) "INCOME" else "EXPENSE") }, modifier = Modifier.weight(1f))
        }
        if (s.type != TxnType.TRANSFER) AccountRow(s.accounts, s.accountId, vm::pickAccount)
        NoteRow(s.note, s.photo != null, vm::setNote, onCamera = { showPhotoMenu = true })
        Column(Modifier.fillMaxWidth().background(Base.page).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcKeypad(vm::press)
            if (s.calc.isPending) PrimaryButton(stringResource(R.string.add_calculate), vm::calculate, Modifier.fillMaxWidth())
            else PrimaryButton(stringResource(R.string.save), vm::save, Modifier.fillMaxWidth(), enabled = s.canSave)
        }
    }

    if (showDatePicker) DatePickDialog(s.date, onPick = { vm.setDate(it); showDatePicker = false }, onDismiss = { showDatePicker = false })
    if (showPhotoMenu) PhotoMenu(
        hasPhoto = s.photo != null,
        onCamera = { showPhotoMenu = false; vm.cameraUri().also { cameraUri = it; takePhoto.launch(it) } },
        onGallery = { showPhotoMenu = false; pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemove = { showPhotoMenu = false; vm.removePhoto() },
        onDismiss = { showPhotoMenu = false },
    )
}

@Composable
private fun Header(type: TxnType, onClose: () -> Unit, onType: (TxnType) -> Unit) {
    val close = stringResource(R.string.close)
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Base.page).clickable(role = Role.Button, onClickLabel = close, onClick = onClose), contentAlignment = Alignment.Center) {
            Icon(LazeeIcons.Close, close, tint = Base.text, modifier = Modifier.size(22.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val types = listOf(TxnType.EXPENSE, TxnType.INCOME, TxnType.TRANSFER)
            SegmentedControl(
                listOf(stringResource(R.string.expense), stringResource(R.string.income), stringResource(R.string.transfer)),
                types.indexOf(type), { onType(types[it]) }, Modifier.width(240.dp),
            )
        }
        Box(Modifier.width(44.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AmountRow(s: AddUiState, onTap: () -> Unit, onLongPress: () -> Unit) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val dayLabel = when (s.date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        today.minusDays(2) -> stringResource(R.string.day_before)
        else -> Labels.shortDate(context, s.date)
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Base.page)
                .combinedClickable(role = Role.Button, onClickLabel = stringResource(R.string.add_date_hint), onClick = onTap, onLongClick = onLongPress)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(LazeeIcons.Calendar, null, tint = Base.text, modifier = Modifier.size(16.dp))
            Text(dayLabel, style = Type.small.copy(color = Base.text))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            if (s.calc.isPending) Text(s.calc.expr.replace("+", " + ").replace("−", " − ").replace("×", " × ").replace("÷", " ÷ "), style = Type.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("RM ${Money.plain(s.amount)}", style = Type.addAmount, maxLines = 1)
        }
    }
}

@Composable
private fun TemplateRow(s: AddUiState, vm: AddViewModel) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        s.templates.forEach { t -> Chip(t.name, false, onClick = { vm.applyTemplate(t) }) }
    }
}

@Composable
private fun AccountRow(accounts: List<io.github.steven_stu77.lazee.core.data.Account>, selected: Long?, onPick: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        accounts.forEach { a -> AccountChip(a.name, a.id == selected) { onPick(a.id) } }
    }
}

@Composable
fun AccountChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Text(
        name, style = Type.small.copy(color = if (selected) Color.White else Base.text),
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) p.accent else Base.page)
            .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
private fun NoteRow(note: String, hasPhoto: Boolean, onNote: (String) -> Unit, onCamera: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NoteField(note, onNote, Modifier.weight(1f))
        val label = stringResource(R.string.add_photo)
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(if (hasPhoto) p.tint else Base.page)
                .clickable(role = Role.Button, onClickLabel = label, onClick = onCamera),
            contentAlignment = Alignment.Center,
        ) { Icon(LazeeIcons.Camera, label, tint = if (hasPhoto) p.accent else Base.textSub, modifier = Modifier.size(22.dp)) }
    }
}
