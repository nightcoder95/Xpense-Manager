# UPI Auto-Capture — Design Spec

**Date:** 2026-06-12
**Status:** Approved design, pre-implementation
**App:** Xpense-Manager (native Kotlin/Compose, MVVM + Room + DataStore + Hilt)
**Distribution:** Personal / sideload (no Play Store policy constraints)

## Problem

99% of the user's spends are UPI payments, cycled across 8 apps (GPay, PhonePe,
Paytm, CRED, Navi, Super.money, MobiKwik, Amazon Pay). Manual entry afterward is
friction and gets skipped. Goal: right after a successful payment, the app pops a
pre-filled modal (amount + which app + payee) so the user only picks a category +
optional remark and taps Save.

Hard constraints: **accuracy** and **near-zero battery/RAM cost**.

## Why prior attempt failed (reference repo `Xpense_tracker/ExpenseTracker`)

A React Native build used an `AccessibilityService` (`UpiScraperService.kt`) that:
- Listened **globally** (all apps), so it woke on every UI event everywhere.
- Used `typeWindowContentChanged` → fired per-frame.
- Matched one **generic** brittle string set (`₹`, `Successful`, `Paid to`).

Result: battery drain + low accuracy. The drain came from *global + per-frame*,
not from accessibility itself.

## Detection signal decision

Evaluated three signals against user's real experience:

- **Notifications:** apps mostly DON'T post on success (user is in-app looking at
  the success screen). Misses most. Rejected.
- **Bank UPI SMS:** ~6/10 delivery, can't identify source app, payee is raw VPA,
  and wakes on every incoming SMS (user gets many). Rejected.
- **Accessibility (success screen):** the success screen is the ONLY signal present
  every single time, because that's exactly where the user is. **Chosen.**

The success screen *is* the ground truth. The task is to read it cheaply.

## Battery / RAM rationale

- AccessibilityService is event-driven and system-bound — **no polling, no loop,
  no wakelock, no AlarmManager/JobScheduler**. Idle = 0 CPU.
- Scoped via config XML:
  - `android:packageNames` = the 8 payment packages → system delivers events ONLY
    when one of those apps is foreground. Dormant everywhere else.
  - `android:accessibilityEventTypes="typeWindowStateChanged"` → ~1 event per
    screen switch, not per frame.
- Per payment ≈ 5–10 window changes × microseconds (early-exit probe) = single-digit
  ms CPU. Outside the 8 apps: 0 events → 0 CPU → 0 battery.
- RAM: service is a lean object in the app process; it loads NO Room/Hilt/Compose,
  holds only `{app, amount, payee, ts}`. Hosting an a11y service raises process
  priority (tends to stay resident) but idle-resident ≠ battery.
- Verifiable via Battery Historian (`dumpsys batterystats`, zero idle wakeups) and
  Energy Profiler.

| | Reference (failed) | This design |
|---|---|---|
| Scope | all apps | 8 packages |
| Event type | content-changed (per-frame) | window-state (per-screen) |
| Idle CPU | wakes on all UI | 0 |
| Parser | 1 generic | per-app |

## Architecture

New isolated package `com.example.capture` (app namespace is `com.example`).
One-way dependency: `capture → domain` (use case). `domain` never references capture.

```
com.example.capture/
├── service/PaymentDetectionService.kt   AccessibilityService, scoped to 8 pkgs
├── parser/
│   ├── PaymentParser.kt                  interface: parse(root) -> PaymentSignal?
│   ├── GpayParser.kt PhonePeParser.kt PaytmParser.kt CredParser.kt
│   │   NaviParser.kt SuperMoneyParser.kt MobikwikParser.kt AmazonPayParser.kt
│   └── ParserRegistry.kt                 packageName -> PaymentParser
├── model/PaymentSignal.kt               {sourceApp, amountMinor: Long, payee:String?, rawText, ts}
├── dedup/SignalDeduplicator.kt          drop repeat fires of same success screen
└── ui/CaptureModalActivity.kt           transparent overlay activity = the popup
```

Principles:
- **Service is a thin sensor:** detect → build `PaymentSignal` → launch modal. No DB,
  no business logic.
- **Parsers are pure functions** (`AccessibilityNodeInfo` → `PaymentSignal?`),
  unit-tested offline against captured node dumps. No Android runtime needed.
- **Modal reuses existing write path:** `CaptureModalActivity` pre-fills, user
  completes, Save flows through the existing `SaveTransactionUseCase`. No duplicate
  write path.
- Feature gated by a DataStore master toggle; disabling is instant (checked first in
  `onAccessibilityEvent`).

## Data flow

```
1. User finishes payment; app shows success screen.
2. System delivers TYPE_WINDOW_STATE_CHANGED (only because pkg is in scoped list).
3. Service checks master toggle -> if off, return.
4. Early-exit probe: rootNode contains the parser's successMarkers?
      no  -> return (microseconds, no tree walk)
      yes -> continue
5. ParserRegistry[event.packageName].parse(rootNode) -> PaymentSignal?
      null (not confident) -> return, NO modal  (bias: false-negative over false-positive)
6. SignalDeduplicator.seen(signal)? -> drop if repeat (same app+amount within window)
7. Launch CaptureModalActivity with extras (amount, app, payee, ts).
      (Background-activity-launch is permitted from an a11y service.)
8. Modal: amount + app pre-filled; payee -> remark hint; default account; category picker.
9. User picks category (+ optional remark), taps Save.
10. Save -> SaveTransactionUseCase (existing path). Done.
```

## Parser strategy (accuracy lever)

```kotlin
interface PaymentParser {
    val packageName: String
    val successMarkers: List<String>                 // cheap probe before tree walk
    fun parse(root: AccessibilityNodeInfo): PaymentSignal?   // null = not confident
}
```

- **Two-stage:** cheap `successMarkers` text probe → only on hit walk the tree.
- **Amount:** regex `₹\s?([\d,]+\.?\d*)`, normalized to minor units (paise, `Long`)
  internally; converted to the `Transaction.amount: Double` on save. Reject if no
  valid amount → null.
- **Payee:** best-effort from app-specific anchor ("Paid to", "To:", "Banking name").
  Missing payee ≠ failure — amount alone still fires; payee only pre-fills remark.
- **Stable IDs:** prefer `viewIdResourceName` where an app exposes stable resource
  IDs (survives wording/locale changes); text match is fallback.
- **Resilience:** every parse wrapped in try/catch → exception swallowed, returns
  null, never crashes the payment app. Mandatory `AccessibilityNodeInfo` recycling.

### Debug capture mode (how parsers are built accurately)

A debug-only toggle makes the service dump the full node tree (text +
`viewIdResourceName`) of a success screen to a file. User makes one real payment per
app; each parser is built from the actual dump — exact, not guessed. This is the step
the reference skipped.

### Target packages (v1: all 8)

| App | Package |
|---|---|
| GPay | `com.google.android.apps.nbu.paisa.user` |
| PhonePe | `com.phonepe.app` |
| Paytm | `net.one97.paytm` |
| CRED | `com.dreamplug.androidapp` |
| Navi | `com.naviapp` |
| Super.money | `money.super.payments` |
| MobiKwik | `com.mobikwik_new` |
| Amazon Pay | `in.amazon.mShop.android.shopping` |

> Packages to be confirmed against installed apps during the debug-dump step; the
> dump step naturally verifies each.

## Permissions & onboarding

- Accessibility cannot be code-granted. Settings screen gets an "Auto-capture
  payments" card → plain-language explainer → deep-link `ACTION_ACCESSIBILITY_SETTINGS`
  (port the intent pattern from reference `TrackerModule`).
- First-run consent: state exactly what is read (success screens of the 8 payment
  apps only — enforced by `packageNames` scope), that data never leaves the device,
  and no other app/screen is touched.
- Master toggle in DataStore; off = service returns immediately (permission stays).

## Error handling / resilience

- Parse exception → null, no modal, no crash.
- Mandatory node recycling (prevents a11y memory leak).
- Service killed/rebound by OS → fine, it is stateless; rebinds on next event.
- Modal swipe/dismiss = discard, zero writes. Save is the only write.
- Dedup window guards double-fire from multiple `windowStateChanged` events.

## Testing

- **Parsers (bulk):** pure unit tests over captured node dumps — happy path, missing
  payee, no amount, garbage screen → null. No device.
- **Dedup:** repeat / within-window / outside-window.
- **Modal → Save:** instrumented test, pre-fill → `SaveTransactionUseCase` write.
- **Manual matrix:** one real payment per app after its parser lands.

## Out of scope (v1, YAGNI)

- SMS detection/backfill (dropped: every-SMS wakeups, no source app, raw-VPA payee).
- Notification-based detection (apps don't post on success).
- Auto-categorization ML.
- Multi-account auto-pick (use default account; user changes in modal).
- Floating bubble / heads-up notification surfaces (overlay activity chosen).

## Acceptance criteria

1. With the feature on, completing a payment in any of the 8 apps shows the modal
   with correct amount and correct source app within ~1s.
2. Save writes a transaction via `SaveTransactionUseCase` identical in shape to a
   manual entry.
3. With the feature off (toggle), zero modals; service returns immediately.
4. Idle (not in a payment app): zero accessibility events handled — verified via
   Battery Historian showing no wakeups attributable to the service.
5. A parse failure or unknown screen produces no modal and no crash.
6. Each of the 8 parsers passes its unit tests against a real captured node dump.
