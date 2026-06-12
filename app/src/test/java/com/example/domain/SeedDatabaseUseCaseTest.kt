package com.example.domain

import com.example.domain.usecase.SeedDatabaseUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SeedDatabaseUseCaseTest {
    @Test fun seedsOnlyWhenEmpty_andIsIdempotent() = runTest {
        val cat = FakeCategoryRepository()
        val acc = FakeAccountRepository()
        val uc = SeedDatabaseUseCase(cat, acc)

        uc()
        val afterFirst = cat.count()
        uc()
        val afterSecond = cat.count()

        assertEquals(afterFirst, afterSecond) // second call no-ops
        assertEquals(18, afterFirst)          // 18 default categories
        assertEquals(3, acc.count())          // 3 default accounts
    }
}
