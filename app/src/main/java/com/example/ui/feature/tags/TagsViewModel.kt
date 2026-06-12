package com.example.ui.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TagCount(val tag: String, val count: Int)

@HiltViewModel
class TagsViewModel @Inject constructor(
    txnRepo: TransactionRepository
) : ViewModel() {

    val tags: StateFlow<List<TagCount>> = txnRepo.all()
        .map { txns ->
            txns.filter { it.tag.isNotBlank() }
                .groupingBy { it.tag }.eachCount()
                .map { (tag, count) -> TagCount(tag, count) }
                .sortedByDescending { it.count }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
