package io.github.steven_stu77.lazee.feature.accounts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.AccountType
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.feature.onboarding.filterMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountEditUiState(
    val original: Account? = null,
    val name: String = "",
    val type: AccountType = AccountType.EWALLET,
    val opening: String = "",
    val negativeOpening: Boolean = false,
    val statementDay: String = "",
    val dueDay: String = "",
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class AccountEditViewModel @Inject constructor(handle: SavedStateHandle, private val ledger: LedgerRepository) : ViewModel() {
    private val _state = MutableStateFlow(AccountEditUiState())
    val state: StateFlow<AccountEditUiState> = _state.asStateFlow()

    init {
        val id = handle.get<Long>("id") ?: 0L
        if (id > 0) viewModelScope.launch {
            val a = ledger.account(id) ?: return@launch
            _state.update {
                it.copy(original = a, name = a.name, type = a.type, opening = Money.toInput(kotlin.math.abs(a.openingBalance)),
                    negativeOpening = a.openingBalance < 0, statementDay = a.statementDay?.toString().orEmpty(), dueDay = a.dueDay?.toString().orEmpty())
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v.take(24)) }
    fun setType(v: AccountType) = _state.update { it.copy(type = v) }
    fun setOpening(v: String) = _state.update { it.copy(opening = v.filterMoney()) }
    fun setNegative(v: Boolean) = _state.update { it.copy(negativeOpening = v) }
    fun setStatementDay(v: String) = _state.update { it.copy(statementDay = v.dayOnly()) }
    fun setDueDay(v: String) = _state.update { it.copy(dueDay = v.dayOnly()) }

    private fun String.dayOnly() = filter(Char::isDigit).take(2).let { d -> d.toIntOrNull()?.coerceIn(1, 31)?.toString() ?: "" }

    fun save(archive: Boolean? = null) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val opening = (Money.parse(s.opening) ?: 0).let { if (s.negativeOpening) -it else it }
            val card = s.type == AccountType.CREDIT_CARD
            val a = (s.original ?: Account(name = s.name, type = s.type)).copy(
                name = s.name.trim(), type = s.type, openingBalance = opening,
                statementDay = if (card) s.statementDay.toIntOrNull() else null,
                dueDay = if (card) s.dueDay.toIntOrNull() else null,
                archived = archive ?: s.original?.archived ?: false,
            )
            ledger.saveAccount(a)
            _state.update { it.copy(saved = true) }
        }
    }
}
