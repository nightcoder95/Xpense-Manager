package com.example.ui.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Category
import com.example.domain.model.TxnType
import com.example.domain.repository.CategoryRepository
import com.example.domain.usecase.ReorderCategoriesUseCase
import com.example.domain.usecase.SetDefaultCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val type: TxnType = TxnType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val defaultName: String? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repo: CategoryRepository,
    private val setDefault: SetDefaultCategoryUseCase,
    private val reorder: ReorderCategoriesUseCase
) : ViewModel() {

    private val type = MutableStateFlow(TxnType.EXPENSE)
    private val allCategories = repo.all()

    fun setType(t: TxnType) { type.value = t }

    val uiState: StateFlow<CategoriesUiState> =
        combine(allCategories, type) { all, t ->
            val ofType = all.filter { it.type == t }.sortedBy { it.sortOrder }
            CategoriesUiState(
                type = t,
                categories = ofType,
                defaultName = ofType.firstOrNull { it.isDefault }?.name
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoriesUiState())

    fun makeDefault(target: Category) {
        viewModelScope.launch { setDefault(uiState.value.categories, target) }
    }

    fun applyOrder(ordered: List<Category>) {
        viewModelScope.launch { reorder(ordered) }
    }
}
