package io.github.steven_stu77.lazee.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.CategoryKind
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val expense: List<Category> = emptyList(),
    val income: List<Category> = emptyList(),
    val subs: Map<Long, List<Category>> = emptyMap(),
    val archived: List<Category> = emptyList(),
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(private val ledger: LedgerRepository) : ViewModel() {
    val state: StateFlow<CategoriesUiState> = ledger.observeCategories().map { all ->
        val live = all.filter { !it.archived }
        CategoriesUiState(
            expense = live.filter { it.kind == CategoryKind.EXPENSE && it.parentId == null },
            income = live.filter { it.kind == CategoryKind.INCOME && it.parentId == null },
            subs = live.filter { it.parentId != null }.groupBy { it.parentId!! },
            archived = all.filter { it.archived },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    /** Moves a top-level category one step within its group; delta is -1 or +1. */
    fun move(c: Category, delta: Int) = viewModelScope.launch {
        val s = state.value
        val group = (if (c.kind == CategoryKind.EXPENSE) s.expense else s.income).toMutableList()
        val i = group.indexOfFirst { it.id == c.id }
        val j = i + delta
        if (i < 0 || j !in group.indices) return@launch
        group[i] = group[j].also { group[j] = group[i] }
        val other = if (c.kind == CategoryKind.EXPENSE) s.income else s.expense
        // Expense group first, then income, so sortOrder stays unique.
        ledger.reorderCategories(if (c.kind == CategoryKind.EXPENSE) group + other else other + group)
    }

    /** Built-ins can only be archived, never deleted; history keeps pointing at the archived row. */
    fun setArchived(c: Category, archived: Boolean) = viewModelScope.launch { ledger.saveCategory(c.copy(archived = archived)) }
}
