package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.usecase.SeedDatabaseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppBootstrapViewModel @Inject constructor(
    seed: SeedDatabaseUseCase
) : ViewModel() {
    init { viewModelScope.launch { seed() } }
}
