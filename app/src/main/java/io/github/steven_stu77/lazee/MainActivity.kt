package io.github.steven_stu77.lazee

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LazeeTheme
import io.github.steven_stu77.lazee.core.ui.theme.Palettes
import io.github.steven_stu77.lazee.nav.LazeeNavHost

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val s = settings
            if (s == null) {
                Box(Modifier.fillMaxSize().background(Base.page))
            } else {
                LaunchedEffect(s.language) { applyLanguage(s.language) }
                LazeeTheme(Palettes.of(s.theme), s.hideAmounts) {
                    LazeeNavHost(startOnboarding = !s.onboardingDone, deepLink = intent.getStringExtra(EXTRA_ROUTE))
                }
            }
        }
    }

    /** In-app language (spec §10). Empty = follow the phone: Chinese if the phone is Chinese, else English. */
    private fun applyLanguage(lang: String) {
        if (lang.isEmpty()) return
        val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        if (!current.startsWith(lang)) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
    }

    companion object {
        /** Notification taps open a route, e.g. "pending". */
        const val EXTRA_ROUTE = "route"
    }
}
