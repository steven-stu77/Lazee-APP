package io.github.steven_stu77.lazee.feature.bills

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.repo.BillRepository
import io.github.steven_stu77.lazee.core.repo.BillStatus
import io.github.steven_stu77.lazee.core.ui.components.ToastBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BillsUiState(
    val subscriptionsOnly: Boolean = false,
    val items: List<BillStatus> = emptyList(),
    val subscriptionTotal: Long = 0,
    val today: LocalDate = LocalDate.now(),
)

@HiltViewModel
class BillsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bills: BillRepository,
) : ViewModel() {
    private val subsOnly = MutableStateFlow(false)

    val state: StateFlow<BillsUiState> = combine(bills.observeMonth(), subsOnly) { list, subs ->
        BillsUiState(
            subscriptionsOnly = subs,
            items = if (subs) list.filter { it.bill.isSubscription } else list,
            subscriptionTotal = list.filter { it.bill.isSubscription }.sumOf { it.bill.amount },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillsUiState())

    fun setSubscriptionsOnly(on: Boolean) { subsOnly.value = on }

    fun markPaid(b: BillStatus) = viewModelScope.launch {
        if (bills.markPaid(b.bill.id) != null) ToastBus.show(context.getString(R.string.bills_marked_paid, Money.format(b.bill.amount)))
    }
}
