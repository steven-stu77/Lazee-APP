package io.github.steven_stu77.lazee.feature.loans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.Loan
import io.github.steven_stu77.lazee.core.data.LoanDirection
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.repo.LoanRepository
import io.github.steven_stu77.lazee.core.repo.LoanView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LoansUiState(
    val direction: LoanDirection = LoanDirection.LENT,
    val loans: List<LoanView> = emptyList(),
    val outstanding: Long = 0,
    val accounts: List<Account> = emptyList(),
)

@HiltViewModel
class LoansViewModel @Inject constructor(private val loans: LoanRepository, ledger: LedgerRepository) : ViewModel() {
    private val direction = MutableStateFlow(LoanDirection.LENT)

    val state: StateFlow<LoansUiState> = combine(loans.observeLoans(), direction, ledger.observeAccounts()) { all, d, accs ->
        val mine = all.filter { it.loan.direction == d }
        LoansUiState(d, mine, mine.filter { !it.loan.settled }.sumOf { it.outstanding }, accs.filter { !it.archived })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoansUiState())

    fun setDirection(d: LoanDirection) { direction.value = d }

    fun create(person: String, principal: Long, rate: Double?, due: LocalDate?, accountId: Long) = viewModelScope.launch {
        loans.create(Loan(person = person.trim(), direction = direction.value, principal = principal, annualRatePercent = rate,
            startDate = LocalDate.now(), dueDate = due, accountId = accountId))
    }

    fun repay(loanId: Long, amount: Long) = viewModelScope.launch { loans.repay(loanId, amount, LocalDate.now()) }
}
