package com.example.domain

import com.example.domain.model.Category
import com.example.domain.model.TxnType
import com.example.domain.usecase.ReorderCategoriesUseCase
import com.example.domain.usecase.SetDefaultCategoryUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryAdminUseCasesTest {

    @Test fun `set default flips only target within type`() = runTest {
        val repo = FakeCategoryRepository()
        val a = Category("A", TxnType.EXPENSE, "i", "#1", isDefault = true)
        val b = Category("B", TxnType.EXPENSE, "i", "#2")
        val inc = Category("C", TxnType.INCOME, "i", "#3", isDefault = true)
        repo.seedDefaults(listOf(a, b, inc))

        SetDefaultCategoryUseCase(repo)(listOf(a, b, inc), b)

        val all = repo.all().first()
        assertTrue(all.first { it.name == "B" }.isDefault)
        assertTrue(!all.first { it.name == "A" }.isDefault)
        assertTrue(all.first { it.name == "C" }.isDefault) // income default untouched
    }

    @Test fun `reorder writes sequential sortOrder`() = runTest {
        val repo = FakeCategoryRepository()
        val x = Category("X", TxnType.EXPENSE, "i", "#1", sortOrder = 5)
        val y = Category("Y", TxnType.EXPENSE, "i", "#2", sortOrder = 9)
        repo.seedDefaults(listOf(x, y))

        ReorderCategoriesUseCase(repo)(listOf(y, x))

        val all = repo.all().first()
        assertEquals(0, all.first { it.name == "Y" }.sortOrder)
        assertEquals(1, all.first { it.name == "X" }.sortOrder)
    }
}
