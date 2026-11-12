package io.github.steven_stu77.lazee.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

/** All destinations. Each feature folder owns its screen; this file only wires routes. */
fun NavGraphBuilder.appGraph(nav: NavHostController) {
    listOf(Routes.ONBOARDING, Routes.HOME, Routes.STATS, Routes.BUDGET, Routes.ME, Routes.ADD).forEach { r ->
        composable(r) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(r) } }
    }
}
