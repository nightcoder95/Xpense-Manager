package com.example.ui.screens.addtransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Holds the in-progress Add/Edit transaction form in [SavedStateHandle] so the entry
 * survives configuration change and process death. Encapsulates the amount-keypad rules
 * (single dot, leading-zero replace, DEL) previously inlined in AddTransactionSheet.
 */
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val handle: SavedStateHandle
) : ViewModel() {
    val type = handle.getStateFlow("type", "EXPENSE")
    val amount = handle.getStateFlow("amount", "")
    val category = handle.getStateFlow("category", "Others")
    val paymentMode = handle.getStateFlow("paymentMode", "Cash")
    val dateMillis = handle.getStateFlow("date", System.currentTimeMillis())
    val note = handle.getStateFlow("note", "")
    val tag = handle.getStateFlow("tag", "")

    fun onTypeChange(v: String) {
        handle["type"] = v
        handle["category"] = when (v) {
            "INCOME" -> "Salary"
            "TRANSFER" -> "Wallet Transfer"
            else -> "Others"
        }
    }

    fun onAmountChange(v: String) { handle["amount"] = v }
    fun onCategoryChange(v: String) { handle["category"] = v }
    fun onPaymentChange(v: String) { handle["paymentMode"] = v }
    fun onDateChange(v: Long) { handle["date"] = v }
    fun onNoteChange(v: String) { handle["note"] = v }
    fun onTagToggle(v: String) { handle["tag"] = if (tag.value == v) "" else v }

    fun onKey(k: String) {
        val cur = amount.value
        handle["amount"] = when {
            k == "DEL" -> if (cur.isNotEmpty()) cur.dropLast(1) else cur
            k == "." -> if (cur.contains(".")) cur else (if (cur.isEmpty()) "0." else "$cur.")
            cur == "0" -> k
            else -> cur + k
        }
    }

    fun seedFrom(t: com.example.domain.model.Transaction?, accountName: String) {
        if (t == null) return
        handle["type"] = t.type.name
        handle["amount"] = if (t.amount % 1.0 == 0.0) t.amount.toLong().toString() else t.amount.toString()
        handle["category"] = t.category
        handle["paymentMode"] = accountName
        handle["date"] = t.date
        handle["note"] = t.note
        handle["tag"] = t.tag
    }

    fun reset() {
        handle["type"] = "EXPENSE"
        handle["amount"] = ""
        handle["category"] = "Others"
        handle["paymentMode"] = "Cash"
        handle["date"] = System.currentTimeMillis()
        handle["note"] = ""
        handle["tag"] = ""
    }
}
