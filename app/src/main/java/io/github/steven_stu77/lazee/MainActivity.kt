package io.github.steven_stu77.lazee

import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ProcessLifecycleOwner
import io.github.steven_stu77.lazee.core.security.AppLock
import io.github.steven_stu77.lazee.core.security.LockScreen
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.steven_stu77.lazee.core.ui.theme.Base
import io.github.steven_stu77.lazee.core.ui.theme.LazeeTheme
import io.github.steven_stu77.lazee.core.ui.theme.Palettes
import io.github.steven_stu77.lazee.nav.LazeeNavHost

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()
    @Inject lateinit var appLock: AppLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLock)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val locked by appLock.locked.collectAsStateWithLifecycle()
            val s = settings
            if (s == null) {
                Box(Modifier.fillMaxSize().background(Base.page))
            } else {
                LaunchedEffect(s.language) { applyLanguage(s.language) }
                LaunchedEffect(s.appLockEnabled) { setLockEnabled(s.appLockEnabled) }
                LazeeTheme(Palettes.of(s.theme), s.hideAmounts) {
                    Box(Modifier.fillMaxSize()) {
                        LazeeNavHost(startOnboarding = !s.onboardingDone, deepLink = intent.getStringExtra(EXTRA_ROUTE))
                        if (s.appLockEnabled && locked) {
                            val title = stringResource(R.string.lock_unlock)
                            val prompt = { AppLock.prompt(this@MainActivity, title, onSuccess = appLock::unlocked) }
                            LaunchedEffect(Unit) { prompt() }
                            LockScreen(onUnlock = prompt)
                        }
                    }
                }
            }
        }
    }

    /** FLAG_SECURE hides the screen from recents and screenshots while the lock is on. */
    private fun setLockEnabled(on: Boolean) {
        appLock.enabled = on
        if (on) window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
