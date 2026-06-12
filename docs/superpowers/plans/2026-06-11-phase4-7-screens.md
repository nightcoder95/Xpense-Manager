# Phases 4–7 — Screens (Pixel-Match) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Rebuild every screen to pixel-match the reference screenshots, on top of the Phase 0–3 foundation + design system + navigation.

**Architecture:** Each screen = stateless `XScreen.kt` + `@HiltViewModel XViewModel.kt` (UseCases only) + `XUiState.kt`. All visuals come from `core/designsystem`. **Every screen ships with a Roborazzi golden recorded against its reference screenshot — that golden is the acceptance gate.** Each screen's ViewModel pulls range-scoped data via UseCases (no in-composable filtering).

**Tech Stack:** Compose, Hilt, navigation-compose, Roborazzi, Coil (images), DataStore (settings).

**Source spec:** `docs/superpowers/specs/2026-06-11-...-design.md` Part D (D2–D13), Part E (edge cases), Part F (tests). **Builds on:** Phases 0–3.

## How to read this plan

UI pixel work is iterative and visual; authoring every Compose line blind would drift. So each screen is one **task block** with: files, the reference screenshot, the exact section layout (from Part D), the design-system components to use, the ViewModel/UseCase wiring (concrete signatures), the edge cases to cover (from Part E), and the **gate**. Within a task, the loop is:

1. Write `XUiState` + `XViewModel` (TDD: unit-test the VM's state mapping with fake repos/usecases).
2. Build `XScreen` from design-system components per the layout spec.
3. Record Roborazzi golden; visually diff against the named `assets/` screenshot; tune until it matches.
4. Wire into the NavHost route (Phase 3 Task 5).
5. Commit.

**Per-screen Definition of Done:** VM unit-tested; screen built only from `core/designsystem` (zero hardcoded `Color(0xFF…)`); golden recorded and visually matches the reference; route wired; lists use `key=`; no main-thread work (StrictMode clean).

---

# PHASE 4 — Core screens

### Task 4.1: Home (`assets/Screenshot_2026_0611_111908.jpg`, spec D2)

**Files:** `ui/feature/home/HomeScreen.kt`, `HomeViewModel.kt`, `HomeUiState.kt`; Test `test/.../home/HomeViewModelTest.kt`.

**Layout (top→bottom):** greeting header ("Good Morning, **Guest User**" + search/premium/avatar icons) · **Cash Flow card** (`SummaryCard` + "This Month ▾") · **Quick Setup Guide** (3 radio rows + "Completed (N) ▸") · **Recent Transactions** (`SectionHeader` + "See all" → `Routes.DAY`; 3× `TransactionRow`) · **Budgets** card (`XPillToggle` Monthly/Annual; empty "No Budget Yet?" + Set Budget, or progress) · **Scheduled** card (upsell, non-functional) · **Promo** gold banner (Invite/No thanks) · **Discover** Coil image carousel + "See more" · "Customize" button.

**VM wiring:**
```kotlin
data class HomeUiState(
  val greeting: String, val summary: MonthlySummary, val budget: Budget?,
  val recent: List<Transaction>, val setupDone: Int)
```
Compose from `txnRepo.inRange(MonthRange.bounds(year,month))`, `MonthlySummaryUseCase`, `budgetRepo`, `catRepo`. Greeting from time-of-day. `recent = list.sortedByDescending{date}.take(3)`.

**Edge cases (E):** empty month → zero summary + empty Recent state (E#2); budget unset vs set (E#12).

- [ ] VM + test → build screen → record golden vs screenshot → wire `Routes.HOME` → commit.

### Task 4.2: Add Transaction (`…111921`,`…111927`,`…111931`,`…111938`, spec D3/D4)

**Files:** `ui/feature/addtransaction/AddTransactionScreen.kt` (rebuild), reuse `AddTransactionViewModel` (Phase 2). New `CategoryPickerSheet.kt`, `AccountPickerSheet.kt` in `core/designsystem`.

**Layout:** top bar (back + "Add transaction") · `XPillToggle` Expense/Income/Transfer · date+time row · **Amount** row (₹ + value + calculator icon; numeric entry) · Category row → `CategoryPickerSheet` (grid/list toggle, edit pencil → `Routes.CATEGORIES`, close) · Payment-mode row → `AccountPickerSheet` · "Write a note" · "Add tags" + (?) help + chips · "Add attachment ▸" (Coil/SAF picker, premium-lite) · **white circular floppy Save FAB**.

**Replaces:** the bespoke keypad → system keyboard / numeric field; inline tag UI → component. Save uses `viewModel.saveTransaction(...)` + `addVm.reset()`; disabled when invalid (Phase 2 Task 3 logic).

**Edge cases (E):** amount parsing (E#3); transfer same from/to blocked (E#8, via `validate`); decimal display (E#5).

- [ ] Pickers + test → screen → goldens (expense+income+picker) → wire `Routes.ADD` → commit.

### Task 4.3: Analysis (`…112042`, spec D7)

**Files:** `ui/feature/analysis/AnalysisScreen.kt`, `AnalysisViewModel.kt`, `AnalysisUiState.kt`; VM test.

**Layout:** `XPillToggle` Week/Month/Year/Custom · `DateRangeHeader` (+"N TRANSACTIONS") · `SummaryCard` · Budget card · Trends card (upsell) · **Categories** donut card (`XPillToggle` Spending/Income + `DonutChart` + top list) · **Payment modes** card (`XPillToggle` Spending/Income/Transfers + list) · Stats (Avg Spending per day [real day count] / per txn; Avg Income).

**VM wiring:** `CategoryBreakdownUseCase`, `GetPaymentModeBreakdownUseCase` (new — group by accountId, like CategoryBreakdown), `SpendingStatsUseCase`, `MonthlySummaryUseCase`. Period selector recomputes range via `MonthRange`/week/year bounds (add `RangeBounds` helpers to `core/datetime`).

**Edge cases (E):** empty month donut empty-ring (E#2); per-day uses real days (E#6).

- [ ] add `PaymentModeBreakdownUseCase` (+test) → VM+test → screen → golden → wire `Routes.ANALYSIS` → commit.

### Task 4.4: Accounts (INFERRED — match visual language, spec D13)

**Files:** `ui/feature/accounts/AccountsScreen.kt`, `AccountsViewModel.kt`, `AccountsUiState.kt`; VM test.

**Layout:** header + total-net `SummaryCard` · list of account cards (rounded-square icon tile, name, type, `AmountText` balance via `AccountBalancesUseCase`) · "+ Add account" → account editor sheet. Tap account → its transactions (reuse Day/Custom list, filtered by accountId).

**VM wiring:** `accountRepo.all()` + `txnRepo.all()` → `AccountBalancesUseCase`.

**Edge cases (E):** delete account with transactions → block or reassign (E#11).

- [ ] VM+test (balances incl. transfers) → screen → golden (mark INFERRED) → wire `Routes.ACCOUNTS` → commit.

### Task 4.5: More (`…112102`, spec D8)

**Files:** `ui/feature/more/MoreScreen.kt` (rebuild), `MoreViewModel.kt`.

**Layout:** profile card (Guest User / Sign in [no-op] + Backup now [no-op]) · 2-col action grid (Transactions→Custom, Scheduled Txns, Budgets, Categories, Tags, Go Premium) · Views row (Day/Calendar/Custom) · Unlock Premium banner · options list (Settings, Invite, Rate, Query, FAQs, About). All entries `navController.navigate(...)`; placeholders no-op with a toast.

- [ ] screen → golden → wire `Routes.MORE` → commit.

---

# PHASE 5 — Secondary screens

### Task 5.1: Categories (`…112005`, spec D5)
**Files:** `ui/feature/categories/CategoriesScreen.kt`, `CategoriesViewModel.kt`.
**Layout:** top bar "Categories" + overflow · `XPillToggle` Expense/Income · "Default Category ▸" row (→ `SetDefaultCategoryUseCase`) · "Edit order" (drag-reorder → `ReorderCategoriesUseCase`) · 2-col `CategoryTile` grid · + FAB → `Routes.EDIT_CATEGORY`.
**New UseCases:** `SetDefaultCategoryUseCase`, `ReorderCategoriesUseCase` (write `sortOrder`/`isDefault`) + tests.
- [ ] usecases+tests → VM+test → screen → golden → wire → commit.

### Task 5.2: Edit Category (`…112027`, spec D6)
**Files:** `ui/feature/editcategory/EditCategoryScreen.kt`, `EditCategoryViewModel.kt`; expand `ui/util/CategoryIconHelper.kt`.
**Layout:** top bar (back + delete) · big preview icon · name field · color palette grid · **grouped icon picker** (Food/Travel/Shopping/Family/Entertainment/Business/Finance/Medical/Utilities/Miscellaneous) · save FAB.
**Work:** extend `CategoryIconHelper` with grouped icon metadata (`Map<Group, List<icon>>`) covering the screenshot's icon set. Create vs edit via `name` arg.
- [ ] expand icon set → VM+test (create/update) → screen → golden → wire `Routes.EDIT_CATEGORY` → commit.

### Task 5.3: Day / Calendar / Custom views (`…112109`,`…112113`,`…112120`,`…112125`, spec D9–D11)
**Files:** `ui/feature/views/{DayViewScreen,CalendarViewScreen,CustomViewScreen}.kt` + one `ViewsViewModel.kt`; `core/designsystem/FilterSheet.kt`.
**Day:** date `DateRangeHeader` + `SummaryCard` + day list (`GetDayViewUseCase`). **Calendar:** 7-col M–S month grid, per-day colored amount chips (`GetCalendarMonthUseCase`), tap→Day. **Custom:** search + active-filter chips + `SummaryCard` + list + filter FAB → **`FilterSheet`** (Year/month vs Date range; Category All/Spending/Income/Transfer + chips + Select all; Payment chips). Uses `SearchTransactionsUseCase` (Phase 1) — **fixes A2#12** (real picker, not hardcoded).
**New UseCases:** `GetDayViewUseCase`, `GetCalendarMonthUseCase` (+tests).
**Edge cases:** filter→empty list state (E#13); today label across months (already Phase 2).
- [ ] usecases+tests → FilterSheet → 3 screens → 4 goldens → wire `Routes.DAY/CALENDAR/CUSTOM` → commit.

### Task 5.4: Budgets (INFERRED, spec D13)
**Files:** `ui/feature/budgets/BudgetsScreen.kt`, `BudgetsViewModel.kt`.
**Layout:** `XPillToggle` Monthly/Annual · current-period budget + progress bars (overall + per-category) · set/edit dialog → `budgetRepo.upsert` (annual key = "yyyy"). Real, free feature (no gating).
- [ ] VM+test → screen → golden (INFERRED) → wire `Routes.BUDGETS` → commit.

### Task 5.5: Settings (`…112145`, spec D12) — DataStore
**Files:** `ui/feature/settings/SettingsScreen.kt`, `SettingsViewModel.kt`; `data/preferences/PreferencesRepository.kt` + DataStore impl; `domain/repository` interface.
**Layout:** App Icon picker (activity-alias swap) · Appearance (Theme/Time/Decimal) · Preferences (Currency&Format→drives `CurrencyFormatter`; Default Payment/Category; First Day of Month; Week Starts On; Home Balance View; Haptics switch) · Notifications (Daily Reminder/Budget Alerts switches → WorkManager job, default off) · Backup/Restore/Export (no-op placeholders) · Security (switches, placeholders) · Support · **Danger Zone** (Delete Data → `txnRepo.deleteAll`; Delete Account no-op).
**Work:** `PreferencesRepository` over DataStore (enabled Phase 0); inject into `CurrencyFormatter` provider so currency/decimal prefs apply app-wide (E#16). WorkManager added here for reminders (single periodic job, constraints, default off — Part C4).
- [ ] PreferencesRepository+test → VM+test → screen → golden → wire `Routes.SETTINGS` → commit.

---

# PHASE 6 — Premium placeholders (pixel-match, non-functional)

### Task 6.1: Scheduled Txns
`ui/feature/scheduled/ScheduledScreen.kt` — list of recurring rules over `ScheduledEntity` (real, free for personal use): add/edit/toggle; `ScheduledViewModel` + `ScheduledRepository` (+DAO query, migration table already exists). Optional WorkManager trigger. Golden (INFERRED).

### Task 6.2: Tags
`ui/feature/tags/TagsScreen.kt` — list distinct tags from transactions; tap → Custom view filtered by tag. `TagsViewModel` (derive from `txnRepo.all()`). Golden (INFERRED).

### Task 6.3: Go Premium / Unlock Premium
`ui/feature/premium/GoPremiumScreen.kt` — feature list + CTA, non-functional (no billing). Golden (INFERRED).

### Task 6.4: Upsell surfaces + Discover + app-icon
Wire the Home Scheduled/Promo/Discover cards (Task 4.1) and Analysis Trends card to static content/Coil images; implement app-icon picker via `<activity-alias>` entries in the manifest + enable/disable components from Settings (Task 5.5). Goldens already covered by Home/Analysis.

- [ ] each: screen → golden → wire route → commit.

---

# PHASE 7 — Accessibility & polish

### Task 7.1: Accessibility pass
Content descriptions on all icons/clickables; min 48dp touch targets; `testTag`s for UI tests; verify TalkBack order on Home/Add/Analysis.

### Task 7.2: RTL + font scale (E#15)
Test every screen at `layoutDirection = Rtl` and `fontScale = 1.5`; fix clipped/overlapping layouts. Add a parameterized Roborazzi pass at large font scale for Home/Add/Transactions.

### Task 7.3: Empty/error states + haptics
Confirm every list/screen has an empty state (E#2/#13) and surfaces errors (snackbar). Apply haptics per Settings toggle.

### Task 7.4: Final perf + screenshot-diff gate
Run Macrobenchmark (startup + scroll); confirm StrictMode clean; run **all** Roborazzi goldens in verify mode (`-Proborazzi.test.verify=true`) — full visual regression vs every reference screenshot. APK-size check.

- [ ] each task: implement → test → commit. Phase tag `phase-7-done`.

---

## Self-Review

**Spec coverage:**
- Part D D2–D13 → Tasks 4.1–4.5, 5.1–5.5, 6.1–6.4 (every screen mapped, screenshot named or INFERRED flagged). ✅
- A2#12 broken filter chips → Task 5.3 FilterSheet + `SearchTransactionsUseCase`. ✅
- Part E edge cases referenced inline per screen (E#2,3,5,6,8,11,12,13,15,16). ✅ (E#1/#9/#10/#17 closed in earlier phases; E#18 = Phase 1 migration.)
- Part F tests: per-screen VM unit tests + Roborazzi goldens + Macrobenchmark Task 7.4. ✅
- Part C4 perf: stateless components, `key=`, range queries, WorkManager-not-polling, StrictMode/Macrobench gate. ✅

**New UseCases introduced (each gets a TDD unit test in its task):** `PaymentModeBreakdownUseCase` (4.3), `SetDefaultCategoryUseCase`/`ReorderCategoriesUseCase` (5.1), `GetDayViewUseCase`/`GetCalendarMonthUseCase` (5.3). Range helpers added to `core/datetime` (4.3).

**Placeholder scan:** Per-screen Compose authored during execution against named screenshots with a recorded golden gate — this is the stated, intentional method for pixel work, not an unfilled TODO. Non-UI logic (UseCases, repos, prefs) is fully specified with signatures.

**Type consistency:** screen VMs consume Phase-0/1 signatures (`txnRepo.inRange(start,end)`, `MonthRange.bounds`, `*UseCase` classes, `AccountBalancesUseCase.from(accounts,txns)`, `SaveTransactionUseCase.validate`); design-system component names match Phase 3 (`XSurfaceCard`, `XPillToggle`, `SummaryCard`, `TransactionRow`, `DateRangeHeader`, `BottomNavBar`, `CategoryPickerSheet`, `FilterSheet`); routes match Phase 3 `Routes`.

**Dependency note:** Phase 5.5 adds WorkManager + DataStore prefs wiring; Phase 6.4 adds `<activity-alias>` icon variants. Both flagged in their tasks.
