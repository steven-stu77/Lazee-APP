package io.github.steven_stu77.lazee.feature.txn

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.Template
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.repo.PhotoStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TxnDetailUiState(
    val loaded: Boolean = false,
    val txn: Transaction? = null,
    val category: Category? = null,
    val parentCategory: Category? = null,
    val account: Account? = null,
    val toAccount: Account? = null,
    val tags: List<String> = emptyList(),
    val photo: File? = null,
    val deleted: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TxnDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val ledger: LedgerRepository,
    private val photos: PhotoStore,
) : ViewModel() {
    private val id: Long = handle.get<Long>("id") ?: 0L
    private val deleted = MutableStateFlow(false)

    val state: StateFlow<TxnDetailUiState> = combine(
        ledger.observeTransaction(id), ledger.observeCategories(), ledger.observeAccounts(), ledger.observeTagNames(), deleted,
    ) { t, cats, accs, tags, del ->
        val cat = cats.firstOrNull { it.id == t?.categoryId }
        TxnDetailUiState(
            loaded = true,
            txn = t,
            category = cat,
            parentCategory = cat?.parentId?.let { p -> cats.firstOrNull { it.id == p } },
            account = accs.firstOrNull { it.id == t?.accountId },
            toAccount = accs.firstOrNull { it.id == t?.toAccountId },
            tags = tags[id].orEmpty(),
            photo = t?.photoPath?.let(photos::file)?.takeIf { it.exists() },
            deleted = del,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TxnDetailUiState())

    fun delete() = viewModelScope.launch {
        val t = state.value.txn ?: return@launch
        ledger.deleteTransactions(listOf(t.id))
        photos.delete(t.photoPath)
        deleted.value = true
    }

    /** "撤销自动记账": removes an auto-logged record entirely. */
    fun undoAuto() = delete()

    /** Keeps an auto-logged record but marks it as confirmed by the user. */
    fun confirmAuto() = viewModelScope.launch {
        val t = state.value.txn ?: return@launch
        ledger.saveTransaction(t.copy(isAuto = false))
    }

    fun saveTemplate(name: String) = viewModelScope.launch {
        val t = state.value.txn ?: return@launch
        ledger.saveTemplate(Template(name = name.trim().take(20), type = t.type, amount = t.amount, categoryId = t.categoryId, accountId = t.accountId, note = t.note))
    }
}
