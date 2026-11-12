package io.github.steven_stu77.lazee.feature.txn

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.TxnSource
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.BannerKind
import io.github.steven_stu77.lazee.core.ui.components.CategoryBadge
import io.github.steven_stu77.lazee.core.ui.components.InfoBanner
import io.github.steven_stu77.lazee.core.ui.components.LazeeTextField
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SecondaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.ToastBus
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Dims
import io.github.steven_stu77.lazee.core.ui.theme.Type

@Composable
fun TxnDetailScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, onCopy: (Long) -> Unit, vm: TxnDetailViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var templateName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(s.deleted, s.loaded, s.txn) { if (s.deleted || (s.loaded && s.txn == null)) onBack() }
    val t = s.txn ?: return

    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.txn_title), onBack, stringResource(R.string.back))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val name = if (t.type == TxnType.TRANSFER) stringResource(R.string.transfer) else categoryName(s.category)
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryBadge(name, if (t.type == TxnType.TRANSFER) 7 else s.category?.colorIndex ?: 7, size = 56.dp)
                Text(Labels.txnTitle(ctx, t, s.category), style = Type.section, textAlign = TextAlign.Center)
                val sign = when (t.type) { TxnType.EXPENSE -> "-"; TxnType.INCOME -> "+"; else -> "" }
                Text(sign + moneyText(t.amount), style = Type.heroAmount.copy(color = Base.text))
            }
            if (t.isAuto) InfoBanner(stringResource(R.string.txn_auto_banner), BannerKind.THEME)
            SectionCard {
                Field(stringResource(R.string.txn_type), stringResource(when (t.type) { TxnType.EXPENSE -> R.string.expense; TxnType.INCOME -> R.string.income; TxnType.TRANSFER -> R.string.transfer }))
                if (t.type != TxnType.TRANSFER) Field(stringResource(R.string.txn_category), listOfNotNull(s.parentCategory?.let { categoryName(it) }, categoryName(s.category)).joinToString(" › "))
                Field(stringResource(R.string.txn_account), if (t.type == TxnType.TRANSFER) "${s.account?.name.orEmpty()} → ${s.toAccount?.name.orEmpty()}" else s.account?.name.orEmpty())
                Field(stringResource(R.string.txn_date), Labels.dayFull(ctx, t.date))
                t.merchant?.let { Field(stringResource(R.string.txn_merchant), it) }
                t.note?.let { Field(stringResource(R.string.txn_note), it) }
                if (s.tags.isNotEmpty()) Field(stringResource(R.string.txn_tags), s.tags.joinToString("  ") { "#$it" })
                Field(stringResource(R.string.txn_source), stringResource(when (t.source) {
                    TxnSource.MANUAL -> R.string.source_manual; TxnSource.NOTIFICATION -> R.string.source_notification
                    TxnSource.BILL -> R.string.source_bill; TxnSource.LOAN -> R.string.source_loan
                }) + if (t.isAuto) " · " + stringResource(R.string.auto_tag) else "")
            }
            s.photo?.let { f ->
                val bmp = remember(f) { BitmapFactory.decodeFile(f.path)?.asImageBitmap() }
                if (bmp != null) Image(bmp, stringResource(R.string.add_photo), Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(Dims.cardRadius), contentScale = ContentScale.Fit)
            }
        }
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (t.isAuto) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.txn_undo_auto), { vm.undoAuto() }, Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.txn_keep), { vm.confirmAuto() }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.edit), { onEdit(t.id) }, Modifier.weight(1f))
                SecondaryButton(stringResource(R.string.txn_copy), { onCopy(t.id) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(stringResource(R.string.txn_save_template), { templateName = Labels.txnTitle(ctx, t, s.category) }, Modifier.weight(1f))
                TextButton({ confirmDelete = true }, Modifier.weight(1f)) { Text(stringResource(R.string.delete), color = Base.dangerText) }
            }
        }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text(stringResource(R.string.txn_delete_confirm)) },
        confirmButton = { TextButton({ confirmDelete = false; vm.delete() }) { Text(stringResource(R.string.delete), color = Base.dangerText) } },
        dismissButton = { TextButton({ confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
    )
    templateName?.let { name ->
        val saved = stringResource(R.string.txn_template_saved)
        AlertDialog(
            onDismissRequest = { templateName = null },
            title = { Text(stringResource(R.string.txn_save_template)) },
            text = { LazeeTextField(name, { templateName = it.take(20) }, placeholder = stringResource(R.string.txn_template_name)) },
            confirmButton = { TextButton({ vm.saveTemplate(name); templateName = null; ToastBus.show(saved) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
            dismissButton = { TextButton({ templateName = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = Type.small, modifier = Modifier.weight(0.35f))
        Text(value, style = Type.body, modifier = Modifier.weight(0.65f), textAlign = TextAlign.End)
    }
    HorizontalDivider(color = Base.divider)
}
