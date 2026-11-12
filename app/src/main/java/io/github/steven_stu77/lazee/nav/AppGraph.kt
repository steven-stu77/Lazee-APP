package io.github.steven_stu77.lazee.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.github.steven_stu77.lazee.feature.accounts.AccountDetailScreen
import io.github.steven_stu77.lazee.feature.accounts.AccountEditScreen
import io.github.steven_stu77.lazee.feature.add.AddScreen
import io.github.steven_stu77.lazee.feature.categories.CategoriesScreen
import io.github.steven_stu77.lazee.feature.bills.BillEditScreen
import io.github.steven_stu77.lazee.feature.bills.BillsScreen
import io.github.steven_stu77.lazee.feature.loans.LoansScreen
import io.github.steven_stu77.lazee.feature.settings.RemindersScreen
import io.github.steven_stu77.lazee.feature.autolog.pending.PendingScreen
import io.github.steven_stu77.lazee.feature.autolog.settings.AutoLogSettingsScreen
import io.github.steven_stu77.lazee.feature.autolog.settings.ParserTestScreen
import io.github.steven_stu77.lazee.feature.categories.NewCategoryScreen
import io.github.steven_stu77.lazee.feature.home.HomeScreen
import io.github.steven_stu77.lazee.feature.me.MeScreen
import io.github.steven_stu77.lazee.feature.me.MeViewModel
import io.github.steven_stu77.lazee.feature.onboarding.OnboardingScreen
import io.github.steven_stu77.lazee.feature.search.SearchScreen
import io.github.steven_stu77.lazee.feature.stats.CategoryDetailScreen
import io.github.steven_stu77.lazee.feature.stats.StatsScreen
import io.github.steven_stu77.lazee.feature.stats.YearScreen
import io.github.steven_stu77.lazee.feature.budget.BudgetScreen
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.feature.txn.TxnDetailScreen

const val NEW_CATEGORY_RESULT = "newCategoryId"

private fun longArg(name: String) = navArgument(name) { type = NavType.LongType; defaultValue = 0L }
private fun intArg(name: String) = navArgument(name) { type = NavType.IntType; defaultValue = 0 }

/** All destinations. Each feature folder owns its screen; this file only wires routes. */
fun NavGraphBuilder.appGraph(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }

    composable(Routes.ONBOARDING) {
        OnboardingScreen(onDone = { autoLog ->
            nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            if (autoLog) nav.navigate(Routes.AUTOLOG)
        })
    }
    composable(Routes.HOME) {
        HomeScreen(
            onSearch = { nav.navigate(Routes.SEARCH) },
            onPending = { nav.navigate(Routes.PENDING) },
            onBills = { nav.navigate(Routes.BILLS) },
            onBudget = { nav.switchTab(Routes.BUDGET) },
            onTxn = { nav.navigate(Routes.txn(it)) },
        )
    }
    composable(
        Routes.ADD,
        arguments = listOf(longArg("txnId"), navArgument("copy") { type = NavType.BoolType; defaultValue = false }, longArg("categoryId")),
    ) { entry ->
        val created by entry.savedStateHandle.getStateFlow<Long?>(NEW_CATEGORY_RESULT, null).collectAsStateWithLifecycle()
        AddScreen(onClose = back, onNewCategory = { kind -> nav.navigate(Routes.newCategory(kind)) }, newCategoryId = created)
    }
    composable(Routes.TXN, arguments = listOf(longArg("id"))) {
        TxnDetailScreen(
            onBack = back,
            onEdit = { nav.navigate(Routes.add(txnId = it)) },
            onCopy = { nav.navigate(Routes.add(txnId = it, copy = true)) },
        )
    }
    composable(Routes.SEARCH) { SearchScreen(onBack = back, onTxn = { nav.navigate(Routes.txn(it)) }) }
    composable(Routes.CATEGORIES) {
        CategoriesScreen(
            onBack = back,
            onNew = { kind, parent -> nav.navigate(Routes.newCategory(kind, parent)) },
            onEdit = { nav.navigate(Routes.newCategory(editId = it)) },
        )
    }
    composable(
        Routes.NEW_CATEGORY,
        arguments = listOf(navArgument("kind") { type = NavType.StringType; defaultValue = "EXPENSE" }, longArg("parentId"), longArg("editId")),
    ) {
        NewCategoryScreen(onBack = back, onSaved = { id ->
            nav.previousBackStackEntry?.savedStateHandle?.set(NEW_CATEGORY_RESULT, id)
            nav.popBackStack()
        })
    }
    composable(Routes.ME) {
        val vm: MeViewModel = hiltViewModel()
        MeScreen(onNavigate = { nav.navigate(it) }, onLockToggle = vm::setAppLock, vm = vm)
    }
    composable(Routes.ACCOUNT, arguments = listOf(longArg("id"))) {
        AccountDetailScreen(onBack = back, onEdit = { nav.navigate(Routes.accountEdit(it)) }, onTxn = { nav.navigate(Routes.txn(it)) })
    }
    composable(Routes.ACCOUNT_EDIT, arguments = listOf(longArg("id"))) { AccountEditScreen(onBack = back) }

    // Filled in by later milestones.
    composable(Routes.STATS) {
        StatsScreen(onCategory = { id, ym -> nav.navigate(Routes.categoryDetail(id, BillMath.ym(ym))) }, onYear = { nav.navigate(Routes.year(it)) })
    }
    composable(Routes.BUDGET) { BudgetScreen() }
    composable(Routes.CATEGORY_DETAIL, arguments = listOf(longArg("id"), intArg("ym"))) {
        CategoryDetailScreen(onBack = back, onTxn = { nav.navigate(Routes.txn(it)) })
    }
    composable(Routes.YEAR, arguments = listOf(intArg("year"))) { YearScreen(onBack = back) }

    composable(Routes.PENDING) { PendingScreen(onBack = back) }
    composable(Routes.AUTOLOG) { AutoLogSettingsScreen(onBack = back, onParserTest = { nav.navigate(Routes.PARSER_TEST) }) }
    composable(Routes.PARSER_TEST) { ParserTestScreen(onBack = back) }

    composable(Routes.BILLS) { BillsScreen(onBack = back, onEdit = { nav.navigate(Routes.billEdit(it)) }) }
    composable(Routes.BILL_EDIT, arguments = listOf(longArg("id"))) { BillEditScreen(onBack = back) }
    composable(Routes.LOANS) { LoansScreen(onBack = back) }
    composable(Routes.REMINDERS) { RemindersScreen(onBack = back) }

    listOf(Routes.BACKUP, Routes.LOOK, Routes.ABOUT).forEach { r -> composable(r) { Placeholder(r) } }
}

@Composable
private fun Placeholder(r: String) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(r) }
