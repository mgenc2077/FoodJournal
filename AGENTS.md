# Food Journal — Android App

## Project overview

Single-module Android app (`:app`), Kotlin, package `com.mgenc.foodjournal`.
Entrypoint: `MainActivity` (ComponentActivity) uses Jetpack Compose with Navigation 3.
Data layer: Room (SQLite) via `FoodJournalApp` Application class.
Sync: Go server in `sync-engine/` directory.

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
- **Room** database (version 6) with KSP (`2.3.8`) for annotation processing. Entities + DAOs in `data/Database.kt`. Migrations in `FoodJournalApp.kt`.
- NavDisplay transitions are disabled (`EnterTransition.None togetherWith ExitTransition.None`) — no white flash on navigation.
- **AGP 9.2.1** has built-in Kotlin — do NOT apply `org.jetbrains.kotlin.android`. Apply `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.kotlin.plugin.serialization`, and `com.google.devtools.ksp` only.
- No `kotlinOptions` block in AGP 9.x — JVM target follows `compileOptions`.
- **Java 17** source/target compatibility. compileSdk 36, minSdk 34, targetSdk 36.
- XML themes are minimal (platform `android:Theme.Material.Light.NoActionBar`) — real theming happens in Compose (`ui/theme/Theme.kt`).
- Edge-to-edge via `enableEdgeToEdge()` in `MainActivity`.
- Version catalog at `gradle/libs.versions.toml` — add dependencies there, not hardcoded.
- DI is manual via `FoodJournalApp` Application class + `AndroidViewModel` — no Hilt/Dagger.
- Cleartext HTTP enabled via `network_security_config.xml` (for LAN sync).

## Screen flow

- **DailyJournal** (start) → hamburger drawer with "Today", "Timeline", "Recipes", "Reminders", and "Settings"
- **AddEntry** ← FAB on DailyJournal; includes recipe dropdown that pre-fills name + notes
- **EditEntry** ← tap any entry row (from DailyJournal or Timeline)
- **Timeline** ← drawer item; scrollable list grouped by date then meal type
- **Recipes** ← drawer item; list of recipe templates
- **EditRecipe** ← tap a recipe or FAB on Recipes screen
- **Settings** ← drawer item; server URL, sync button, last sync time
- **Reminders** ← drawer item; daily food input reminders per meal (Breakfast, Lunch, Dinner) via WorkManager

## Key paths

| What | Path |
|---|---|
| App build config | `app/build.gradle.kts` |
| Version catalog | `gradle/libs.versions.toml` |
| Application + migrations | `FoodJournalApp.kt` |
| Entity + DAO + DB | `data/Database.kt` |
| Sync wire models | `data/SyncModels.kt` |
| UUIDv7 utility | `util/UUID.kt` |
| Activity + routes | `MainActivity.kt` |
| Compose theme | `ui/theme/Theme.kt` |
| Network security config | `app/src/main/res/xml/network_security_config.xml` |
| Screens | `screen/DailyJournalScreen.kt`, `AddEditEntryScreen.kt`, `TimelineScreen.kt`, `RecipeListScreen.kt`, `EditRecipeScreen.kt`, `SettingsScreen.kt`, `screen/ReminderScreen.kt` |
| ViewModels | `viewmodel/DailyJournalViewModel.kt`, `AddEditEntryViewModel.kt`, `TimelineViewModel.kt`, `RecipeListViewModel.kt`, `EditRecipeViewModel.kt`, `SettingsViewModel.kt`, `viewmodel/ReminderViewModel.kt` |
| Strings | `app/src/main/res/values/strings.xml` |
| Go sync server | `sync-engine/` |

## Sync architecture

- All IDs are UUIDv7 (text, not auto-increment Long).
- Both entities have `updatedAt` (unix ms) for change tracking and `deletedAt` (nullable) for soft deletes.
- Deletion is soft — sets `deletedAt` + `updatedAt`. UI filters out deleted rows. Deleted rows sync to propagate the deletion.
- `SettingsViewModel` handles the full sync flow: POST /sync → handle 303 → rebuild → re-sync.
- Server URL and last sync time persisted in SharedPreferences.
- Conflict resolution: always keep the row with the highest `updatedAt`.
