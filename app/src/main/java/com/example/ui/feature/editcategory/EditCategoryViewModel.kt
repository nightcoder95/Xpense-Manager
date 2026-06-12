package com.example.ui.feature.editcategory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Category
import com.example.domain.model.TxnType
import com.example.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditCategoryUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val colorHex: String = "#6C5DD3",
    val iconName: String = "others",
    val type: TxnType = TxnType.EXPENSE
)

@HiltViewModel
class EditCategoryViewModel @Inject constructor(
    private val repo: CategoryRepository,
    handle: SavedStateHandle
) : ViewModel() {

    private val editingName: String? = handle.get<String>("name")?.takeIf { it.isNotBlank() }
    private val _state = MutableStateFlow(EditCategoryUiState(isEditing = editingName != null))
    val state: StateFlow<EditCategoryUiState> = _state

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    init {
        if (editingName != null) viewModelScope.launch {
            repo.all().first().firstOrNull { it.name == editingName }?.let { c ->
                _state.value = EditCategoryUiState(true, c.name, c.colorHex, c.iconName, c.type)
            }
        }
    }

    fun onName(v: String) { _state.value = _state.value.copy(name = v) }
    fun onColor(v: String) { _state.value = _state.value.copy(colorHex = v) }
    fun onIcon(v: String) { _state.value = _state.value.copy(iconName = v) }
    fun onType(v: TxnType) { _state.value = _state.value.copy(type = v) }

    fun canSave(): Boolean = _state.value.name.isNotBlank()

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            runCatching { repo.upsert(Category(s.name, s.type, s.iconName, s.colorHex)) }
                .onSuccess { onDone() }
                .onFailure { _messages.tryEmit("Could not save category") }
        }
    }

    fun delete(onDone: () -> Unit) {
        val s = _state.value
        if (!s.isEditing) { onDone(); return }
        viewModelScope.launch {
            runCatching { repo.delete(Category(s.name, s.type, s.iconName, s.colorHex)) }
                .onSuccess { onDone() }
                .onFailure { _messages.tryEmit("Could not delete category") }
        }
    }
}
