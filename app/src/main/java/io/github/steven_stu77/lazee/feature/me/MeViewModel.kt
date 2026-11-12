package io.github.steven_stu77.lazee.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.prefs.Settings
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.AccountWithBalance
import io.github.steven_stu77.lazee.core.repo.BillRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.repo.LoanRepository
import io.github.steven_stu77.lazee.core.repo.isLiability
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val assets: Long = 0,
    val liabilities: Long = 0,
    val unpaidBills: Int = 0,
    val openLoans: Int = 0,
    val categoryCount: Int = 0,
    val settings: Settings = Settings(),
) {
    val netWorth: Long get() = assets - liabilities
}

@HiltViewModel
class MeViewModel @Inject constructor(
    ledger: LedgerRepository,
    bills: BillRepository,
    loans: LoanRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<MeUiState> = combine(
        ledger.observeAccountsWithBalance(), bills.observeMonth(), loans.observeOutstanding(), ledger.observeCategories(), settings.settings,
    ) { accs, billList, loanTotals, cats, s ->
        val live = accs.filter { !it.account.archived }
        // Credit card balances are negative when you owe; that debt is a liability.
        val cardDebt = live.filter { it.account.isLiability() }.sumOf { (-it.balance).coerceAtLeast(0) }
        val assets = live.filter { !it.account.isLiability() }.sumOf { it.balance } + loanTotals.lent
        MeUiState(
            accounts = live,
            assets = assets,
            liabilities = cardDebt + loanTotals.borrowed,
            unpaidBills = billList.count { !it.paid },
            openLoans = loanTotals.openCount,
            categoryCount = cats.count { !it.archived },
            settings = s,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeUiState())

    fun setAppLock(on: Boolean) = viewModelScope.launch { settings.setAppLock(on) }
}
