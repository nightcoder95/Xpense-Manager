package com.example.domain.usecase

import com.example.domain.model.Category
import com.example.domain.model.TxnType
import com.example.domain.repository.CategoryRepository
import javax.inject.Inject

/** Marks one category default within its [TxnType], clearing the flag on the others. */
class SetDefaultCategoryUseCase @Inject constructor(private val repo: CategoryRepository) {
    suspend operator fun invoke(all: List<Category>, target: Category) {
        all.filter { it.type == target.type }.forEach { c ->
            val shouldBe = c.name == target.name
            if (c.isDefault != shouldBe) repo.upsert(c.copy(isDefault = shouldBe))
        }
    }
}

/** Persists a new ordering by writing each category's index as its [Category.sortOrder]. */
class ReorderCategoriesUseCase @Inject constructor(private val repo: CategoryRepository) {
    suspend operator fun invoke(ordered: List<Category>) {
        ordered.forEachIndexed { index, c ->
            if (c.sortOrder != index) repo.upsert(c.copy(sortOrder = index))
        }
    }
}
