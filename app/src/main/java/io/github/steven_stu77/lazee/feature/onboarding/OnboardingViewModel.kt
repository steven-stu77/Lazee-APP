package io.github.steven_stu77.lazee.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.AccountType
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountOption(val key: String, val type: AccountType, val checked: Boolean, val balance: String = "")

data class OnboardingUiState(
    val step: Int = 0,
    val language: String = "",
    val options: List<AccountOption> = listOf(
        AccountOption("cash", AccountType.CASH, true),
        AccountOption("TNG eWallet", AccountType.EWALLET, false),
        AccountOption("MAE", AccountType.BANK, false),
        AccountOption("bank", AccountType.BANK, false),
        AccountOption("card", AccountType.CREDIT_CARD, false),
    ),
    val budget: String = "",
    val finished: Boolean = false,
) {
    companion object { const val STEPS = 4 }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val ledger: LedgerRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun setLanguage(lang: String) {
        _state.update { it.copy(language = lang) }
        viewModelScope.launch { settings.setLanguage(lang) }
    }

    fun toggle(i: Int) = _state.update { s ->
        s.copy(options = s.options.mapIndexed { j, o -> if (i == j) o.copy(checked = !o.checked) else o })
    }

    fun setBalance(i: Int, text: String) = _state.update { s ->
        s.copy(options = s.options.mapIndexed { j, o -> if (i == j) o.copy(balance = text.filterMoney()) else o })
    }

    fun setBudget(text: String) = _state.update { it.copy(budget = text.filterMoney()) }
    fun next() = _state.update { it.copy(step = (it.step + 1).coerceAtMost(OnboardingUiState.STEPS)) }
    fun back() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    /** Saves everything; [names] maps option keys to the display name in the chosen language. */
    fun finish(names: Map<String, String>) {
        val s = _state.value
        viewModelScope.launch {
            if (!ledger.isSeeded()) {
                val accounts = s.options.filter { it.checked }.map {
                    Account(name = names[it.key] ?: it.key, type = it.type, openingBalance = Money.parse(it.balance) ?: 0)
                }.ifEmpty { listOf(Account(name = names["cash"] ?: "Cash", type = AccountType.CASH)) }
                ledger.seed(accounts)
            }
            settings.setMonthlyBudget(Money.parse(s.budget) ?: 0)
            ledger.allAccounts().firstOrNull()?.let { settings.setLastAccountId(it.id) }
            settings.setOnboardingDone(true)
            _state.update { it.copy(finished = true) }
        }
    }
}

/** Keeps digits and one dot with at most 2 decimals. */
fun String.filterMoney(): String {
    val cleaned = filter { it.isDigit() || it == '.' }
    val dot = cleaned.indexOf('.')
    if (dot < 0) return cleaned.take(7)
    return cleaned.substring(0, dot).take(7) + "." + cleaned.substring(dot + 1).replace(".", "").take(2)
}
