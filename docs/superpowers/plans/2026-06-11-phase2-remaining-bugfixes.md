# Phase 2 — Remaining Bug Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Close the P0/P1 bugs not handled by the foundation — rotation/process-death data loss on Add, surfaced save/validation errors, and small correctness/quality bugs — without redesigning UI yet.

**Architecture:** Extract a `@HiltViewModel AddTransactionViewModel` with `SavedStateHandle`-backed form state so the in-progress transaction survives config change/process death. Consume the existing `FinanceViewModel.errors` flow as a Snackbar. Fix day-grouping, deprecated APIs, and DonutChart density.

**Tech Stack:** As Phase 1 (Hilt, Compose, coroutines/test, Robolectric).

**Source spec:** `docs/superpowers/specs/2026-06-11-...-design.md` (A1#3, A1#4/#5, A2#13/#14/#15). **Builds on:** `docs/superpowers/plans/2026-06-11-phase0-1-foundation.md`.

**Current anchors (verified):** `FinanceViewModel` exposes `errors: SharedFlow<String>`, `saveTransaction(type,amount,category,paymentMode,date,note,tag)`, `editingTransaction: StateFlow<ui.model.Transaction?>`. `SaveTransactionUseCase.validate(amount,type,accountId,toAccountId): ValidationResult(isValid,error)`. Add screen = `ui/screens/AddTransactionSheet.kt` (`AddTransactionScreen`).

---

### Task 1: Surface errors as a Snackbar (fixes A1#5 — currently `errors` is emitted but never collected)

**Files:**
- Modify: `app/src/main/java/com/example/MainActivity.kt`

- [ ] **Step 1: Add a SnackbarHost wired to `viewModel.errors`**

In `MainAppContent`, add a `SnackbarHostState`, collect errors, pass host to `Scaffold`:

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
LaunchedEffect(Unit) {
    viewModel.errors.collect { snackbarHostState.showSnackbar(it) }
}
// in Scaffold(...):
snackbarHost = { SnackbarHost(snackbarHostState) },
```

- [ ] **Step 2: Build + manual check**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Manually: trigger a failing save (amount 0 after Task 3) → snackbar shows "Enter an amount greater than 0".

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/MainActivity.kt && git commit -m "fix: collect FinanceViewModel.errors into a snackbar"
```

---

### Task 2: AddTransactionViewModel with SavedStateHandle (fixes A1#3 rotation/process-death loss)

**Files:**
- Create: `app/src/main/java/com/example/ui/screens/addtransaction/AddTransactionViewModel.kt`
- Test: `app/src/test/java/com/example/ui/AddTransactionViewModelTest.kt`

- [ ] **Step 1: Failing test — state persists through SavedStateHandle**

```kotlin
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
```

- [ ] **Step 2: Run, verify fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.ui.AddTransactionViewModelTest"`
Expected: FAIL (unresolved).

- [ ] **Step 3: Implement the ViewModel** — all Add-form fields persisted in `SavedStateHandle.getStateFlow(...)`; encapsulates amount-keypad rules (single dot, leading-zero replace, DEL) previously inline in `AddTransactionSheet.kt:480-494`.

```kotlin
package com.example.ui.screens.addtransaction
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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

    fun onTypeChange(v: String) { handle["type"] = v
        handle["category"] = when (v) { "INCOME" -> "Salary"; "TRANSFER" -> "Wallet Transfer"; else -> "Others" } }
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
    fun seedFrom(t: com.example.ui.model.Transaction?) {
        if (t == null) return
        handle["type"] = t.type
        handle["amount"] = if (t.amount % 1.0 == 0.0) t.amount.toLong().toString() else t.amount.toString()
        handle["category"] = t.category; handle["paymentMode"] = t.paymentMode
        handle["date"] = t.date; handle["note"] = t.note; handle["tag"] = t.tag
    }
    fun reset() { handle["type"]="EXPENSE"; handle["amount"]=""; handle["category"]="Others"
        handle["paymentMode"]="Cash"; handle["date"]=System.currentTimeMillis(); handle["note"]=""; handle["tag"]="" }
}
```

- [ ] **Step 4: Run, verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.ui.AddTransactionViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/ui/screens/addtransaction app/src/test/java/com/example/ui/AddTransactionViewModelTest.kt && git commit -m "feat: AddTransactionViewModel with SavedStateHandle (fixes rotation loss)"
```

---

### Task 3: Wire AddTransactionScreen to the VM + disable Save when invalid (fixes A1#3/#4)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/AddTransactionSheet.kt`

- [ ] **Step 1: Replace `remember { mutableStateOf(...) }` form state with the VM**

```kotlin
val addVm: AddTransactionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
val type by addVm.type.collectAsState()
val amountString by addVm.amount.collectAsState()
val selectedCategoryName by addVm.category.collectAsState()
val selectedPaymentMode by addVm.paymentMode.collectAsState()
val dateMillis by addVm.dateMillis.collectAsState()
val note by addVm.note.collectAsState()
val tag by addVm.tag.collectAsState()
LaunchedEffect(editingTransaction) { if (editingTransaction != null) addVm.seedFrom(editingTransaction) }
```

Route every mutation through `addVm.onXChange(...)`/`addVm.onKey(...)`. Remove the local `var`s and the inline keypad mutation logic.

- [ ] **Step 2: Compute validity + disable Save**

```kotlin
val amt = amountString.toDoubleOrNull() ?: 0.0
val isValid = com.example.domain.usecase.SaveTransactionUseCase
    .validate(amt, com.example.domain.model.TxnType.valueOf(type), 1L, null).isValid
```

Apply to both Save affordances (keypad plate + collapsed button): `clickable(enabled = isValid)` / `Button(enabled = isValid)`, and reduce alpha when `!isValid`. On click call the existing `viewModel.saveTransaction(...)` then `addVm.reset()`.

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Manual check** — type amount, rotate device → amount/note preserved; Save greyed at 0; valid save closes sheet and resets.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/ui/screens/AddTransactionSheet.kt && git commit -m "fix: Add screen uses saveable VM; Save disabled when invalid"
```

---

### Task 4: Day-grouping labels respect viewed month (fixes A2#13)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/TransactionsTab.kt` (the `groupedTransactions` block)
- Test: `app/src/test/java/com/example/ui/DayGroupingTest.kt`

- [ ] **Step 1: Extract pure grouping fn + failing test**

Create `app/src/main/java/com/example/ui/util/DayGrouping.kt`:

```kotlin
package com.example.ui.util
import java.util.Calendar
object DayGrouping {
    // Returns "Today"/"Yesterday" only when the txn date is actually today/yesterday relative to `now`.
    fun label(date: Long, now: Long = System.currentTimeMillis()): String {
        val d = Calendar.getInstance().apply { timeInMillis = date }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val yest = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DATE, -1) }
        fun sameDay(a: Calendar, b: Calendar) =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
        return when { sameDay(d, today) -> "Today"; sameDay(d, yest) -> "Yesterday"
            else -> DateTimeUtils.formatDate(date) }
    }
}
```

Test:

```kotlin
package com.example.ui
import com.example.ui.util.DayGrouping
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
class DayGroupingTest {
    private fun day(y:Int,m:Int,d:Int): Long = Calendar.getInstance().apply {
        clear(); set(y, m-1, d, 12, 0) }.timeInMillis
    @Test fun pastMonthDate_isNotLabelledToday() {
        val now = day(2026,6,11)
        assertEquals(false, DayGrouping.label(day(2026,1,11), now) == "Today")
    }
    @Test fun todayLabel() {
        val now = day(2026,6,11)
        assertEquals("Today", DayGrouping.label(day(2026,6,11), now))
    }
}
```

- [ ] **Step 2: Run, fail; implement (above); run, pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.ui.DayGroupingTest"`
Expected: FAIL → PASS after creating `DayGrouping.kt`.

- [ ] **Step 3: Use it in `TransactionsTab.kt`** — replace the inline `groupKey` computation (`TransactionsTab.kt:83-91`) with `DayGrouping.label(txn.date)`.

- [ ] **Step 4: Build + commit**

```bash
./gradlew :app:assembleDebug && git add app/src/main/java/com/example/ui/util/DayGrouping.kt app/src/main/java/com/example/ui/screens/TransactionsTab.kt app/src/test/java/com/example/ui/DayGroupingTest.kt && git commit -m "fix: day-grouping labels respect viewed month (A2#13)"
```

---

### Task 5: DonutChart strokes in dp + deprecated `capitalize` (fixes A2#15, A2#14)

**Files:**
- Modify: `app/src/main/java/com/example/ui/components/DonutChart.kt`
- Modify: `app/src/main/java/com/example/ui/screens/AddTransactionSheet.kt`

- [ ] **Step 1: dp strokes** — in `DonutChart`, convert width via density:

```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    val stroke = 14.dp.toPx()
    // empty ring: Stroke(width = stroke, ...); slices: Stroke(width = stroke, ...)
}
```

Replace the hardcoded `24f`/`36f`.

- [ ] **Step 2: replace `opt.capitalize(Locale.ROOT)`** (`AddTransactionSheet.kt:175`) with `opt.lowercase().replaceFirstChar { it.uppercase() }`.

- [ ] **Step 3: Build + commit**

```bash
./gradlew :app:assembleDebug && git add app/src/main/java/com/example/ui/components/DonutChart.kt app/src/main/java/com/example/ui/screens/AddTransactionSheet.kt && git commit -m "fix: donut strokes in dp; replace deprecated capitalize"
```

---

### Task 6: Phase wrap — full regression

- [ ] **Step 1:** Run `./gradlew :app:testDebugUnitTest :app:assembleDebug` → all green.
- [ ] **Step 2:** Manual smoke: rotate mid-add (no loss), invalid save blocked + snackbar, past-month list has no "Today".
- [ ] **Step 3:** `git add -A && git commit -m "chore: phase 2 complete" && git tag phase-2-done`

---

## Self-Review

**Spec coverage:** A1#3 rotation → Task 2/3 ✅; A1#4 disable save → Task 3 ✅; A1#5 error surface → Task 1 ✅; A2#13 → Task 4 ✅; A2#14 → Task 5 ✅; A2#15 → Task 5 ✅. A1#17 (insets/bottom-sheet) intentionally deferred to Phase 3 (lands with navigation-compose).
**Placeholder scan:** none.
**Type consistency:** `SaveTransactionUseCase.validate(Double,TxnType,Long,Long?)` matches Phase 1 impl; `AddTransactionViewModel` method names used identically in test and screen wiring; `ui.model.Transaction` fields match Phase 1 DTO.
