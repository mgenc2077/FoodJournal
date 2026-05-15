# Food Journal

I wanted to use an app for tracking what you eat each day, but found all the available apps too complex, so I made my own.

## Features

- **Daily journal** — view and manage entries for any day with prev/next navigation
- **Meal categories** — tag entries as Breakfast, Lunch, Dinner, or Snack
- **Notes** — optional free-text notes on each entry
- **Timeline** — scrollable view of all entries grouped by date and meal type
- **Reorder** — move entries up or down within a meal group
- **Recipes** — save recipe templates with ingredient notes; quick-fill new entries from a recipe dropdown
- **Edit & delete** — tap to edit, delete button on every entry
- **Material You** — dynamic color theming that adapts to your wallpaper

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Navigation 3 (NavDisplay + entryProvider DSL)
- Room (SQLite) with KSP for persistence
- Manual DI via Application class + AndroidViewModel
- AGP 9.2.1, compileSdk 36, minSdk 34

## Releases

Pre-built APKs are available on the [Releases page](../../releases). Each release is automatically built and published when a version tag (`vX.X.X`) is pushed.

## Screens

| Screen | Purpose |
|---|---|
| Daily Journal | Main screen; entries for one day, grouped by meal, with day navigation and FAB to add |
| Add Entry | Form to create a new entry (food name, meal type, notes); recipe dropdown pre-fills from templates |
| Edit Entry | Edit an existing entry or delete it |
| Timeline | All entries across all days, grouped by date then meal type |
| Recipes | Manage recipe templates (name + ingredient notes) |

## Local development

```sh
./gradlew assembleDebug          # debug APK
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # instrumented tests (requires device/emulator)
```