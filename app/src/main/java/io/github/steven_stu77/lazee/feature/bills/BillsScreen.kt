package io.github.steven_stu77.lazee.feature.bills

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.PaymentMethod
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.core.repo.BillStatus
import io.github.steven_stu77.lazee.core.ui.components.BannerKind
import io.github.steven_stu77.lazee.core.ui.components.InfoBanner
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SegmentedControl
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.autolog.pending.OutlineButton

@Composable
fun BillsScreen(onBack: () -> Unit, onEdit: (Long) -> Unit, vm: BillsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.bills_title), onBack, stringResource(R.string.back)) {
            TextButton({ onEdit(0) }) { Text(stringResource(R.string.bills_add), style = Type.body.copy(color = LocalPalette.current.accent, fontWeight = FontWeight.SemiBold)) }
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { InfoBanner(stringResource(R.string.bills_info), BannerKind.INFO) }
            item {
                SegmentedControl(listOf(stringResource(R.string.search_all), stringResource(R.string.bills_subscriptions)), if (s.subscriptionsOnly) 1 else 0,
                    { vm.setSubscriptionsOnly(it == 1) }, Modifier.fillMaxWidth())
            }
            if (s.subscriptionsOnly) item { Text(stringResource(R.string.bills_sub_total, moneyText(s.subscriptionTotal)), style = Type.section) }
            if (s.items.isEmpty()) item { Text(stringResource(R.string.bills_empty), style = Type.small, modifier = Modifier.padding(vertical = 24.dp)) }
            items(s.items, key = { it.bill.id }) { b -> BillCard(b, s.today, onClick = { onEdit(b.bill.id) }, onMarkPaid = { vm.markPaid(b) }) }
        }
    }
}

@Composable
private fun BillCard(b: BillStatus, today: java.time.LocalDate, onClick: () -> Unit, onMarkPaid: () -> Unit) {
    val p = LocalPalette.current
    SectionCard(Modifier.clickable(onClick = onClick), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (b.paid) Base.divider else p.tint),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Text(b.due.dayOfMonth.toString(), style = Type.section.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (b.paid) Base.textSub else p.tintText))
                Text(stringResource(R.string.bills_day_unit), style = Type.tiny.copy(fontSize = 10.sp, color = if (b.paid) Base.textSub else p.tintText))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(b.bill.name + (b.installment?.let { " · " + stringResource(R.string.bills_installment, it, b.bill.totalInstallments ?: it) } ?: ""), style = Type.body.copy(fontWeight = FontWeight.SemiBold))
                Text(stringResource(R.string.bills_schedule, b.bill.dayOfMonth, b.bill.remindDaysBefore), style = Type.tiny)
            }
            Text(moneyText(b.bill.amount), style = Type.body.copy(fontWeight = FontWeight.Bold))
        }
        val days = BillMath.daysUntil(today, b.due)
        val (text, color) = when {
            b.paid && b.payment?.method == PaymentMethod.NOTIFICATION -> stringResource(R.string.bills_paid_matched) to p.tintText
            b.paid && b.payment?.method == PaymentMethod.AUTO -> stringResource(R.string.bills_paid_auto) to p.tintText
            b.paid -> stringResource(R.string.bills_paid_manual) to p.tintText
            days < 0 -> stringResource(R.string.bill_overdue) to Base.dangerText
            days == 0L -> stringResource(R.string.bill_due_today) to Base.warnStrong
            else -> stringResource(R.string.bill_due_in, days) to Base.textSub
        }
        Text(text, style = Type.small.copy(color = color, fontWeight = FontWeight.Medium))
        if (!b.paid) OutlineButton(stringResource(R.string.bills_mark_paid), onMarkPaid, Modifier.fillMaxWidth())
    }
}

