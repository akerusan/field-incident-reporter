# Field Incident Reporter

Field technicians photograph and report physical incidents (broken equipment, road hazards) from an
Android app. Reports are stored on the device first and synced to the API when a connection is
available, so the app works offline.

| Folder | What |
|---|---|
| [`android/`](android/) | Android app: Kotlin, Jetpack Compose, MVVM, Hilt, Room, WorkManager |
| [`server/`](server/) | Local mock API: json-server with photo uploads, and its [OpenAPI contract](server/openapi.yaml) |
| [`docs/`](docs/) | Architecture decisions and the AI usage log |

## Run the mock API

Requirements: Node.js 20 or later.

```bash
cd server
npm install
npm start          # http://localhost:3000, seeded with sample incidents on first start
npm run reset      # restore the seed data and remove uploaded photos
```

The app reaches it at `http://10.0.2.2:3000` from the emulator. For a physical device, either run
`adb reverse tcp:3000 tcp:3000` and set `api.baseUrl=http://localhost:3000/` in
`android/local.properties`, or use your computer's LAN address (`http://192.168.x.x:3000/`).

Two switches help demo the app's loading and error handling:

```bash
DELAY_MS=2000 npm start      # every request takes 2 s
FAIL_STATUS=500 npm start    # every app write (PUT/DELETE) fails with this status
```

(PowerShell: `$env:DELAY_MS=2000; npm start`.)

### API contract

The full contract is [`server/openapi.yaml`](server/openapi.yaml).

| Method | Path | Notes |
|---|---|---|
| `GET` | `/incidents` | Filters: `status`, `severity`, `category`. Sort: `_sort=reportedAt&_order=desc` |
| `GET` | `/incidents/{id}` | |
| `PUT` | `/incidents/{id}` | **Upsert**: creates (201) or replaces (200). Ids are UUIDs generated on the device, so retries are safe |
| `PATCH` | `/incidents/{id}` | Operator triage: `status` (and `updatedAt`) only |
| `DELETE` | `/incidents/{id}` | `404` means already deleted |
| `POST` | `/uploads` | Multipart `file` (JPEG/PNG/WebP, max 10 MB), returns `{ "path": "/uploads/…" }` |

Errors use one shape, `{ "code": "...", "message": "..." }`. Once an operator resolves an incident,
the app can no longer change or delete it: `PUT` and `DELETE` return `409 INCIDENT_RESOLVED`.

## Run the Android app

Requirements: Android Studio (stable) with an emulator or a device on API 26+. The Gradle wrapper
provisions the JDKs it needs.

```bash
cd android
./gradlew installDebug
```

Or open the `android/` folder in Android Studio and run the `app` configuration.

From the command line, Gradle needs the Android SDK location: set `ANDROID_HOME`, or open the
project once in Android Studio, which writes it to `android/local.properties`. On Windows, clone to
a short path or run `git config --global core.longpaths true`, since some source paths are long.

## Checks

```bash
cd android && ./gradlew spotlessCheck assembleDebug testDebugUnitTest lintDebug
cd server  && npm run lint:api && npm run smoke
```

The same checks run in CI on every pull request.
