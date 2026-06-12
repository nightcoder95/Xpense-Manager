package com.example.ui.feature.editaccount

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Account
import com.example.domain.model.AccountType
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditAccountUiState(
    val isEditing: Boolean = false,
    val id: Long = 0,
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    val openingBalanceText: String = "",
    val colorHex: String = "#43C59E"
)

@HiltViewModel
class EditAccountViewModel @Inject constructor(
    private val repo: AccountRepository,
    private val txnRepo: TransactionRepository,
    handle: SavedStateHandle
) : ViewModel() {

    private val editingId: Long? = handle.get<String>("id")?.toLongOrNull()?.takeIf { it > 0 }
    private val _state = MutableStateFlow(EditAccountUiState(isEditing = editingId != null))
    val state: StateFlow<EditAccountUiState> = _state

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    init {
        if (editingId != null) viewModelScope.launch {
            repo.getById(editingId)?.let { a ->
                _state.value = EditAccountUiState(
                    isEditing = true,
                    id = a.id,
                    name = a.name,
                    type = a.type,
                    openingBalanceText = if (a.openingBalance == 0.0) "" else a.openingBalance.toString(),
                    colorHex = a.colorHex
                )
            }
        }
    }

    fun onName(v: String) { _state.value = _state.value.copy(name = v) }
    fun onType(v: AccountType) { _state.value = _state.value.copy(type = v) }
    fun onBalance(v: String) { _state.value = _state.value.copy(openingBalanceText = v.filter { it.isDigit() || it == '.' || it == '-' }) }
    fun onColor(v: String) { _state.value = _state.value.copy(colorHex = v) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repo.upsert(
                    Account(
                        id = s.id,
                        name = s.name.trim(),
                        type = s.type,
                        openingBalance = s.openingBalanceText.toDoubleOrNull() ?: 0.0,
                        iconName = iconForType(s.type),
                        colorHex = s.colorHex
                    )
                )
            }.onSuccess { onDone() }
                .onFailure { _messages.tryEmit("Could not save account") }
        }
    }

    fun delete(onDone: () -> Unit) {
        val s = _state.value
        if (!s.isEditing) { onDone(); return }
        viewModelScope.launch {
            // Block deletion while transactions still reference this account — otherwise they
            // become orphans (no FK constraint exists), corrupting balances and labels.
            val refs = runCatching { txnRepo.countByAccount(s.id) }.getOrDefault(0)
            if (refs > 0) {
                _messages.tryEmit("Can't delete: $refs transaction(s) use this account. Reassign or delete them first.")
                return@launch
            }
            runCatching { repo.delete(s.id) }
                .onSuccess { onDone() }
                .onFailure { _messages.tryEmit("Could not delete account") }
        }
    }

    private fun iconForType(t: AccountType) = when (t) {
        AccountType.CASH -> "cash"
        AccountType.BANK -> "bank"
        AccountType.CREDIT_CARD -> "card"
        AccountType.WALLET -> "wallet"
    }
}
