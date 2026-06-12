# Phase 3 — Design System + Navigation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Build the single source of truth for the pixel-match UI — design tokens, typography, and reusable components from the screenshots — and replace the hand-rolled tab `when` + overlay with `navigation-compose`, a 5-slot bottom bar with a center FAB, and proper insets/back handling.

**Architecture:** `core/designsystem` holds tokens + stateless components. A `NavHost` owns all routes; the bottom bar drives tab destinations and the center FAB opens `addTransaction` as a real destination (fixes A1#17). Roborazzi golden harness is set up so every later screen has a screenshot gate.

**Tech Stack:** Compose, navigation-compose (enabled Phase 0), Roborazzi (wired), Material3.

**Source spec:** `docs/superpowers/specs/2026-06-11-...-design.md` (Part C, A1#17, D1). **Builds on:** Phases 0–2.

**Reference screenshots (assets/):** bottom nav + center FAB visible in `Screenshot_2026_0611_111908.jpg`, `..._112042.jpg`, `..._112102.jpg`.

> Note on UI fidelity: exact dp/sp/color values below are the starting spec from the screenshots; the Roborazzi golden + visual diff against the reference image is the acceptance gate. Tune values during execution until the golden matches.

---

### Task 1: Design tokens (replace `ui/theme/Color.kt`)

**Files:**
- Modify: `app/src/main/java/com/example/ui/theme/Color.kt`
- Create: `app/src/main/java/com/example/core/designsystem/Tokens.kt`

- [ ] **Step 1: Define semantic tokens** (`Tokens.kt`)

```kotlin
package com.example.core.designsystem
import androidx.compose.ui.graphics.Color
object XColors {
    val Background = Color(0xFF0B0D10)
    val Surface = Color(0xFF16191F)
    val SurfaceVariant = Color(0xFF1E222A)
    val Outline = Color(0xFF262B33)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9AA1AC)
    val Spending = Color(0xFFF2706B)
    val Income = Color(0xFF43C59E)
    val AccentGold = Color(0xFFE3B341)
    val Indigo = Color(0xFF6C5DD3)
    val OnAccent = Color(0xFF0B0D10)
}
fun String.toComposeColor(): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(XColors.Indigo)
```

- [ ] **Step 2: Point `Color.kt` legacy names at the new tokens** so existing screens keep compiling (e.g. `val ObsidianBackground = XColors.Background`, `SurfaceCard = XColors.Surface`, `SurfaceCardElevated = XColors.SurfaceVariant`, `NeutralMutedText = XColors.TextSecondary`, `SoftWhiteText = XColors.TextPrimary`, `IncomeNeonGreen = XColors.Income`, `ExpenseNeonCoral = XColors.Spending`, `PremiumAccentGold = XColors.AccentGold`, `IndigoSpark = XColors.Indigo`). This makes the whole app adopt the new palette in one edit.

- [ ] **Step 3: Build + commit**

```bash
./gradlew :app:assembleDebug && git add app/src/main/java/com/example/core/designsystem/Tokens.kt app/src/main/java/com/example/ui/theme/Color.kt && git commit -m "feat(ds): semantic color tokens; legacy names aliased; toComposeColor ext"
```

---

### Task 2: Typography scale (`ui/theme/Type.kt`)

**Files:**
- Modify: `app/src/main/java/com/example/ui/theme/Type.kt`

- [ ] **Step 1: Full Material3 scale** — define `displayLarge` (amounts: Black, ~32sp, tight tracking), `titleLarge`/`titleMedium`, `bodyMedium`, `labelSmall` (uppercase section labels) matching the screenshots. Keep `FontFamily.Default`.

- [ ] **Step 2: Build + commit**

```bash
./gradlew :app:assembleDebug && git add app/src/main/java/com/example/ui/theme/Type.kt && git commit -m "feat(ds): full typography scale"
```

---

### Task 3: Core components — part 1 (containers, text, buttons)

**Files:**
- Create: `app/src/main/java/com/example/core/designsystem/Components.kt`
- Test (screenshot): `app/src/test/java/com/example/ds/ComponentScreenshotTest.kt`

- [ ] **Step 1: Implement components**

`XSurfaceCard(modifier, content)` (Surface bg, 1dp Outline border, 20dp radius), `SectionHeader(text)` (uppercase labelSmall, TextSecondary), `XPrimaryButton(text, enabled, onClick)`, `AmountText(amount, type, style)` (sign + Spending/Income color + `CurrencyFormatter`), `EmptyState(icon, title, subtitle)`.

```kotlin
package com.example.core.designsystem
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.core.common.CurrencyFormatter

@Composable fun XSurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier
        .clip(RoundedCornerShape(20.dp))
        .background(XColors.Surface)
        .border(1.dp, XColors.Outline, RoundedCornerShape(20.dp))
        .padding(16.dp), content = content)
}
// SectionHeader, XPrimaryButton, AmountText(uses CurrencyFormatter("₹")), EmptyState ...
```

- [ ] **Step 2: Roborazzi golden test for a sample card** (proves harness works)

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentScreenshotTest {
    @get:Rule val compose = createComposeRule()
    @Test fun surfaceCard_matches() {
        compose.setContent { XSurfaceCard { Text("Hello") } }
        compose.onRoot().captureRoboImage("build/roborazzi/surfaceCard.png")
    }
}
```

- [ ] **Step 3: Run screenshot test**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.ds.ComponentScreenshotTest" -Proborazzi.test.record=true`
Expected: golden PNG written under `build/roborazzi/`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/core/designsystem/Components.kt app/src/test/java/com/example/ds && git commit -m "feat(ds): core components (card/header/button/amount/empty) + roborazzi harness"
```

---

### Task 4: Core components — part 2 (toggles, selectors, rows, nav bar)

**Files:**
- Modify: `app/src/main/java/com/example/core/designsystem/Components.kt`
- Create: `app/src/main/java/com/example/core/designsystem/BottomNavBar.kt`

- [ ] **Step 1: Implement** `XPillToggle(options: List<String>, selected, onSelect)` (rounded capsule, active segment highlighted — used by Expense/Income/Transfer, Week/Month/Year/Custom, Monthly/Annual, Spending/Income/Transfers), `MonthSelector(label, onPrev, onNext)`, `DateRangeHeader(title, subtitle)` ("June 2026" / "N TRANSACTIONS"), `TransactionRow(category, account, note, amount, type, time, color, onClick)` (rounded-square category tile per screenshots), `CategoryTile(name, icon, color, selected)`.

- [ ] **Step 2: Implement** `BottomNavBar` — 5 slots (Home, Analysis, center FAB notch, Accounts, More); center white circular `[+]` raised FAB; active=TextPrimary, inactive=TextSecondary. Stateless: takes `current`, `onSelect`, `onAdd`.

- [ ] **Step 3: Golden tests** for `XPillToggle`, `TransactionRow`, `BottomNavBar` (record mode).

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.ds.*" -Proborazzi.test.record=true`
Expected: goldens written.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/core/designsystem && git commit -m "feat(ds): pill toggle, month selector, transaction row, 5-slot bottom nav"
```

---

### Task 5: Navigation graph (replace tab `when` + overlay)

**Files:**
- Create: `app/src/main/java/com/example/ui/navigation/Routes.kt`
- Create: `app/src/main/java/com/example/ui/navigation/XpenseNavHost.kt`
- Modify: `app/src/main/java/com/example/MainActivity.kt`

- [ ] **Step 1: Routes**

```kotlin
package com.example.ui.navigation
object Routes {
    const val HOME = "home"; const val ANALYSIS = "analysis"; const val ACCOUNTS = "accounts"; const val MORE = "more"
    const val ADD = "add?txnId={txnId}"
    const val CATEGORIES = "categories"; const val EDIT_CATEGORY = "editCategory?name={name}"
    const val BUDGETS = "budgets"; const val SCHEDULED = "scheduled"; const val TAGS = "tags"
    const val SETTINGS = "settings"; const val DAY = "dayView"; const val CALENDAR = "calendarView"
    const val CUSTOM = "customView"; const val GO_PREMIUM = "goPremium"
    val bottomTabs = listOf(HOME, ANALYSIS, ACCOUNTS, MORE)
}
```

- [ ] **Step 2: NavHost + Scaffold with `BottomNavBar`** — `XpenseNavHost(navController)` defines a composable per route. Bottom bar shown only on `bottomTabs`. Center FAB → `navController.navigate("add?txnId=-1")`. `add` is a full-screen destination (fixes A1#17: real back stack + insets, no manual overlay). Wrap existing `HomeTab/AnalysisTab/TransactionsTab/MoreTab` as destinations for now; Phase 4 rebuilds them. Map old `AppTab` usage out.

- [ ] **Step 3: `MainActivity` hosts the NavHost** — replace `MainAppContent`'s `Scaffold`+`when`+`AnimatedVisibility` overlay with `rememberNavController()` + `XpenseNavHost`. Keep the error Snackbar (Phase 2 Task 1) at this level.

- [ ] **Step 4: Build + manual nav check**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL; tabs switch via nav; FAB opens Add as a screen; system back returns.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/ui/navigation app/src/main/java/com/example/MainActivity.kt && git commit -m "feat(nav): navigation-compose graph, 5-slot bottom bar, Add as destination (fixes A1#17)"
```

---

### Task 6: Apply theme + insets globally

**Files:**
- Modify: `app/src/main/java/com/example/ui/theme/Theme.kt`
- Modify: `app/src/main/java/com/example/ui/navigation/XpenseNavHost.kt`

- [ ] **Step 1:** Point `MaterialTheme` `colorScheme` at the new `XColors`; ensure `background`/`surface`/`onX` mapped. Use `Scaffold` `contentWindowInsets` + `WindowInsets.systemBars` once at the NavHost level; remove the per-screen `windowInsetsPadding` duplication.

- [ ] **Step 2:** Build + commit

```bash
./gradlew :app:assembleDebug && git add app/src/main/java/com/example/ui/theme/Theme.kt app/src/main/java/com/example/ui/navigation/XpenseNavHost.kt && git commit -m "feat(ds): global theme + unified insets"
```

---

### Task 7: Phase wrap

- [ ] **Step 1:** `./gradlew :app:testDebugUnitTest :app:assembleDebug` → green; goldens recorded.
- [ ] **Step 2:** Manual: every tab reachable; FAB→Add→back; visuals adopt new tokens.
- [ ] **Step 3:** `git add -A && git commit -m "chore: phase 3 complete" && git tag phase-3-done`

---

## Self-Review

**Spec coverage (Part C, A1#17, D1):** tokens C1 → Task 1 ✅; typography C3 → Task 2 ✅; all C2 components → Tasks 3–4 ✅; perf C4 (stable rows/keys reused later) — components designed stateless+immutable ✅; bottom nav D1 → Task 4/5 ✅; A1#17 insets/back → Task 5/6 ✅.
**Placeholder scan:** exact dp/sp tuning explicitly gated by Roborazzi goldens (stated up top) — not a placeholder, an acceptance method.
**Type consistency:** `XColors` token names reused everywhere; `CurrencyFormatter("₹")` signature matches Phase 1; `BottomNavBar(current,onSelect,onAdd)` and `Routes.bottomTabs` consistent with NavHost usage.
**Deferred:** screen rebuilds = Phases 4–7.
