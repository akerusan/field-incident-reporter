# ADR-0001: Offline-first, with Room as the single source of truth

## Context

Technicians report incidents on site: basements, roofs, plant rooms and roadsides, where coverage
is often weak or absent. A report that fails because the network dropped is a report that may never
be made. The brief also asks for clear asynchronous state handling.

## Decision

- Every write goes to the local Room database first, in one transaction, and returns immediately.
  The UI only ever reads Room through `Flow`s, so it updates the same way after a local write or a
  sync.
- A single `SyncWorker` (WorkManager) sends pending changes, then pulls the server's list. It runs
  when a network is available, survives process death and retries with backoff.
- Each row carries its sync state (`SYNCED`, `PENDING`, `FAILED`) and the pending operation
  (`CREATE`, `UPDATE`, `DELETE`). The transitions are pure functions
  (`core/data/.../sync/SyncTransitions.kt`), tested row by row.
- A monotonic `localVersion` per row, compared with the version the sync sent, decides whether a
  server confirmation still applies (`IncidentDao.markSynced`). An edit made while a sync is in
  flight therefore stays pending instead of being marked synced by mistake.
- All sync work runs under **one** unique work name, chained with `APPEND_OR_REPLACE`. Two names
  would let two push-then-pull runs overlap, and one run's pull could delete a row the other had
  just created.

## Consequences

- The app is fully usable offline; the UI shows pending and failed states per incident and an
  offline banner.
- Sync logic is more involved than calling the API from the ViewModel. It lives in one class
  (`IncidentSynchronizer`), with integration tests against an in-memory database and a mock server,
  including the race conditions above.
- The pull never overwrites rows with unsent changes, so the user's work is never lost to a refresh.
