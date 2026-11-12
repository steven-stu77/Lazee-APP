package io.github.steven_stu77.lazee.feature.bills

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.Bill
import io.github.steven_stu77.lazee.core.data.BillMode
import io.github.steven_stu77.lazee.core.data.BuiltIns
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.CategoryKind
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.repo.BillRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.feature.onboarding.filterMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class BillEditUiState(
    val original: Bill? = null,
    val name: String = "",
    val amount: String = "",
    val day: String = "",
    val remind: String = "2",
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val mode: BillMode = BillMode.REMIND,
    val subscription: Boolean = false,
    val installments: String = "",
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val done: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && (Money.parse(amount) ?: 0) > 0 && day.toIntOrNull() in 1..31 && categoryId != null
}

@HiltViewModel
class BillEditViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val bills: BillRepository,
    private val ledger: LedgerRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BillEditUiState())
    val state: StateFlow<BillEditUiState> = _state.asStateFlow()

    init {
        val id = handle.get<Long>("id") ?: 0L
        viewModelScope.launch {
            val cats = ledger.allCategories().filter { it.kind == CategoryKind.EXPENSE && !it.archived }
            val accs = ledger.allAccounts().filter { !it.archived }
            val b = if (id > 0) bills.bill(id) else null
            _state.update {
                it.copy(
                    categories = cats, accounts = accs, original = b,
                    name = b?.name.orEmpty(), amount = b?.let { x -> Money.toInput(x.amount) }.orEmpty(),
                    day = b?.dayOfMonth?.toString().orEmpty(), remind = (b?.remindDaysBefore ?: 2).toString(),
                    categoryId = b?.categoryId ?: cats.firstOrNull { c -> c.builtInKey == BuiltIns.BILLS }?.id,
                    accountId = b?.accountId, mode = b?.mode ?: BillMode.REMIND, subscription = b?.isSubscription ?: false,
                    installments = b?.totalInstallments?.toString().orEmpty(),
                )
            }
        }
    }

    fun update(block: (BillEditUiState) -> BillEditUiState) = _state.update(block)
    fun setAmount(v: String) = _state.update { it.copy(amount = v.filterMoney()) }
    fun setDay(v: String) = _state.update { it.copy(day = v.filter(Char::isDigit).take(2)) }
    fun setRemind(v: String) = _state.update { it.copy(remind = v.filter(Char::isDigit).take(2)) }
    fun setInstallments(v: String) = _state.update { it.copy(installments = v.filter(Char::isDigit).take(3)) }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val base = s.original ?: Bill(name = "", amount = 0, dayOfMonth = 1, categoryId = 0, startMonth = BillMath.ym(YearMonth.now()))
            bills.save(
                base.copy(
                    name = s.name.trim(), amount = Money.parse(s.amount) ?: 0, dayOfMonth = s.day.toInt().coerceIn(1, 31),
                    remindDaysBefore = (s.remind.toIntOrNull() ?: 2).coerceIn(0, 28), mode = s.mode, categoryId = s.categoryId!!,
                    accountId = s.accountId, isSubscription = s.subscription,
                    totalInstallments = s.installments.toIntOrNull()?.takeIf { it > 0 }, active = true,
                ),
            )
            _state.update { it.copy(done = true) }
        }
    }

    fun delete() = viewModelScope.launch {
        _state.value.original?.let { bills.delete(it) }
        _state.update { it.copy(done = true) }
    }
}
