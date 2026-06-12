package com.example.ui

import androidx.lifecycle.SavedStateHandle
import com.example.ui.screens.addtransaction.AddTransactionViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class AddTransactionViewModelTest {
    @Test fun amount_survives_recreation_via_savedState() {
        val handle = SavedStateHandle()
        AddTransactionViewModel(handle).apply { onAmountChange("1234"); onNoteChange("lunch") }
        // simulate process death: new VM from same handle
        val restored = AddTransactionViewModel(handle)
        assertEquals("1234", restored.amount.value)
        assertEquals("lunch", restored.note.value)
    }

    @Test fun amountInput_ignoresMultipleDots_andLeadingZero() {
        val vm = AddTransactionViewModel(SavedStateHandle())
        vm.onKey("0"); vm.onKey("5")   // "0" then "5" -> "5"
        assertEquals("5", vm.amount.value)
        vm.onKey("."); vm.onKey(".")   // single dot
        assertEquals("5.", vm.amount.value)
    }
}
