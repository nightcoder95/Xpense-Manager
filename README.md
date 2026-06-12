# Xpense Manager

An elegant, always-dark personal finance app for Android — transaction logging, multi-account tracking, budgets, and rich visual analytics, with optional Gemini-powered insights.

> Expense Manager: a personal finance planner, transaction ledger, and descriptive analytics dashboard.

## Features

- **Transactions** — log income/expense entries with categories, tags, accounts, and notes
- **Accounts** — track balances across multiple accounts
- **Budgets** — set and monitor spending limits
- **Categories & Tags** — fully customizable, editable taxonomy
- **Analytics** — descriptive visual dashboards over your spending
- **Backup & Restore** — local data export/import
- **Dark-first design** — a polished, always-dark Material 3 UI
- **AI insights** — server-side Gemini API integration

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | Clean Architecture (data / domain / ui) + MVVM |
| DI | Hilt |
| Persistence | Room, DataStore Preferences |
| Networking | Retrofit, OkHttp, Moshi |
| Images | Coil |
| Backend | Firebase, Gemini API |
| Testing | JUnit, Robolectric, Roborazzi (screenshot tests), Macrobenchmark |
| Build | Gradle (Kotlin DSL), KSP, Secrets Gradle Plugin |

**Min SDK** 24 · **Target SDK** 36 · **Java** 11

## Project Structure

```
app/src/main/java/com/example/
├── core/          # design system, datetime, common utils
├── data/          # Room (entity, dao), repositories, preferences, backup, model
├── domain/        # use cases, domain models, repository interfaces
├── di/            # Hilt modules
└── ui/            # Compose screens, feature modules, components, theme, navigation
    └── feature/   # home, accounts, budgets, categories, tags, analysis, settings, ...
benchmark/         # Macrobenchmark module
docs/              # design specs and implementation plans
```

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- Android SDK 36

### Setup

1. Clone:
   ```bash
   git clone https://github.com/nightcoder95/Xpense-Manager.git
   cd Xpense-Manager
   ```

2. Create `.env` from the template and add your Gemini API key:
   ```bash
   cp .env.example .env
   ```
   ```properties
   GEMINI_API_KEY=your_key_here
   ```

3. Build:
   ```bash
   ./gradlew assembleDebug
   ```

4. Install on a connected device/emulator:
   ```bash
   ./gradlew installDebug
   ```

### Release Builds

Release signing reads from environment variables (keystore is **not** committed):

```bash
export KEYSTORE_PATH=/path/to/my-upload-key.jks
export STORE_PASSWORD=...
export KEY_PASSWORD=...
./gradlew assembleRelease
```

## Testing

```bash
./gradlew test                    # unit + Robolectric + screenshot tests
./gradlew connectedAndroidTest    # instrumented tests
```

## Security

`.env`, `*.jks`, `debug.keystore`, and `local.properties` are git-ignored. Never commit secrets or signing keys.

## License

See [LICENSE](LICENSE).
