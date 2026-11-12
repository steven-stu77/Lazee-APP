package io.github.steven_stu77.lazee.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.notify.Notifier
import io.github.steven_stu77.lazee.core.prefs.Settings
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.prefs.ThemeKey
import io.github.steven_stu77.lazee.core.ui.components.HeroCard
import io.github.steven_stu77.lazee.core.ui.components.LazeeIcons
import io.github.steven_stu77.lazee.core.ui.components.SectionCard
import io.github.steven_stu77.lazee.core.ui.components.SubPageHeader
import io.github.steven_stu77.lazee.core.ui.components.moneyText
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LocalPalette
import io.github.steven_stu77.lazee.core.ui.theme.Palettes
import io.github.steven_stu77.lazee.core.ui.theme.Type
import io.github.steven_stu77.lazee.feature.me.themeName
import io.github.steven_stu77.lazee.feature.onboarding.BigChoice
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LookViewModel @Inject constructor(private val settings: SettingsRepository, private val notifier: Notifier) : ViewModel() {
    val state: StateFlow<Settings> = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())
    fun setLanguage(l: String) = viewModelScope.launch { settings.setLanguage(l) }
    fun setTheme(t: ThemeKey) = viewModelScope.launch { settings.setTheme(t) }
    /** Channel names are shown in system settings, so re-create them in the new language. */
    fun refreshChannels() = notifier.createChannels()
}

/** 外观与语言 (spec §5.14): changes apply immediately, no restart. */
@Composable
fun LookScreen(onBack: () -> Unit, vm: LookViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(s.language) { vm.refreshChannels() }
    val lang = s.language.ifEmpty { if (io.github.steven_stu77.lazee.core.ui.Labels.isZh(androidx.compose.ui.platform.LocalContext.current)) "zh" else "en" }
    Column(Modifier.fillMaxSize().background(Base.page)) {
        SubPageHeader(stringResource(R.string.look_title), onBack, stringResource(R.string.back))
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.look_language), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigChoice("中文", lang == "zh", Modifier.weight(1f)) { vm.setLanguage("zh") }
                BigChoice("English", lang == "en", Modifier.weight(1f)) { vm.setLanguage("en") }
            }
            Text(stringResource(R.string.look_theme), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
            SectionCard(padding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
                Palettes.all.forEachIndexed { i, p ->
                    if (i > 0) HorizontalDivider(color = Base.divider)
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.RadioButton) { vm.setTheme(p.key) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(p.deep))
                        Box(Modifier.size(16.dp).clip(CircleShape).background(p.accent))
                        Text(stringResource(themeName(p.key)), style = Type.body, modifier = Modifier.weight(1f))
                        if (p.key == s.theme) Icon(LazeeIcons.Check, null, tint = p.accent, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Text(stringResource(R.string.cat_preview), style = Type.small, modifier = Modifier.padding(horizontal = 4.dp))
            val pal = LocalPalette.current
            HeroCard {
                Text(stringResource(R.string.home_month_spend), style = Type.small.copy(color = pal.sub))
                Text(moneyText(68_420), style = Type.heroAmount.copy(color = Color.White))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))) {
                    Box(Modifier.fillMaxWidth(0.46f).height(8.dp).clip(CircleShape).background(pal.glow))
                }
            }
        }
    }
}
