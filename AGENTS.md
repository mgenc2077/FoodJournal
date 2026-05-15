# Food Journal — Android App

## Project overview

Single-module Android app (`:app`), Kotlin, package `com.mgenc.foodjournal`.
Entrypoint: `MainActivity` (ComponentActivity) uses Jetpack Compose with Navigation 3.
Screens: `FirstScreen` → `SecondScreen`, wired via `NavDisplay` + `entryProvider` DSL.

## Build & test

```sh
./gradlew assembleDebug          # debug APK
./gradlew test                   # unit tests (src/test)
./gradlew connectedAndroidTest   # instrumented tests (src/androidTest, requires device/emulator)
./gradlew lint                   # Android Lint
```

## Architecture & conventions

- **Jetpack Compose** with Material 3 dynamic theming (Compose BOM `2026.03.00`).
- **Navigation 3** (`navigation3-runtime` + `navigation3-ui`) — routes are `@Serializable` data objects implementing `NavKey`. Use `rememberNavBackStack`, `NavDisplay`, and `entryProvider` DSL.
- **AGP 9.2.1** has built-in Kotlin — do NOT apply `org.jetbrains.kotlin.android`. Apply `org.jetbrains.kotlin.plugin.compose` and `org.jetbrains.kotlin.plugin.serialization` only.
- No `kotlinOptions` block in AGP 9.x — JVM target follows `compileOptions`.
- **Java 17** source/target compatibility. compileSdk 36, minSdk 34, targetSdk 36.
- XML themes are minimal (platform `android:Theme.Material.Light.NoActionBar`) — real theming happens in Compose (`ui/theme/Theme.kt`).
- Edge-to-edge via `enableEdgeToEdge()` in `MainActivity`.
- Version catalog at `gradle/libs.versions.toml` — add dependencies there, not hardcoded.

## Key paths

| What | Path |
|---|---|
| App build config | `app/build.gradle.kts` |
| Version catalog | `gradle/libs.versions.toml` |
| Activity + routes | `app/src/main/java/com/mgenc/foodjournal/MainActivity.kt` |
| Compose theme | `app/src/main/java/com/mgenc/foodjournal/ui/theme/Theme.kt` |
| Screens | `app/src/main/java/com/mgenc/foodjournal/FirstScreen.kt`, `SecondScreen.kt` |
| Strings | `app/src/main/res/values/strings.xml` |
