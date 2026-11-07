# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository

Monorepo for Field Incident Reporter, a prototype where field technicians photograph and report
physical incidents (broken equipment, road hazards) from an Android app.

- `android/`: the Android app (Gradle project root; open this folder in Android Studio)
- `server/`: local mock API (json-server) and its OpenAPI contract
- `web/`: operator dashboard (Next.js)
- `docs/`: ADRs, AI usage log, screenshots

Files listed in `.gitignore` under "Private working notes" are local-only and must never be
committed or quoted in public text (README, docs, commits, PRs).

## Android commands

Run from `android/` (`./gradlew` from Git Bash, `gradlew.bat` from cmd/PowerShell):

```bash
./gradlew spotlessCheck assembleDebug testDebugUnitTest lintDebug   # what CI runs
./gradlew spotlessApply                                               # format Kotlin + Gradle scripts
./gradlew :core:data:testDebugUnitTest                                # one module's tests
./gradlew :core:data:testDebugUnitTest --tests "*SyncWorkerTest"      # one test class
./gradlew :core:domain:test                                           # JVM-only modules use `test`
```

## Build setup

- AGP 9.5 with **built-in Kotlin**: there is no `org.jetbrains.kotlin.android` plugin. Pure JVM
  modules use `org.jetbrains.kotlin.jvm`.
- Every module applies a convention plugin from `android/build-logic/convention`
  (`fir.android.application`, `fir.android.library`, `fir.android.library.compose`,
  `fir.android.feature`, `fir.android.hilt`, `fir.android.room`, `fir.jvm.library`). SDK levels and
  the JVM toolchain come from `gradle/libs.versions.toml` (`compileSdk`, `minSdk`, `jvmToolchain`).
  Change shared build config there, not in module scripts.
- JVM toolchain 21 (Robolectric needs it); the Gradle daemon runs on JDK 25
  (`gradle/gradle-daemon-jvm.properties`). foojay provisions missing JDKs.
- Robolectric tests pin `sdk=36` in `src/test/resources/robolectric.properties` (Compose test
  tooling doesn't run on the SDK 37 sandbox yet).
- Configuration cache and build cache are on; build logic must stay configuration-cache safe.
- Type-safe project accessors: depend on modules as `projects.core.domain`.
- The mock API base URL is a `BuildConfig` field in `:core:network`, default
  `http://10.0.2.2:3000/`, overridable with `api.baseUrl=` in `android/local.properties`.
  Cleartext HTTP is allowed in debug builds only (`app/src/debug`).

## Module architecture

Dependency rule: `feature → domain + model + designsystem`. Features never depend on `data`,
`database` or `network`; ViewModels call use cases only. `:app` wires implementations with Hilt.

- `:core:model` (JVM): domain models, one type per file
- `:core:domain` (JVM): repository/service interfaces and use cases
- `:core:database`: Room entities and DAOs (schema exported to `schemas/`)
- `:core:network`: Retrofit API and DTOs
- `:core:data`: offline-first repository, mappers, `SyncWorker`, photo storage
- `:core:designsystem`: theme and shared Compose components
- `:core:testing` (JVM): fakes (`FakeIncidentRepository`, `FakeClock`, ...), `MainDispatcherRule`, test data builders
- `:feature:incidentlist`, `:feature:incidentdetail`, `:feature:incidenteditor`

Room is the single source of truth; the UI observes Room flows and a single WorkManager unique
work (`sync`) pushes pending local changes, then pulls from the server. See `docs/adr/` for the
reasoning behind the sync design.

Sync invariants (break these and the integration tests in `core/data` should fail):
- Sync state changes go through the pure functions in `core/data/.../sync/SyncTransitions.kt`.
- `localVersion` only grows, and only on user writes; sync-side writes never bump it. Server
  confirmations are conditional on the version that was sent (`IncidentDao.markSynced`,
  `deleteIfVersion`, `markFailed`).
- The pull never overwrites PENDING or FAILED rows; rows inserted by the pull get version 1/1.
- One unique work name only; `SyncWorker` returns success or retry, never failure.
- Data tests use `DataTestEnvironment` (in-memory Room + MockWebServer). A mocked `GET /incidents`
  must include rows the test expects to stay synced, or the pull removes them.
