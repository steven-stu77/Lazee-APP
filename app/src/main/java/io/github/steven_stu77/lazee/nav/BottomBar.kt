package io.github.steven_stu77.lazee.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Type

/** 明细 · 统计 · [+] · 预算 · 我的 */
@Composable
fun BottomBar(current: String?, onTab: (String) -> Unit, onAdd: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().background(Base.card).navigationBarsPadding()) {
        HorizontalDivider(color = Color(0xFFE6E8EC))
        Row(Modifier.fillMaxWidth().height(68.dp).padding(top = 8.dp), verticalAlignment = Alignment.Top) {
            NavItem(LazeeIcons.Home, stringResource(R.string.nav_home), current == Routes.HOME) { onTab(Routes.HOME) }
            NavItem(LazeeIcons.Stats, stringResource(R.string.nav_stats), current == Routes.STATS) { onTab(Routes.STATS) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                val label = stringResource(R.string.add_title)
                Box(
                    Modifier.offset(y = (-8).dp).size(56.dp).shadow(10.dp, CircleShape, ambientColor = p.deep, spotColor = p.deep)
                        .clip(CircleShape).background(p.accent)
                        .clickable(role = Role.Button, onClickLabel = label, onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LazeeIcons.Plus, label, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
            NavItem(LazeeIcons.Budget, stringResource(R.string.nav_budget), current == Routes.BUDGET) { onTab(Routes.BUDGET) }
            NavItem(LazeeIcons.Me, stringResource(R.string.nav_me), current == Routes.ME) { onTab(Routes.ME) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) LocalPalette.current.accent else Base.disabled
    Column(
        Modifier.weight(1f).clickable(role = Role.Tab, onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(label, style = Type.nav.copy(color = color))
    }
}
