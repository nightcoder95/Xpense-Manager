package com.example.domain.usecase

import com.example.data.local.DefaultCategories
import com.example.data.local.toDomain
import com.example.domain.repository.AccountRepository
import com.example.domain.repository.CategoryRepository
import javax.inject.Inject

/** Single, idempotent seed path. Replaces the old double-seed (Room callback + VM init). */
class SeedDatabaseUseCase @Inject constructor(
    private val categories: CategoryRepository,
    private val accounts: AccountRepository
) {
    suspend operator fun invoke() {
        if (categories.count() == 0) categories.seedDefaults(DefaultCategories.list.map { it.toDomain() })
        if (accounts.count() == 0) accounts.seedDefaults(DefaultCategories.accounts.map { it.toDomain() })
    }
}
