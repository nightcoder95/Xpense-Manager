package com.example.ui.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AccountBalance
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.TransactionRepository
import com.example.domain.usecase.AccountBalancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AccountsUiState(
    val balances: List<AccountBalance> = emptyList(),
    val totalNet: Double = 0.0
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    accountRepo: AccountRepository,
    txnRepo: TransactionRepository
) : ViewModel() {

    private val balancesUseCase = AccountBalancesUseCase()

    val uiState: StateFlow<AccountsUiState> =
        combine(accountRepo.all(), txnRepo.all()) { accounts, txns ->
            val balances = balancesUseCase.from(accounts.filterNot { it.archived }, txns)
            AccountsUiState(balances = balances, totalNet = balances.sumOf { it.balance })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountsUiState())
}
