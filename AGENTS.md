# Food Journal — Android App

## Project overview

Single-module Android app (`:app`), Kotlin, package `com.mgenc.foodjournal`.
Entrypoint: `MainActivity` (ComponentActivity) uses Jetpack Compose with Navigation 3.
Data layer: Room (SQLite) via `FoodJournalApp` Application class.

## Build & test

```sh
./gradlew assembleDebug          # debug APK
./gradlew test                   # unit tests (src/test)
./gradlew connectedAndroidTest   # instrumented tests (src/androidTest, requires device/emulator)
./gradlew lint                   # Android Lint
```

## Architecture & conventions

- **Jetpack Compose** with Material 3 dynamic theming (Compose BOM `2026.03.00`).
- **Navigation 3** (`navigation3-runtime` + `navigation3-ui`) — routes are `@Serializable` data objects/classes implementing `NavKey`. Use `rememberNavBackStack`, `NavDisplay`, and `entryProvider` DSL.
- **Room** database with KSP (`2.3.8`) for annotation processing. Entity in `data/Database.kt`.
- **AGP 9.2.1** has built-in Kotlin — do NOT apply `org.jetbrains.kotlin.android`. Apply `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.kotlin.plugin.serialization`, and `com.google.devtools.ksp` only.
- No `kotlinOptions` block in AGP 9.x — JVM target follows `compileOptions`.
- **Java 17** source/target compatibility. compileSdk 36, minSdk 34, targetSdk 36.
- XML themes are minimal (platform `android:Theme.Material.Light.NoActionBar`) — real theming happens in Compose (`ui/theme/Theme.kt`).
- Edge-to-edge via `enableEdgeToEdge()` in `MainActivity`.
- Version catalog at `gradle/libs.versions.toml` — add dependencies there, not hardcoded.
- DI is manual via `FoodJournalApp` Application class + `AndroidViewModel` — no Hilt/Dagger.

## Screen flow

- **DailyJournal** (start) → hamburger drawer with "Today" and "Timeline"
- **AddEntry** ← FAB on DailyJournal
- **EditEntry** ← tap any entry row (from DailyJournal or Timeline)
- **Timeline** ← drawer item; scrollable list grouped by date

## Key paths

| What | Path |
|---|---|
| App build config | `app/build.gradle.kts` |
| Version catalog | `gradle/libs.versions.toml` |
| Application + Room | `FoodJournalApp.kt` |
| Entity + DAO + DB | `data/Database.kt` |
| Activity + routes | `MainActivity.kt` |
| Compose theme | `ui/theme/Theme.kt` |
| Screens | `screen/DailyJournalScreen.kt`, `AddEditEntryScreen.kt`, `TimelineScreen.kt` |
| ViewModels | `viewmodel/DailyJournalViewModel.kt`, `AddEditEntryViewModel.kt`, `TimelineViewModel.kt` |
| Strings | `app/src/main/res/values/strings.xml` |
