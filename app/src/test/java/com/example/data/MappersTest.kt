package com.example.data

import com.example.data.local.entity.TransactionEntity
import com.example.data.local.toDomain
import com.example.domain.model.TxnType
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {
    @Test fun transactionEntity_mapsToDomain() {
        val e = TransactionEntity(1, "EXPENSE", 100.0, "Food", 2, null, 123L, "n", "t")
        val d = e.toDomain()
        assertEquals(TxnType.EXPENSE, d.type)
        assertEquals(2L, d.accountId)
        assertEquals(100.0, d.amount, 0.0)
    }
}
