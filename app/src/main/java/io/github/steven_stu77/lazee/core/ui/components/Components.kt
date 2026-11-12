package io.github.steven_stu77.lazee.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.CategoryColors
import io.github.steven_stu77.lazee.core.ui.theme.Dims
import io.github.steven_stu77.lazee.core.ui.theme.LocalHideAmounts
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type

/** Round badge with the category's first character (upper-cased for Latin names). */
@Composable
fun CategoryBadge(name: String, colorIndex: Int, size: Dp = 38.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(CategoryColors.bg(colorIndex)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().take(1).uppercase(),
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = (size.value * 0.38f).sp, color = CategoryColors.fg(colorIndex)),
        )
    }
}

/** White rounded card used for lists and sections. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(Dims.cardRadius).background(Base.card).padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Dark theme-coloured hero card (home, budget). */
@Composable
fun HeroCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(
        modifier.fillMaxWidth().clip(Dims.heroRadius).background(p.deep)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

enum class BannerKind { WARN, INFO, THEME }

@Composable
fun InfoBanner(
    text: String,
    kind: BannerKind,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    val (bg, fg) = when (kind) {
        BannerKind.WARN -> Base.warnBg to Base.warnText
        BannerKind.INFO -> Base.infoBg to Base.infoText
        BannerKind.THEME -> p.tint to p.tintText
    }
    Row(
        modifier.fillMaxWidth().clip(Dims.cardRadius).background(bg)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .heightIn(min = Dims.minTouch).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        Text(text, style = Type.body.copy(fontSize = 14.sp, color = fg), modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = Type.bodyStrong.copy(fontSize = 14.sp, color = fg))
        if (onClick != null) Icon(LazeeIcons.ChevronRight, null, tint = fg, modifier = Modifier.size(18.dp))
    }
}

enum class TagStyle { ON, OFF, WARN }

/** Small rounded status label, e.g. "已开启". */
@Composable
fun StatusTag(text: String, highlighted: Boolean = false, style: TagStyle = if (highlighted) TagStyle.ON else TagStyle.OFF) {
    val p = LocalPalette.current
    val (bg, fg) = when (style) {
        TagStyle.ON -> p.tint to p.tintText
        TagStyle.OFF -> Base.divider to Base.textSub
        TagStyle.WARN -> Base.warnBg to Base.warnText
    }
    Text(
        text,
        style = Type.tiny.copy(color = fg, fontWeight = FontWeight.Medium),
        modifier = Modifier.clip(Dims.tagRadius).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** 52×32 switch drawn like the prototype. */
@Composable
fun LazeeSwitch(checked: Boolean, onChange: (Boolean) -> Unit, contentDescription: String? = null) {
    val p = LocalPalette.current
    Box(
        Modifier.size(52.dp, 32.dp).clip(CircleShape).background(if (checked) p.accent else Base.switchOff)
            .clickable(role = Role.Switch, onClickLabel = contentDescription) { onChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(Color.White))
    }
}

/** Title + description with a switch, an arrow, or a status tag on the right. */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    status: String? = null,
    statusOn: Boolean = false,
    statusStyle: TagStyle = if (statusOn) TagStyle.ON else TagStyle.OFF,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .heightIn(min = 52.dp).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Type.body)
            if (description != null) Text(description, style = Type.tiny)
        }
        if (status != null) StatusTag(status, style = statusStyle)
        if (checked != null && onCheckedChange != null) LazeeSwitch(checked, onCheckedChange, title)
        else if (onClick != null) Icon(LazeeIcons.ChevronRight, null, tint = Base.disabled, modifier = Modifier.size(18.dp))
    }
}

/** Pill segmented control (支出 / 收入 / 转账 …). */
@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(Base.divider).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 36.dp).clip(RoundedCornerShape(10.dp))
                    .background(if (on) Base.card else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = Type.body.copy(fontSize = 14.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) Base.text else Base.textSub))
            }
        }
    }
}

/** Formats cents honoring the global "hide amounts" switch. */
@Composable
fun AmountText(
    cents: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
    prefix: String = "",
    color: Color = Color.Unspecified,
) {
    val hidden = LocalHideAmounts.current
    Text(prefix + Money.format(cents, hidden), style = style, color = color, modifier = modifier)
}

@Composable
fun moneyText(cents: Long): String = Money.format(cents, LocalHideAmounts.current)

/** Back / close header for sub-pages: white round button + 20sp bold title (prototype). */
@Composable
fun SubPageHeader(title: String, onBack: () -> Unit, backLabel: String, close: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(Dims.minTouch).clip(CircleShape).background(Base.card)
                .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (close) LazeeIcons.Close else LazeeIcons.ChevronLeft, backLabel, tint = Base.text, modifier = Modifier.size(22.dp))
        }
        Text(title, style = Type.pageTitle.copy(fontSize = 20.sp), modifier = Modifier.weight(1f))
        actions()
    }
}

/** Thin progress bar; colour switches to orange at 80% and red when over. */
@Composable
fun BudgetBar(used: Long, limit: Long, modifier: Modifier = Modifier, track: Color = Base.divider, normal: Color = LocalPalette.current.accent) {
    val ratio = if (limit <= 0) 0f else used.toFloat() / limit
    val color = when {
        ratio > 1f -> Base.danger
        ratio >= 0.8f -> Base.orange
        else -> normal
    }
    Box(modifier.fillMaxWidth().heightIn(min = 6.dp).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).heightIn(min = 6.dp).clip(CircleShape).background(color))
    }
}

/** Primary filled button. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val p = LocalPalette.current
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp))
            .background(if (enabled) p.accent else Base.switchOff)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.bodyStrong.copy(color = if (enabled) Color.White else Base.disabled))
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(14.dp)).background(Base.divider)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Type.bodyStrong)
    }
}

/** Small rounded chip used for accounts, templates, sub-categories. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val p = LocalPalette.current
    Text(
        text,
        style = Type.body.copy(fontSize = 14.sp, color = if (selected) p.tintText else Base.text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
        modifier = Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (selected) p.tint else Base.card)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 36.dp).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** Plain tappable row for dialogs and menus. */
@Composable
fun MenuItem(text: String, onClick: () -> Unit) {
    Text(text, style = Type.body, modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp))
}

/** Neutral progress bar (no warning colours), e.g. share of the biggest category. */
@Composable
fun PlainBar(ratio: Float, modifier: Modifier = Modifier, color: Color = LocalPalette.current.accent) {
    Box(modifier.fillMaxWidth().heightIn(min = 6.dp).clip(CircleShape).background(Base.divider)) {
        Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).heightIn(min = 6.dp).clip(CircleShape).background(color))
    }
}
