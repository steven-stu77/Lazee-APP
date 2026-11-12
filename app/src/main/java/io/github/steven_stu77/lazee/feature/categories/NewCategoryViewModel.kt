package io.github.steven_stu77.lazee.feature.categories

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.CategoryKind
import io.github.steven_stu77.lazee.core.logic.CategoryRules
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.ui.Labels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewCategoryUiState(
    val name: String = "",
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val colorIndex: Int = 0,
    val parentId: Long? = null,
    val parents: List<Category> = emptyList(),
    val all: List<Category> = emptyList(),
    val editing: Category? = null,
    val savedId: Long? = null,
) {
    val isEdit: Boolean get() = editing != null
}

@HiltViewModel
class NewCategoryViewModel @Inject constructor(
    handle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val ledger: LedgerRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(NewCategoryUiState())
    val state: StateFlow<NewCategoryUiState> = _state.asStateFlow()

    init {
        val kind = runCatching { CategoryKind.valueOf(handle.get<String>("kind") ?: "EXPENSE") }.getOrDefault(CategoryKind.EXPENSE)
        val parent = handle.get<Long>("parentId")?.takeIf { it > 0 }
        val editId = handle.get<Long>("editId")?.takeIf { it > 0 }
        viewModelScope.launch {
            val all = ledger.allCategories()
            val editing = editId?.let { id -> all.firstOrNull { it.id == id } }
            _state.update {
                it.copy(
                    all = all,
                    kind = editing?.kind ?: kind,
                    parentId = editing?.parentId ?: parent,
                    editing = editing,
                    name = editing?.let { c -> Labels.categoryName(context, c) } ?: "",
                    colorIndex = editing?.colorIndex ?: (all.count { c -> c.kind == kind } % 8),
                ).withParents()
            }
        }
    }

    private fun NewCategoryUiState.withParents() =
        copy(parents = all.filter { it.kind == kind && it.parentId == null && !it.archived && it.id != editing?.id })

    fun setName(n: String) = _state.update { it.copy(name = CategoryRules.clamp(n)) }
    fun setKind(k: CategoryKind) = _state.update { if (it.isEdit) it else it.copy(kind = k, parentId = null).withParents() }
    fun setColor(i: Int) = _state.update { it.copy(colorIndex = i) }
    fun setParent(id: Long?) = _state.update { if (it.isEdit) it else it.copy(parentId = id) }

    /** Same-kind names already taken (translated built-ins included). */
    fun isDuplicate(s: NewCategoryUiState = _state.value): Boolean = CategoryRules.isDuplicate(
        s.name,
        s.all.filter { it.kind == s.kind && it.id != s.editing?.id }.map { Labels.categoryName(context, it) },
    )

    fun save() {
        val s = _state.value
        if (s.name.isBlank() || isDuplicate(s)) return
        viewModelScope.launch {
            val e = s.editing
            val c = if (e != null) {
                // A built-in renamed by the user keeps its key but shows the custom name.
                val renamed = s.name.trim() != Labels.categoryName(context, e)
                e.copy(customName = if (renamed || e.builtInKey == null) s.name.trim() else e.customName, colorIndex = s.colorIndex)
            } else Category(customName = s.name.trim(), kind = s.kind, colorIndex = s.colorIndex, parentId = s.parentId)
            val id = ledger.saveCategory(c)
            _state.update { it.copy(savedId = id) }
        }
    }
}
