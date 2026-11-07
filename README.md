# Field Incident Reporter

Field technicians photograph and report physical incidents (broken equipment, road hazards) from an
Android app. Reports are stored on the device first and synced to the API when a connection is
available, so the app works offline.

| Folder | What |
|---|---|
| [`android/`](android/) | Android app: Kotlin, Jetpack Compose, MVVM, Hilt, Room, WorkManager |
| [`docs/`](docs/) | Architecture decisions and the AI usage log |

## Run the Android app

Requirements: Android Studio (stable) with an emulator or a device on API 26+. The Gradle wrapper
provisions the JDKs it needs.

```bash
cd android
./gradlew installDebug
```

Or open the `android/` folder in Android Studio and run the `app` configuration.

## Checks

```bash
cd android
./gradlew spotlessCheck assembleDebug testDebugUnitTest lintDebug
```

The same checks run in CI on every pull request.
