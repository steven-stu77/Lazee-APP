package io.github.steven_stu77.lazee.feature.accounts

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.AccountType
import io.github.steven_stu77.lazee.core.data.Bill
import io.github.steven_stu77.lazee.core.data.BuiltIns
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.core.logic.CardMath
import io.github.steven_stu77.lazee.core.logic.LedgerMath
import io.github.steven_stu77.lazee.core.repo.BillRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.repo.toEntry
import io.github.steven_stu77.lazee.core.ui.components.ToastBus
import io.github.steven_stu77.lazee.feature.home.TxnRow
import io.github.steven_stu77.lazee.feature.home.toRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class AccountDetailUiState(
    val account: Account? = null,
    val balance: Long = 0,
    val amountDue: Long? = null,
    val hasBill: Boolean = false,
    val groups: List<LedgerMath.DayGroup<TxnRow>> = emptyList(),
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val ledger: LedgerRepository,
    private val bills: BillRepository,
) : ViewModel() {
    private val id = handle.get<Long>("id") ?: 0L

    val state: StateFlow<AccountDetailUiState> = combine(
        ledger.observeAllTransactions(), ledger.observeAccounts(), ledger.observeCategories(), bills.observeBills(),
    ) { txns, accs, cats, billList ->
        val a = accs.firstOrNull { it.id == id }
        val mine = txns.filter { it.accountId == id || it.toAccountId == id }
        val entries = mine.map { it.toEntry() }
        val today = LocalDate.now()
        AccountDetailUiState(
            account = a,
            balance = a?.let { LedgerMath.balance(id, it.openingBalance, entries) } ?: 0,
            amountDue = a?.statementDay?.takeIf { a.type == AccountType.CREDIT_CARD }?.let { CardMath.amountDue(id, a.openingBalance, entries, it, today) },
            hasBill = billList.any { it.active && it.name == billName(a) },
            groups = LedgerMath.groupByDay(
                mine.map { it.toRow(context, cats.associateBy { c -> c.id }, accs.associateBy { x -> x.id }) }, { it.date },
                { if (it.type == TxnType.EXPENSE) it.amount else 0L },
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailUiState())

    private fun billName(a: Account?) = a?.let { context.getString(R.string.acc_card_bill_name, it.name) }

    /** One tap: a monthly "card repayment" reminder on the card's due day. */
    fun createCardBill() = viewModelScope.launch {
        val s = state.value
        val a = s.account ?: return@launch
        val cat = ledger.categoryByKey(BuiltIns.BILLS) ?: return@launch
        bills.save(
            Bill(name = billName(a)!!, amount = s.amountDue ?: 0, dayOfMonth = a.dueDay ?: 15, categoryId = cat.id,
                startMonth = BillMath.ym(YearMonth.now())),
        )
        ToastBus.show(context.getString(R.string.acc_card_bill_made))
    }
}
