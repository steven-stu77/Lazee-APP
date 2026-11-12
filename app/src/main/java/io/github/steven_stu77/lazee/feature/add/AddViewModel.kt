package io.github.steven_stu77.lazee.feature.add

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.calc.Calculator
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.CategoryKind
import io.github.steven_stu77.lazee.core.data.Template
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TxnSource
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.BudgetAlerts
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.repo.PhotoStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

data class AddUiState(
    val type: TxnType = TxnType.EXPENSE,
    val calc: Calculator = Calculator(),
    val date: LocalDate = LocalDate.now(),
    val categories: List<Category> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val accounts: List<Account> = emptyList(),
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val note: String = "",
    val photo: String? = null,
    val templates: List<Template> = emptyList(),
    val editingId: Long = 0,
    val original: Transaction? = null,
    val divideByZero: Boolean = false,
    val savedAmount: Long? = null,
) {
    val amount: Long get() = calc.evaluate().cents
    val subCategories: List<Category> get() = allCategories.filter { it.parentId == categoryId && it.parentId != null && !it.archived }
    val canSave: Boolean get() = amount > 0 && when (type) {
        TxnType.TRANSFER -> accountId != null && toAccountId != null && accountId != toAccountId
        else -> categoryId != null && accountId != null
    }
}

@HiltViewModel
class AddViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val ledger: LedgerRepository,
    private val settings: SettingsRepository,
    private val photos: PhotoStore,
    private val budgetAlerts: BudgetAlerts,
) : ViewModel() {
    private val _state = MutableStateFlow(AddUiState())
    val state: StateFlow<AddUiState> = _state.asStateFlow()

    init {
        val txnId = handle.get<Long>("txnId") ?: 0L
        val copy = handle.get<Boolean>("copy") ?: false
        val presetCategory = handle.get<Long>("categoryId") ?: 0L
        viewModelScope.launch {
            combine(ledger.observeCategories(), ledger.observeAccounts(), ledger.observeTemplates()) { c, a, t -> Triple(c, a, t) }
                .collect { (cats, accs, temps) ->
                    _state.update { s ->
                        val active = accs.filter { !it.archived }
                        s.copy(
                            allCategories = cats,
                            categories = topLevel(cats, s.type),
                            accounts = active,
                            templates = temps.take(5),
                            accountId = s.accountId ?: settings.current().lastAccountId.takeIf { id -> active.any { it.id == id } } ?: active.firstOrNull()?.id,
                            categoryId = s.categoryId ?: if (presetCategory > 0) presetCategory else topLevel(cats, s.type).firstOrNull()?.id,
                        )
                    }
                }
        }
        if (txnId > 0) viewModelScope.launch { load(txnId, copy) }
    }

    private fun topLevel(all: List<Category>, type: TxnType): List<Category> {
        val kind = if (type == TxnType.INCOME) CategoryKind.INCOME else CategoryKind.EXPENSE
        return all.filter { it.kind == kind && it.parentId == null && !it.archived }
    }

    private suspend fun load(id: Long, copy: Boolean) {
        val t = ledger.transaction(id) ?: return
        val cat = t.categoryId?.let { ledger.category(it) }
        _state.update {
            it.copy(
                type = t.type,
                calc = Calculator.fromCents(t.amount),
                date = if (copy) LocalDate.now() else t.date,
                categoryId = cat?.parentId ?: cat?.id,
                subCategoryId = if (cat?.parentId != null) cat.id else null,
                categories = topLevel(it.allCategories, t.type),
                accountId = t.accountId,
                toAccountId = t.toAccountId,
                note = t.note.orEmpty(),
                photo = if (copy) null else t.photoPath,
                editingId = if (copy) 0 else t.id,
                original = if (copy) null else t,
            )
        }
    }

    fun setType(type: TxnType) = _state.update {
        val cats = topLevel(it.allCategories, type)
        it.copy(type = type, categories = cats, categoryId = cats.firstOrNull()?.id, subCategoryId = null)
    }

    fun press(key: Char) = _state.update { it.copy(calc = it.calc.press(key), divideByZero = false) }

    fun calculate() = _state.update {
        val r = it.calc.evaluate()
        it.copy(calc = it.calc.collapse(), divideByZero = r.divideByZero)
    }

    /** Tap: today → yesterday → 2 days ago → today. */
    fun cycleDate() = _state.update {
        val today = LocalDate.now()
        val next = when (it.date) {
            today -> today.minusDays(1)
            today.minusDays(1) -> today.minusDays(2)
            else -> today
        }
        it.copy(date = next)
    }

    fun setDate(d: LocalDate) = _state.update { it.copy(date = d) }
    fun pickCategory(id: Long) = _state.update { it.copy(categoryId = id, subCategoryId = null) }
    fun pickSub(id: Long) = _state.update { it.copy(subCategoryId = if (it.subCategoryId == id) null else id) }
    fun pickAccount(id: Long) = _state.update { it.copy(accountId = id) }
    fun pickToAccount(id: Long) = _state.update { it.copy(toAccountId = id) }
    fun setNote(n: String) = _state.update { it.copy(note = n.take(200)) }

    /** Called when the new-category page returns its id. */
    fun categoryCreated(id: Long) = _state.update { it.copy(categoryId = id, subCategoryId = null) }

    fun applyTemplate(t: Template) = _state.update {
        val cats = topLevel(it.allCategories, t.type)
        val cat = it.allCategories.firstOrNull { c -> c.id == t.categoryId }
        it.copy(
            type = t.type, categories = cats,
            categoryId = cat?.parentId ?: cat?.id ?: it.categoryId,
            subCategoryId = if (cat?.parentId != null) cat.id else null,
            accountId = t.accountId, note = t.note.orEmpty(),
            calc = if (t.amount != null) Calculator.fromCents(t.amount) else it.calc,
        )
    }

    fun attachPhoto(uri: Uri) = viewModelScope.launch {
        val name = photos.import(uri) ?: return@launch
        _state.value.photo?.takeIf { it != _state.value.original?.photoPath }?.let(photos::delete)
        _state.update { it.copy(photo = name) }
    }

    fun removePhoto() = _state.update { it.copy(photo = null) }
    fun cameraUri(): Uri = photos.newCameraUri()

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val base = s.original
            val t = Transaction(
                id = s.editingId,
                type = s.type,
                amount = s.amount,
                categoryId = if (s.type == TxnType.TRANSFER) null else (s.subCategoryId ?: s.categoryId),
                accountId = s.accountId!!,
                toAccountId = if (s.type == TxnType.TRANSFER) s.toAccountId else null,
                date = s.date,
                createdAt = base?.createdAt ?: Instant.now(),
                note = s.note.trim().ifEmpty { null },
                merchant = base?.merchant,
                source = base?.source ?: TxnSource.MANUAL,
                // Editing an auto-logged record counts as the user confirming it.
                isAuto = false,
                billId = base?.billId,
                loanId = base?.loanId,
                photoPath = s.photo,
            )
            ledger.saveTransaction(t)
            if (base?.photoPath != null && base.photoPath != s.photo) photos.delete(base.photoPath)
            settings.setLastAccountId(t.accountId)
            if (t.type == TxnType.EXPENSE) budgetAlerts.check(t.date)
            _state.update { it.copy(savedAmount = t.amount) }
        }
    }
}
