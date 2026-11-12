package io.github.steven_stu77.lazee.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.LedgerMath
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.core.ui.components.CategoryBadge
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.Dims
import io.github.steven_stu77.lazee.core.ui.theme.LocalHideAmounts
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type

/** "今天 · 支出 RM x" header and a white card of rows. */
@Composable
fun DayGroupCard(
    g: LedgerMath.DayGroup<TxnRow>,
    selection: Set<Long>,
    onClick: (Long) -> Unit,
    onLongClick: ((Long) -> Unit)?,
) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(Labels.dayGroup(ctx, g.date), style = Type.small, modifier = Modifier.weight(1f))
            Text(spentLabel(g.spent), style = Type.small)
        }
        TxnCard(g.items, selection, onClick, onLongClick)
    }
}

@Composable
fun TxnCard(items: List<TxnRow>, selection: Set<Long>, onClick: (Long) -> Unit, onLongClick: ((Long) -> Unit)?) {
    Column(Modifier.fillMaxWidth().clip(Dims.cardRadius).background(Base.card).padding(horizontal = 16.dp, vertical = 4.dp)) {
        items.forEachIndexed { i, row ->
            if (i > 0) HorizontalDivider(color = Base.divider)
            TxnRowView(row, row.id in selection, { onClick(row.id) }, onLongClick?.let { { it(row.id) } })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TxnRowView(row: TxnRow, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)?) {
    val p = LocalPalette.current
    val hidden = LocalHideAmounts.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (selected) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(p.accent), contentAlignment = Alignment.Center) {
                Icon(LazeeIcons.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        } else CategoryBadge(row.categoryName, row.colorIndex)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.title, style = Type.body.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (row.isAuto) AutoTag()
            }
            Text("${row.categoryName} · ${row.accountName}", style = Type.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val (prefix, color) = when (row.type) {
            TxnType.EXPENSE -> (if (row.amount < 0) "+" else "-") to Base.text
            TxnType.INCOME -> "+" to p.accent
            TxnType.TRANSFER -> "" to Base.textSub
        }
        val text = if (hidden) Money.HIDDEN else prefix + Money.format(kotlin.math.abs(row.amount))
        Text(text, style = Type.body.copy(fontWeight = FontWeight.SemiBold, color = color))
    }
}

@Composable
fun AutoTag() {
    val p = LocalPalette.current
    Text(
        stringResource(R.string.auto_tag), style = Type.tiny.copy(fontSize = 11.sp, color = p.tintText, fontWeight = FontWeight.Medium),
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(p.tint).padding(horizontal = 6.dp, vertical = 1.dp),
    )
}
