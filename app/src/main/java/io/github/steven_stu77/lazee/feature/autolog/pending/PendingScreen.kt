package io.github.steven_stu77.lazee.feature.autolog.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.Confidence
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.categoryName
import io.github.steven_stu77.lazee.core.ui.components.BannerKind
import io.github.steven_stu77.lazee.core.ui.components.CategoryBadge
import io.github.steven_stu77.lazee.core.ui.components.InfoBanner
import io.github.steven_stu77.lazee.core.ui.components.PrimaryButton
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.onboarding.CheckBox
import io.github.steven_stu77.lazee.feature.search.ListPicker
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PendingScreen(onBack: () -> Unit, vm: PendingViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf<PendingCard?>(null) }
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.pending_title), onBack, stringResource(R.string.back))
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { InfoBanner(stringResource(R.string.pending_info), BannerKind.THEME) }
            if (s.loaded && s.cards.isEmpty()) item {
                Text(stringResource(R.string.pending_all_done), style = Type.body.copy(color = Base.textSub), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp))
            }
            items(s.cards, key = { it.item.id }) { c -> Card(c, onPick = { picking = c }, onAlwaysAsk = { vm.setAlwaysAsk(c.item.id, it) }, onIgnore = { vm.ignore(c) }, onConfirm = { vm.confirm(c) }) }
        }
    }
    picking?.let { c ->
        ListPicker(stringResource(R.string.txn_category), vm.categoriesFor(c).map { it.id to categoryName(it) }, { picking = null }) { id -> id?.let { vm.setCategory(c.item.id, it) } }
    }
}

@Composable
private fun Card(c: PendingCard, onPick: () -> Unit, onAlwaysAsk: (Boolean) -> Unit, onIgnore: () -> Unit, onConfirm: () -> Unit) {
    val ctx = LocalContext.current
    val p = c.item
    SectionCard(padding = PaddingValues(16.dp)) {
        Row {
            Text(stringResource(R.string.an_from_app, c.appName) + (p.last4?.let { " ·· $it" } ?: ""), style = Type.tiny, modifier = Modifier.weight(1f))
            val t = p.postedAt.atZone(ZoneId.systemDefault())
            Text(Labels.dayGroup(ctx, t.toLocalDate()) + " " + t.format(DateTimeFormatter.ofPattern("HH:mm")), style = Type.tiny)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val name = if (c.isTransfer) stringResource(R.string.transfer) else categoryName(c.category)
            CategoryBadge(name, if (c.isTransfer) 7 else c.category?.colorIndex ?: 7, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.merchant, style = Type.body.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                if (c.isTransfer) Text(stringResource(R.string.pending_own_transfer), style = Type.tiny)
                else Text(
                    if (p.confidence == Confidence.LOW) stringResource(R.string.pending_pick_category) else stringResource(R.string.pending_suggested, categoryName(c.category)) + " ›",
                    style = Type.tiny.copy(color = if (p.confidence == Confidence.LOW) Base.warnStrong else Base.textSub),
                    modifier = Modifier.clickable(role = Role.Button, onClick = onPick).padding(vertical = 4.dp),
                )
            }
            val sign = if (p.isRefund || p.direction == io.github.steven_stu77.lazee.core.data.PendingDirection.IN) "+" else ""
            Text(sign + moneyText(p.amount), style = Type.section.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
        }
        c.bill?.let { InfoBanner(stringResource(R.string.pending_bill_match, it.name, it.dayOfMonth), BannerKind.INFO) }
        c.duplicateOf?.let { d -> InfoBanner(stringResource(R.string.pending_duplicate, Labels.txnTitle(ctx, d, null), moneyText(d.amount)), BannerKind.WARN) }
        if (!c.isTransfer && !p.isPersonTransfer) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CheckBox(c.alwaysAsk) { onAlwaysAsk(!c.alwaysAsk) }
            Text(stringResource(R.string.pending_always_ask), style = Type.small.copy(color = Base.text))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // A likely duplicate recommends ignoring: make Ignore the filled button.
            if (c.duplicateOf != null) {
                PrimaryButton(stringResource(R.string.an_ignore), onIgnore, Modifier.weight(1f))
                OutlineButton(stringResource(R.string.pending_confirm), onConfirm, Modifier.weight(1f))
            } else {
                OutlineButton(stringResource(R.string.an_ignore), onIgnore, Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.pending_confirm), onConfirm, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).background(Base.card).border(1.dp, Base.border, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Type.body.copy(fontSize = 14.sp)) }
}
