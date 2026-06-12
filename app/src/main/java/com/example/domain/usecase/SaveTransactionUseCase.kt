package com.example.domain.usecase

import com.example.core.common.AppDispatchers
import com.example.domain.model.Transaction
import com.example.domain.model.TxnType
import com.example.domain.repository.TransactionRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ValidationResult(val isValid: Boolean, val error: String? = null)

class SaveTransactionUseCase @Inject constructor(
    private val repo: TransactionRepository,
    private val dispatchers: AppDispatchers
) {
    suspend operator fun invoke(t: Transaction): Result<Long> {
        val v = validate(t.amount, t.type, t.accountId, t.toAccountId)
        if (!v.isValid) return Result.failure(IllegalArgumentException(v.error))
        return runCatching {
            withContext(dispatchers.io) {
                if (t.id == 0L) repo.upsert(t) else { repo.update(t); t.id }
            }
        }
    }

    companion object {
        fun validate(amount: Double, type: TxnType, accountId: Long, toAccountId: Long?): ValidationResult = when {
            amount <= 0.0 -> ValidationResult(false, "Enter an amount greater than 0")
            type == TxnType.TRANSFER && toAccountId == null -> ValidationResult(false, "Choose a destination account")
            type == TxnType.TRANSFER && toAccountId == accountId -> ValidationResult(false, "Transfer accounts must differ")
            else -> ValidationResult(true)
        }
    }
}
