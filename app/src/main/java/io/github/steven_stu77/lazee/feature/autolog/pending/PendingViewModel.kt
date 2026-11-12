package io.github.steven_stu77.lazee.feature.autolog.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.CategoryKind
import io.github.steven_stu77.lazee.core.data.PendingDirection
import io.github.steven_stu77.lazee.core.data.PendingTxn
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.repo.BillRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.repo.PendingRepository
import io.github.steven_stu77.lazee.feature.autolog.AutoLogConfig
import io.github.steven_stu77.lazee.feature.autolog.AutoLogNotifications
import io.github.steven_stu77.lazee.feature.autolog.AutoLogRepository
import io.github.steven_stu77.lazee.core.data.Bill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingCard(
    val item: PendingTxn,
    val appName: String,
    val category: Category?,
    val bill: Bill?,
    val duplicateOf: Transaction?,
    val alwaysAsk: Boolean,
) {
    val isTransfer: Boolean get() = item.transferPairId != null
}

data class PendingUiState(
    val cards: List<PendingCard> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class PendingViewModel @Inject constructor(
    pending: PendingRepository,
    private val ledger: LedgerRepository,
    bills: BillRepository,
    private val repo: AutoLogRepository,
    private val config: AutoLogConfig,
    private val notifications: AutoLogNotifications,
) : ViewModel() {
    /** Per-card choices the user made before confirming: category override and "always ask". */
    private val overrides = MutableStateFlow<Map<Long, Pair<Long?, Boolean>>>(emptyMap())

    val state: StateFlow<PendingUiState> = combine(
        pending.observePending(), ledger.observeCategories(), bills.observeBills(), ledger.observeAllTransactions(), overrides,
    ) { items, cats, billList, txns, ov ->
        val catMap = cats.associateBy { it.id }
        PendingUiState(
            cards = items.map { p ->
                val o = ov[p.id]
                PendingCard(
                    item = p,
                    appName = config.engine.config(p.sourcePackage)?.displayName ?: p.sourcePackage,
                    category = catMap[o?.first ?: p.suggestedCategoryId],
                    bill = billList.firstOrNull { it.id == p.matchedBillId },
                    duplicateOf = txns.firstOrNull { it.id == p.possibleDuplicateOf },
                    alwaysAsk = o?.second ?: (repo.rule(p.merchant)?.alwaysAsk ?: false),
                )
            },
            expenseCategories = cats.filter { it.kind == CategoryKind.EXPENSE && !it.archived },
            incomeCategories = cats.filter { it.kind == CategoryKind.INCOME && !it.archived },
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingUiState())

    fun setCategory(id: Long, categoryId: Long) = overrides.update { it + (id to (categoryId to (it[id]?.second ?: false))) }
    fun setAlwaysAsk(id: Long, on: Boolean) = overrides.update { it + (id to ((it[id]?.first) to on)) }

    fun confirm(card: PendingCard) = viewModelScope.launch {
        repo.confirm(card.item.id, card.category?.id, card.alwaysAsk)
        notifications.cancelPending(card.item.id)
    }

    fun ignore(card: PendingCard) = viewModelScope.launch {
        repo.ignore(card.item.id)
        notifications.cancelPending(card.item.id)
    }

    fun categoriesFor(card: PendingCard) = if (card.item.direction == PendingDirection.IN && !card.item.isRefund) state.value.incomeCategories else state.value.expenseCategories
}
