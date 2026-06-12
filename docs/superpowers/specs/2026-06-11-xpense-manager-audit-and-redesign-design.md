# Xpense-Manager — Architecture Audit & Pixel-Match Redesign

**Date:** 2026-06-11
**Status:** Design / spec (no code written yet)
**Author:** Audit pass per `/mp-improve-codebase-architecture` + `/ui-ux-pro-max` + `/frontend-ui-engineering`

## Scope decisions (confirmed with user)

1. **Screen scope:** Everything in the reference screenshots **plus** premium features as non-functional UI placeholders/upsells.
2. **Architecture depth:** Layered + DI (Hilt, UseCases, repositories per aggregate, new `Account`/`Scheduled` entities).
3. **Missing-screenshot screens** (Accounts content, Budgets, Scheduled, Tags, Go Premium): designed to match the reference visual language, flagged `INFERRED` for user review.
4. **UI fidelity:** Pixel-match exact. Current "Obsidian/Indigo" custom theme is replaced by the reference design language where they differ.
5. **Sequencing:** Architecture/bug stabilization first, then UI rework on the clean foundation.
6. **Currency:** INR default.
7. **No billing / no auth — personal use.** Premium banners ("Upgrade Now", "Go Premium", "50% off", "Sign in", "Backup now") are rendered for pixel-match but are **non-functional placeholders**: no IAP, no account system, no remote backup. Tapping is a no-op or local-only.
8. **INFERRED screens approved** to follow the reference UI language (Accounts, Budgets, Scheduled, Tags, Go Premium).
9. **Non-functional requirements (hard):** app must be **light, battery-friendly, and snappy**. See Part C4 — these are first-class constraints, not polish.

---

# PART A — Architecture & Code Audit (findings)

Severity: **P0** = data loss / crash / correctness bug; **P1** = architecture/testability blocker; **P2** = DRY/KISS/quality; **P3** = polish.

## A1. Correctness / race / data-loss (P0)

| # | Issue | Location | Detail | Fix |
|---|---|---|---|---|
| 1 | **Double category seed race** | `AppDatabase.kt:41` + `FinanceViewModel.kt:60` | `RoomDatabase.Callback.onCreate` seeds 19 categories AND VM `init` calls `checkAndSeedCategoriesIfNeeded` seeding the same 19. Both run on first launch; order nondeterministic; duplicate writes. | Single seed path. Seed only via Room `onCreate` callback OR a `DatabaseInitializer` UseCase — not both. Remove the VM-side seed. |
| 2 | **`INSTANCE?.let` null in `onCreate`** | `AppDatabase.kt:43` | `INSTANCE` is assigned *after* `build()` returns, but `onCreate` can fire during `build()`. `INSTANCE?.let { }` may no-op → categories never seeded on a cold first run. | Seed using the `db`/`SupportSQLiteDatabase` passed to `onCreate`, or use a `Provider<TransactionDao>` injected into the callback. Do not reference `INSTANCE` inside the callback. |
| 3 | **Rotation / process-death data loss** | `AddTransactionSheet.kt:45-51` | `amountString`, `note`, `category`, `paymentMode`, `dateMillis`, `tag`, `type` use `remember`, not `rememberSaveable`. Config change wipes a half-entered transaction. | Hoist form state into a `AddTransactionViewModel` with `SavedStateHandle`, or `rememberSaveable`. ViewModel-backed (see Part B). |
| 4 | **Silent save no-op** | `AddTransactionSheet.kt:526`, `562` | `if (amt > 0.0) save()` — when amount is 0/blank the Save button does nothing, no feedback. | Disable Save when invalid; show inline error ("Enter an amount"). Validation in UseCase returns typed result. |
| 5 | **Swallowed DB exceptions** | `FinanceViewModel.kt:62-69`, all `viewModelScope.launch` writes | Seed wrapped in `try/catch {}` that ignores everything; all writes are fire-and-forget with no error surface. A failed insert is invisible. | UseCases return `Result<T>`; VM exposes a one-shot `UiEvent`/snackbar flow for failures. |
| 6 | **Bulk delete via N single deletes over stale snapshot** | `FinanceViewModel.kt:76-79`, `250-253` | `clearAllData`/`seedDemoData` loop `repository.deleteTransaction(t.id)` over `transactions.value` (a stale `StateFlow` snapshot, possibly mid-emit). N queries; races with live inserts. | Add `@Query("DELETE FROM transactions")` bulk DAO op (and per-table). One transaction. |
| 7 | **TRANSFER breaks balances & totals** | `HomeTab.kt:48-62`, `75-77`; `AddTransactionSheet.kt:79-87` | Home balance math ignores `TRANSFER` entirely; income/expense totals exclude it inconsistently. Transfer categories are synthesized inline (`Wallet Transfer`…) and never persisted, so editing a transfer can't resolve its category. A transfer should move value between two payment modes but only stores one. | Model transfer as `fromAccount`/`toAccount` (see Account entity, Part B). Until then: persist transfer categories; define transfer's effect on each account balance explicitly and test it. |
| 8 | **Hardcoded fake balances** | `HomeTab.kt:49`, `55` | Bank seeded at `100000`, cash at `15000` literally in the composable. "Accounts & Liquidity" numbers are fiction. | Real `Account` entity with opening balance; balance = opening + Σ(income) − Σ(expense) ± transfers, computed in a UseCase. |

## A2. Edge cases / smaller correctness (P0–P1)

| # | Issue | Location | Fix |
|---|---|---|---|
| 9 | `avgSpendPerDay = total / 30.0` hardcoded 30 | `AnalysisTab.kt:88` | Divide by actual days in the selected month (`YearMonth.lengthOfMonth()`), or days-elapsed for current month. |
| 10 | Locale mixing in number formatting (`Locale.US` vs `Locale.getDefault()`) | throughout (`HomeTab.kt:188`, `:556`, etc.) | One `CurrencyFormatter` utility; single locale policy; configurable currency symbol. |
| 11 | Currency `₹` hardcoded everywhere | all screens | `CurrencyFormatter` + user-selectable currency (Settings → Currency & Format). |
| 12 | Broken filter chips hardcode `"Food and Dining"`/`"Cash"` | `TransactionsTab.kt:163`, `195` | Real multi-select filter bottom sheet (matches Custom-View Filter screenshot). |
| 13 | "Today/Yesterday" grouping compares to `now`, independent of viewed month | `TransactionsTab.kt:78-91` | Acceptable, but document; ensure consistent when month-nav ≠ current month (no "Today" label in a past month). |
| 14 | `String.capitalize` deprecated | `AddTransactionSheet.kt:175` | `replaceFirstChar { it.uppercase() }`. |
| 15 | DonutChart strokes use raw px (`24f`,`36f`), not dp | `DonutChart.kt:46`,`58` | Convert dp→px via density; otherwise inconsistent across screen densities. |
| 16 | No empty/zero guards on averages beyond count | `AnalysisTab.kt:87-91` | Covered by tests; keep guards. |
| 17 | `enableEdgeToEdge` + manual `windowInsetsPadding` on content while bottom sheet is a hand-rolled `AnimatedVisibility` overlay | `MainActivity.kt:29`,`150-163` | Use `ModalBottomSheet`/proper nav destination; consolidate insets handling. |

## A3. Architecture / testability (P1)

- **All business logic lives in composables.** Month filtering (`Calendar.getInstance()` per transaction) duplicated across `HomeTab.kt:66`, `AnalysisTab.kt:51`, `TransactionsTab.kt:49`. Balances, aggregates, budget ratio, category breakdown, stats — all in UI. **Nothing is unit-testable.**
- **God ViewModel** (`FinanceViewModel`) mixes: data flows, UI flags (`isAddSheetOpen`, `isCategoryScreenOpen`), navigation (`selectedTab`), filters, month cursor, and all DB writes. 256 lines, no separation.
- **No DI.** VM constructs `AppDatabase` + `FinanceRepository` directly (`FinanceViewModel.kt:15-16`). Untestable; no fake repo injection.
- **Theme not single source of truth.** `AddTransactionSheet.kt` ignores theme tokens entirely — ~40 hardcoded `Color(0xFF…)`. `MoreTab`, `AnalysisTab` partially too.
- **Performance:** per-recomposition `Calendar` allocation per transaction in filters; should be precomputed in domain layer keyed by `YearMonth`.

## A4. DRY / KISS (P2)

| Duplication | Copies | Consolidation |
|---|---|---|
| Transaction row composable | `HomeTab.kt:558-655`, `TransactionsTab.kt:327-425` | `TransactionRow` shared component |
| Month-selector pill | `HomeTab.kt:112-153`, `AnalysisTab.kt:123-150` | `MonthSelector` component |
| Color-hex parse `Color(android.graphics.Color.parseColor(..))` | ~10 sites | `String.toComposeColor()` ext / store color as `Int` |
| Category seed list (19 entries) | `AppDatabase.kt:51-74`, `FinanceRepository.kt:57-80` | One `DefaultCategories` source |
| Card container (bg+border+radius) | every screen | `SurfaceCard` composable wrapper |
| Empty-state block | `HomeTab.kt:503-540`, `TransactionsTab.kt:248-285` | `EmptyState` component |
| Picker AlertDialogs | `AddTransactionSheet`, `MoreTab`, `AnalysisTab` | shared bottom-sheet pickers |

## A5. Tests / tooling (P2)

- No real tests. `ExampleUnitTest`, `ExampleInstrumentedTest`, `ExampleRobolectricTest`, `GreetingScreenshotTest` are scaffolding.
- Roborazzi + Robolectric already wired → screenshot tests are cheap to add.
- No Room migration strategy; `version = 1`, `exportSchema = false`. Adding entities needs version bump + schema export for migration tests.

---

# PART B — Target Architecture

## B1. Layering (package-by-layer within `:app`; modularize later if needed)

```
com.example
├── XpenseApp.kt                 // @HiltAndroidApp
├── MainActivity.kt              // @AndroidEntryPoint, hosts NavHost
├── core/
│   ├── designsystem/            // tokens, theme, reusable components
│   ├── common/                  // CurrencyFormatter, Result, dispatchers
│   └── datetime/                // YearMonth helpers (java.time)
├── data/
│   ├── local/                   // Room: AppDatabase, DAOs, entities, migrations
│   ├── preferences/             // DataStore (settings)
│   └── repository/              // impls: TransactionRepo, CategoryRepo, BudgetRepo, AccountRepo, PreferencesRepo
├── domain/
│   ├── model/                   // pure Kotlin models (no Room annotations)
│   ├── repository/              // repository interfaces
│   └── usecase/                 // one class per operation
├── di/                          // Hilt modules
└── ui/
    ├── navigation/              // routes, NavHost, bottom bar
    └── feature/
        ├── home/  analysis/  accounts/  more/
        ├── addtransaction/  categories/  editcategory/
        ├── budgets/  scheduled/  tags/  settings/
        └── views/               // day / calendar / custom
```

Each feature folder: `XScreen.kt` (stateless UI), `XViewModel.kt` (`@HiltViewModel`), `XUiState.kt`.

## B2. Dependencies to add / enable (`libs.versions.toml`)

- **Add Hilt:** `hilt-android`, `hilt-compiler` (KSP), `androidx-hilt-navigation-compose`, Hilt Gradle plugin. (Kotlin 2.2.10 + KSP 2.3.5 already present.)
- **Enable** (currently commented): `androidx-navigation-compose`, `androidx-datastore-preferences`, `coil-compose` (for Discover images / attachments).
- **Room schema export:** set `room { schemaDirectory("$projectDir/schemas") }`, `exportSchema = true` → enables migration tests.
- Test: `room-testing`, `hilt-android-testing`, `turbine` (Flow assertions).

## B3. Data model

**New / changed entities** (DB `version = 2`, with migration):

```kotlin
@Entity("accounts")
data class AccountEntity(
  @PrimaryKey(autoGenerate=true) val id: Long = 0,
  val name: String,            // "Cash", "Bank Account", "Credit Card", custom
  val type: String,            // CASH | BANK | CREDIT_CARD | WALLET
  val openingBalance: Double,
  val iconName: String,
  val colorHex: String,
  val archived: Boolean = false
)

@Entity("transactions")  // changed
data class TransactionEntity(
  @PrimaryKey(autoGenerate=true) val id: Long = 0,
  val type: String,            // EXPENSE | INCOME | TRANSFER
  val amount: Double,
  val category: String,        // null/"" for TRANSFER
  val accountId: Long,         // replaces free-text paymentMode
  val toAccountId: Long? = null, // TRANSFER destination
  val date: Long,
  val note: String,
  val tag: String = ""
)

@Entity("categories")  // + sortOrder, isDefault
data class CategoryEntity(
  @PrimaryKey val name: String, val type: String,
  val iconName: String, val colorHex: String,
  val sortOrder: Int = 0, val isDefault: Boolean = false
)

@Entity("budgets")  // unchanged shape; + optional categoryId for per-category budgets, annual support
data class BudgetEntity(
  @PrimaryKey val key: String, // "yyyy-MM" or "yyyy" (annual)
  val amountLimit: Double, val period: String = "MONTHLY" // MONTHLY | ANNUAL
)

@Entity("scheduled")  // premium placeholder, real schema for future
data class ScheduledEntity(
  @PrimaryKey(autoGenerate=true) val id: Long = 0,
  val type: String, val amount: Double, val category: String,
  val accountId: Long, val recurrence: String, // DAILY|WEEKLY|MONTHLY|YEARLY
  val nextRun: Long, val note: String, val enabled: Boolean = true
)
```

**Migration 1→2:** create `accounts`, seed three defaults (Cash, Bank Account, Credit Card) with opening balances moved out of the hardcoded UI numbers; add `accountId`/`toAccountId` to `transactions` and backfill from old `paymentMode` text → matching account id; add `sortOrder`/`isDefault` to `categories`; create `scheduled`. Provide a `MIGRATION_1_2` + a migration test.

**Domain models** are plain Kotlin (`Transaction`, `Account`, `Category`, `Budget`, `MonthlySummary`, `CategoryBreakdown`, `AccountBalance`) mapped from entities — keeps Room out of UI/domain.

## B4. Repositories (interfaces in `domain`, impls in `data`)

- `TransactionRepository`, `CategoryRepository`, `BudgetRepository`, `AccountRepository`, `PreferencesRepository`.
- Move the **single** default-category list here (`DefaultCategories` object).

## B5. UseCases (the deepening — pull logic out of composables)

Each is a small, injectable, **unit-testable** class:

- `GetMonthlySummaryUseCase(year, month)` → income, spending, net, budget ratio. Replaces inline math in Home/Analysis.
- `GetTransactionsForMonthUseCase` → pre-filtered, uses `java.time.YearMonth` (no per-item `Calendar`).
- `GetCategoryBreakdownUseCase(period, type)` → donut portions + list + percentages.
- `GetPaymentModeBreakdownUseCase(type)` → Analysis "Payment modes" section.
- `GetAccountBalancesUseCase` → real balances incl. transfers.
- `GetSpendingStatsUseCase(month)` → avg/day (real day count), avg/txn, counts.
- `GetDayViewUseCase(date)`, `GetCalendarMonthUseCase(month)` → Day/Calendar views.
- `SaveTransactionUseCase` → validates (amount>0, account exists, transfer has distinct from/to), returns `Result`.
- `DeleteTransactionUseCase`, `SaveBudgetUseCase`, `Create/Update/DeleteCategoryUseCase`, `ReorderCategoriesUseCase`, `SetDefaultCategoryUseCase`.
- `SeedDemoDataUseCase`, `ClearAllDataUseCase` (bulk, transactional).
- `SearchTransactionsUseCase(query, filters)`.

VMs depend on UseCases only.

## B6. Navigation

Replace the `selectedTab` enum + manual `when` in `MainActivity` with `navigation-compose`:
- Bottom-bar tabs: `home`, `analysis`, `accounts`, `more` + center FAB → `addTransaction`.
- Pushed routes: `categories`, `editCategory/{name?}`, `budgets`, `scheduled`, `tags`, `settings`, `dayView`, `calendarView`, `customView`, `goPremium`.
- `addTransaction` as a route (full-screen), not a hand-rolled overlay → fixes insets + back handling.

---

# PART C — Design system (pixel-match foundation)

Derived from screenshots. All screens use **one** dark palette + tokens; no per-screen literals.

## C1. Tokens (replace `Color.kt`)

| Token | Value (from screenshots) | Use |
|---|---|---|
| `background` | `#0B0D10` near-black | screen bg |
| `surface` | `#16191F` | cards |
| `surfaceVariant` | `#1E222A` | inner chips, toggles |
| `outline` | `#262B33` | hairline borders |
| `textPrimary` | `#FFFFFF` | headings/amounts |
| `textSecondary` | `#9AA1AC` | labels |
| `spending` | `#F2706B` coral-red | expense |
| `income` | `#43C59E`/green | income |
| `accentGold` | `#E3B341` | premium badges/upsell |
| `onAccent` | black | text on light FAB |
| Category colors | keep the 19 hex values | category icons (data-driven) |

Add light-tile category icon backgrounds matching screenshots (e.g. soft-tinted rounded squares, not circles — reference uses rounded-square tiles in lists, circles in pickers).

## C2. Reusable components (build once, in `core/designsystem`)

`XSurfaceCard`, `XPrimaryButton`, `XPillToggle` (n-segment, used by Expense/Income/Transfer, Week/Month/Year/Custom, Monthly/Annual, Spending/Income/Transfers), `MonthSelector`, `DateRangeHeader` (with "N TRANSACTIONS" subline), `TransactionRow`, `CategoryTile`, `AmountText` (sign+color+format), `SummaryCard` (SPENDING/INCOME/Net Balance — appears on Home/Analysis/Day/Custom), `EmptyState`, `SectionHeader`, `DonutChart` (fixed dp strokes), `CategoryPickerSheet`, `FilterSheet`, `BottomNavBar` (5-slot w/ center FAB notch).

## C3. Typography

Define real Material3 type scale in `Type.kt` (currently only `bodyLarge`). Screenshots use a heavy display weight for amounts (Black, tight tracking), medium for labels. Single font (system/Default OK; optionally bundle Inter).

## C4. Performance & battery (hard NFR — "light, snappy, no drain")

Treated as acceptance criteria, not nice-to-haves.

**Data / battery**
- **Filter in SQL, not memory.** Month/day/range queries use Room `WHERE date BETWEEN :start AND :end` so flows emit only the rows a screen needs. Kills the per-item `Calendar.getInstance()` scans (`HomeTab:66`, `AnalysisTab:51`, `TransactionsTab:49`). Add **indices** on `transactions(date)`, `transactions(accountId)`, `transactions(category)`.
- **No polling / no timers / no wake-y background work.** Notifications (Daily Reminder / Budget Alerts) use a single `WorkManager` periodic job with constraints (only if user enables; default off) — never a foreground loop.
- Aggregations (summary, breakdown, balances) computed once in UseCases off the main thread (`Dispatchers.Default`), cached via `stateIn` with `SharingStarted.WhileSubscribed(5_000)` so they stop when no screen observes.
- DataStore reads cold, cached; no per-frame disk hits.

**UI / snappy**
- All lists are `LazyColumn`/`LazyVerticalGrid` with stable `key =` (transaction id, category name) → minimal recomposition + item reuse.
- Hoist state; pass immutable data + lambdas; mark UI data classes `@Immutable`/`Stable`. Avoid allocating objects in composition (no `Color(parseColor())` per row — precompute as `Int`/`Color` in the mapper).
- No nested scroll conflicts; `derivedStateOf` for scroll-driven UI; `remember` keyed correctly so summaries don't recompute every frame.
- Donut chart animation runs once per data change (already), strokes in dp.
- Images (Discover/attachments) via Coil with size bounds + memory/disk cache; no full-res decode.

**Build / startup**
- `isMinifyEnabled = true` + `isShrinkResources = true` for release (currently **off** — `app/build.gradle.kts`), R8 full mode.
- **Baseline Profile** for the hot startup + Home/Add paths.
- `enableEdgeToEdge` already on; avoid overdraw (single background, no stacked opaque layers like the current gradient-on-gradient cards).
- Keep dependency surface minimal: don't pull Retrofit/OkHttp/Firebase/Moshi into runtime unless used (several are declared but unused → remove from `implementation` to shrink APK & method count).

**Verification (Part F adds):** Macrobenchmark for startup + scroll jank (frame timing); APK-size check in CI; StrictMode in debug to catch main-thread disk/DB.

---

# PART D — Per-screen UI spec (pixel-match)

For each: layout, components, data, and deltas from current. Screens marked `INFERRED` have no screenshot.

### D1. Bottom navigation (global)
- 5 slots: Home, Analysis, **center white circular [+] FAB (raised, notch)**, Accounts, More.
- Active = white icon+label; inactive = muted. Replaces current 4-tab + corner FAB.

### D2. Home (`Screenshot_2026_0611_111908`)
- Header: "Good Morning, **Guest User**" + search, premium (gold gem), avatar icons.
- **Cash Flow card**: "CASH FLOW" label + "This Month ▾" dropdown; SPENDING (coral) / INCOME (green) big amounts; Net Balance pill row.
- **Quick Setup Guide** card: radio rows (Customize categories, Set up accounts, Enable personalised alerts) + "Completed (N) ▸" expander.
- **Recent Transactions** + "See all" → transactions/day view. 3 rows, rounded-square category tiles.
- **Budgets** card: Monthly/Annual `XPillToggle`; "No Budget Yet?" empty + "Set Budget"; when set → progress.
- **Scheduled** card (premium): "Ready to Plan Ahead?" + "Upgrade Now".
- **Promo** banner (gold): "Get 50% off … Premium" + "Invite 1 friend" / "No, thanks".
- **Discover** carousel (premium/content): horizontally-scrolling image cards + "See more". Coil for images.
- "Customize" button bottom.
- Delta: total rebuild; current Home lacks budgets toggle, scheduled, promo, discover, quick-setup styling.

### D3. Add Transaction (`…111921`, `…111927`)
- Top bar: back + "Add transaction".
- `XPillToggle`: Expense / Income / Transfer.
- Date + Time row (calendar/clock icons, pickers).
- **Amount** row: ₹ + value + **calculator** affordance (calculator sheet — premium-ish; at minimum a numeric entry). Reference uses the **system keyboard**, not a custom keypad → drop the bespoke keypad.
- Category row → **`CategoryPickerSheet`** bottom sheet.
- Payment mode row → account picker sheet.
- Other details: "Write a note", "Add tags" + (?) help, tag chips (vacation/amazon/business), "Add attachment ▸".
- Save = **white circular floppy FAB**, bottom-right.
- Delta: replace custom keypad + inline tag UI; add attachment; bottom-sheet pickers; `rememberSaveable`/VM state.

### D4. Category Picker sheet (`…111931`, `…111938`)
- Title "Select Category" + grid/list toggle + edit (pencil → Categories screen) + close.
- Grid of colored category icons, filtered by Expense/Income. Income shows Others/Salary/Sold items/Coupons.

### D5. Categories screen (`…112005`)
- Top bar "Categories" + overflow. Expense/Income tabs. "Default Category ▸" row. "Edit order" button. 2-col grid of category tiles. + FAB → Edit Category (new).

### D6. Edit Category (`…112027`)
- Top bar: back + "Edit category" + delete (trash).
- Big preview icon. "Category name" field. Color palette grid (multi-row). **Grouped icon picker** (Food/Travel/Shopping/Family/Entertainment/Business/Finance/Medical/Utilities/Miscellaneous). "Text" overlay field. Save FAB.
- Requires a much larger icon set than current `CategoryIconHelper` (expand the icon map + grouping metadata).

### D7. Analysis (`…112042`)
- `XPillToggle`: Week/Month/Year/Custom. `DateRangeHeader` with "N TRANSACTIONS".
- `SummaryCard` (SPENDING/INCOME/Net Balance).
- Budget card ("No Budget for This Month?" / set).
- Trends card (premium upsell).
- **Categories** donut card w/ Spending/Income toggle + top category list.
- **Payment modes** card w/ Spending/Income/Transfers toggle + list.
- Stats: Average Spending (per day — real day count; per transaction), Average Income.
- Top-bar export/download icon (premium).

### D8. More (`…112102`)
- Profile: "Guest User / Sign in" + edit + security badge; "Last backup… / Backup now".
- 2-col action grid: Transactions, Scheduled Txns, Budgets, Categories, Tags, Go Premium.
- Views row: Day, Calendar, Custom.
- "Unlock Premium Features" banner ▸.
- More options list: Settings, Invite a friend, Rate app, Query/feedback, FAQs, About app.

### D9. Day View (`…112109`)
- Top bar "Day View" + add. Day `DateRangeHeader` ("Thu, 11 Jun 2026 / N TRANSACTIONS"). `SummaryCard`. Transaction list for the day.

### D10. Calendar View (`…112113`)
- Top bar "Calendar View". Month header. 7-col M–S grid; each day cell shows colored amount chips (income green / spend red). Tap day → Day View.

### D11. Custom View + Filter (`…112120`, `…112125`)
- Search field + filter icon. Active-filter chips: date (Jun 26), All categories, All payment modes. `SummaryCard`. Filtered transaction list. Filter **FAB**.
- **Filter sheet**: Year/month vs Date range toggle; Category (All/Spending/Income/Transfer + chips + Select all); Payment mode (chips + Select all). Replaces the broken hardcoded chips (#12).

### D12. Settings (`…112145`)
- App Icon picker (4 variants — premium activity-alias swap).
- Appearance: Theme, Time Format, Decimal Format.
- Preferences: Currency & Format, Default Payment Mode, Default Category, First Day of Month, Week Starts On, Home Page Balance View, Haptics (switch).
- Notifications: Daily Reminder, Budget Alerts (switches; WorkManager later).
- Backup/Restore/Export (most premium).
- Security: Account Balance Lock, App Lock, Biometric.
- Support, More, **Danger Zone**: Delete Data & Start Afresh, Delete Account.
- Backed by **DataStore** `PreferencesRepository`.

### D13. INFERRED screens (no screenshot — design to match, flag for review)
- **Accounts tab:** list of account cards (icon tile, name, type, balance; total net header `SummaryCard`); tap → account detail/transactions; + to add account. Matches card/tile language.
- **Budgets screen:** Monthly/Annual toggle; per-month budget with progress bars per category; set/edit.
- **Scheduled Txns:** list of recurring rules (premium-gated; show upsell when locked).
- **Tags:** list/manage tag chips; tap → filtered transactions.
- **Go Premium / Unlock Premium:** feature list + CTA (non-functional).

---

# PART E — Edge-case catalog (must be covered by code + tests)

1. Empty DB / first launch (single seed, no race).
2. Month with **no transactions** (summary zeros, donut empty ring, stats guarded).
3. Amount `0`, blank, non-numeric, multiple dots, leading zeros (`AddTransactionSheet:489`).
4. Very large amount / grouping / locale.
5. Decimal vs integer display policy (Settings Decimal Format).
6. February / 30 vs 31-day months for per-day average (#9).
7. Month nav across year boundary (`FinanceViewModel:138-154`) — keep, test.
8. Transfer: same from/to rejected; effect on both account balances; edit/delete reverses correctly (#7).
9. Editing then rotating (#3).
10. Deleting the **default** category, or a category in use by transactions (orphan handling).
11. Deleting an account with transactions (block or reassign).
12. Budget exceeded (ratio ≥ 1) styling (`HomeTab:287-327`).
13. Category/payment filter producing empty list (#12 empty state).
14. Today/Yesterday labels when viewing a past month (#13).
15. RTL + large font scale (`supportsRtl=true`) — layout integrity.
16. Currency change mid-session re-renders all amounts.
17. Clear-all while a write is in flight (#6 transactional).
18. DB migration 1→2 with existing v1 data (paymentMode→accountId backfill).

---

# PART F — Test plan

**Unit (JVM, no Android)** — the payoff of extracting UseCases:
- Every UseCase: summaries, breakdowns, balances (incl. transfers), stats (day-count math), search/filter, validation (`SaveTransactionUseCase`), month-cursor.
- `CurrencyFormatter` (locale/symbol/decimal).
- Fakes for repositories (interfaces enable this).

**Repository / DAO (Robolectric + in-memory Room):**
- CRUD per DAO; bulk delete; budget upsert; category reorder.
- **Migration test** 1→2 (Room `MigrationTestHelper`, schemas exported).
- Single-seed verification (no duplicates).

**Compose UI (Robolectric):**
- Add-transaction validation (Save disabled when amount 0; error shown).
- Filter sheet applies/clears.
- Navigation: FAB→Add, tab switches, back from pushed routes.

**Screenshot (Roborazzi — already wired):**
- One golden per screen vs the reference layout (Home, Add, Analysis, Categories, Edit Category, Day, Calendar, Custom, More, Settings, Accounts). Catches pixel-match regressions.

**Performance (Macrobenchmark + checks):**
- Startup (cold) timing on Home; scroll-jank frame timing on Transactions/Day lists.
- APK-size guard in CI; StrictMode (debug) flags main-thread disk/DB.
- Baseline Profile generated and verified.

**Targets:** UseCases ~100%; repositories high; one screenshot per screen; zero main-thread DB/disk; lists scroll jank-free.

---

# PART G — Phased roadmap (each phase independently shippable & green)

**Phase 0 — Safety net & tooling**
Add Hilt/nav/datastore deps; enable schema export; characterization tests for kept behavior; bump DB to v2 + migration scaffold. **Perf/build setup:** enable core-library desugaring (java.time), `isMinifyEnabled`+`isShrinkResources` for release, Baseline Profile + Macrobenchmark module, StrictMode in debug, drop unused deps.

**Phase 1 — Domain & data foundation (no UI change)**
Domain models, repository interfaces + impls, `Account`/`Scheduled` entities + migration, UseCases, DI modules. Move seed to single path (fix #1, #2). Unit-test all UseCases. App still runs on old screens calling new VMs.

**Phase 2 — Critical bug fixes**
#3 (saveable form), #4/#5 (validation + error surface), #6 (bulk delete), #7/#8 (transfers + real accounts), #9/#10/#11 (stats/format/currency). Tests per Part E.

**Phase 3 — Design system**
Tokens, typography, all `core/designsystem` components, navigation-compose host + 5-slot bottom bar + center FAB. Roborazzi harness.

**Phase 4 — Core screens pixel-match**
Home, Add (+ pickers), Analysis, Accounts, More. Screenshot goldens.

**Phase 5 — Secondary screens**
Categories, Edit Category, Day/Calendar/Custom + Filter sheet, Budgets, Settings (DataStore).

**Phase 6 — Premium placeholders**
Scheduled, Tags, Go Premium, Trends, Discover, app-icon picker, upsell banners. Non-functional but pixel-matched.

**Phase 7 — Polish & a11y**
RTL, font-scale, content descriptions, haptics, empty/error states, final screenshot diff pass.

---

# PART H — Self-audit: implementation-readiness

**Verdict: Implementation-ready for Phases 0–3; Phases 4–6 ready per-screen for screens with screenshots, `INFERRED` screens need user sign-off before build.**

Ready:
- Audit findings are concrete (file:line, fix each).
- Architecture target is specific (packages, entities, migration, UseCase list, DI, deps with real catalog versions).
- Test strategy maps to extracted seams.
- Phasing keeps every step green and reversible.

**Resolved by user (2026-06-11):**
- ✅ INFERRED screens (Accounts/Budgets/Scheduled/Tags/Go Premium) → follow the reference UI language; approved to build.
- ✅ Currency default **INR**, user-changeable.
- ✅ **No billing, no auth, personal use** → premium/Sign-in/backup are non-functional pixel-match placeholders; build the genuinely useful "premium" features (Scheduled, Budgets, Tags) as **real, free** functionality, no upsell gating logic.
- ✅ **Performance/battery/snappy** is a hard NFR → Part C4 + Macrobenchmark in Part F.

Remaining open / assumptions (do **not** block Phase 0–3):
1. **Calculator** affordance on Add screen: assumed plain numeric entry (lighter/snappier); full in-app calculator deferred. Confirm if you want the calculator.
2. **Min sdk 24** → `java.time` needs core-library **desugaring**; enable it in Phase 0 (else `ThreeTenABP`). Decision flagged, not blocking.
3. **Discover content** source: assumed static/bundled placeholder images (no network → lighter, battery-friendly).
4. **Multi-module vs package-by-layer**: chose single-module packages for build simplicity; revisit only if it grows.
5. **Unused declared deps** (Retrofit/OkHttp/Moshi/Firebase): assumed removable — confirm none are needed, then drop to shrink APK.

Risks:
- Pixel-match of 14+ screens is large; Roborazzi goldens are the guardrail.
- Migration 1→2 with real user data must be tested before release (covered in Part F).
- `java.time` + minSdk24 desugaring is the one easy-to-miss config (item 6).
