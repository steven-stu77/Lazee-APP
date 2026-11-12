package io.github.steven_stu77.lazee.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.steven_stu77.lazee.core.ui.theme.Base

@Composable
fun LazeeNavHost(startOnboarding: Boolean) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Column(Modifier.fillMaxSize().background(Base.page)) {
        Box(Modifier.weight(1f).statusBarsPadding()) {
            NavHost(nav, startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.HOME) {
                appGraph(nav)
            }
        }
        if (route in Routes.tabs) {
            BottomBar(route, onTab = { nav.switchTab(it) }, onAdd = { nav.navigate(Routes.add()) })
        }
    }
}

fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
