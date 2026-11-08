# AI usage

This project was built with an AI coding agent (Claude Code) as a pair programmer. This log
records where it was used, why, how its output was checked, and what was rejected. Commits it
co-wrote carry a `Co-Authored-By` trailer.

## How I worked with it

- **I own the design.** The agent drafted a plan; I reviewed it over several revisions and asked
  it to review its own drafts adversarially. Each revision fixed concrete defects (listed below)
  before any code was written.
- **Every change goes through a pull request** that I read before merging, with CI running
  formatting, build, unit tests and lint.
- **Versions are verified, not recalled.** Library versions were read from the Maven repositories
  at build time rather than taken from the model's memory, which can be outdated or invented.
- **Nothing private goes in.** No credentials, tokens or personal data were shared with the agent;
  the mock API holds only made-up seed data.

## Log

### Planning and architecture review

- **Used for:** turning the brief into a phased plan, then reviewing that plan for defects.
- **Why:** a second reader catches gaps in an offline sync design cheaply, before code exists.
- **What the reviews caught** (each was a real defect in an earlier draft):
  - an idempotency claim that didn't hold against stock json-server (a retried create would fail
    on a duplicate id), which led to PUT-as-upsert;
  - a race where a sync could overwrite an edit made while it was running, which led to a
    monotonic `localVersion` and a conditional "mark synced" update;
  - two WorkManager unique work names that could run concurrently and let one sync's pull delete
    a row another had just confirmed, which led to a single work name;
  - an undo snackbar driven by a time-based Room query, which can't work because the query
    parameter is fixed at subscription.
- **Rejected:** a suggested GPS capture flow and an adaptive tablet layout, as scope that adds
  little architectural signal for a prototype.

### Build foundation

- **Used for:** scaffolding the `build-logic` convention plugins, the version catalog and CI. A
  Dependabot config was added too, then removed: on a short-lived project its version-bump pull
  requests were noise, and several proposed versions that were pinned on purpose.
- **Checked by:** a toolchain probe before any feature code (a Hilt + Room module, a pure JVM
  module, a Compose module, Robolectric tests, configuration cache), built from a clean checkout.
- **Issues found and fixed during the probe:**
  - Robolectric's Android 17 sandbox requires a Java 21 runtime, so the toolchain moved from 17
    to 21.
  - On JDK 21+, Robolectric needs `--add-exports java.base/jdk.internal.access`; the first fix the
    agent tried (`--add-opens java.io`) was wrong, and the stack trace's root cause showed the
    right flag.
  - Compose test tooling fails on the SDK 37 sandbox (`InputManager.getInstance()` was removed),
    so Robolectric tests pin SDK 36.

### Mock API

- **Used for:** the json-server extensions (PUT upsert, the `409` rule for resolved incidents,
  uploads, validation), the OpenAPI contract and the smoke test.
- **Checked by:** `npm run smoke`, which exercises every endpoint against a temporary copy of the
  seed, including the idempotent repeated PUT and the `409` cases; the contract is linted with
  Redocly. Both run in CI.
- **Decision kept from review:** json-server is pinned to 0.17.4. The 1.x line is a beta rewrite
  without the middleware and lowdb access this server relies on.
- **Seed photos** are placeholder images drawn programmatically (labelled "SAMPLE PHOTO"), so the
  repository contains no third-party or real-world photos.

### Core modules and sync engine

- **Used for:** drafting the domain model, use cases, Room schema, Retrofit API, the sync engine
  and its tests, following the design agreed in planning.
- **Checked by:** integration tests against an in-memory database and a scripted mock server that
  reproduce the race conditions the design is meant to handle (an edit saved while its upload is in
  flight, an undo while the server is deleting), plus one test per sync state transition.
- **Caught while testing:** several first-draft tests failed because the mock server answered
  `GET /incidents` with an empty list right after accepting a `PUT`. The code was right to remove
  rows "deleted on the server"; the tests were fixed to behave like the real server.
- **Changed from the plan:** network errors (no connection) never mark a change as failed; they
  keep it pending and retry, because a technician can stay offline for a long time. Only repeated server
  errors (5xx) reach the attempt cap and become FAILED.
- **Lint caught** that `ACCESS_NETWORK_STATE` was declared in the app rather than in the module that
  needs it; permissions are now declared by the modules that use them.

### Screens, navigation and sync UI

- **Used for:** the design system components, the three feature screens and their ViewModels, the
  Navigation 3 wiring, and ViewModel and Compose UI tests.
- **Checked by:** unit tests per ViewModel, Compose UI tests under Robolectric (run in CI), and a
  manual pass on an emulator against the mock server: report an incident, see it sync, open it,
  delete it, undo, and check a resolved incident is read-only.
- **Caught on the device, not by tests:**
  - a newly reported incident was inserted just above the visible part of the list (a lazy list
    keeps its first visible item in place); the list now follows new items when it is at the top;
  - "0 min. ago" for a fresh report, now "Just now";
  - the "Open" status pill was nearly invisible on cards in dark mode.
- **Caught by tests:** the selected filter chip and the list could disagree for one frame after a
  filter change; the filter and its results now travel in the same emission.
- **A false alarm, investigated:** an Undo tap that seemed to be ignored turned out to land after the
  10-second snackbar timeout (the scripted test was slow); temporary logging confirmed the right
  result reached the ViewModel, and the logs were removed.

### Review pass

- **Used for:** a structured code review of the whole tree (correctness), then a separate
  simplification review (duplication, dead code, rules spread over several places).
- **Checked by:** reading each finding against the code before fixing it; findings that were wrong
  or would have changed agreed behavior were skipped, and the full test suite ran after each fix.
- **What it caught:**
  - a create whose `PUT` reached the server but whose response was lost: skipping the server on
    delete or discard let the next pull bring the incident back. Deletes and discards now always
    ask the server;
  - replacing waiting work reset WorkManager's attempt counter, so the retry cap never triggered.
    The retry count now lives on the incident row;
  - a photo whose file had disappeared was silently dropped from the upload; it now marks the
    change as failed with a clear reason;
  - an edit to the version 1 database schema after it had shipped in a commit; it became a proper
    migration to version 2, with a migration test.
- **Caught on the device after review:** a scroll race when a new incident is added, and a
  "Report incident" button with no TalkBack label.
- **CI:** one Robolectric run failed because Robolectric downloads its Android runtime at test
  time. The runtime is now resolved and cached by Gradle, and the tests run offline.

### Operator dashboard

- **Used for:** the Next.js dashboard (list with filters and counts, incident page with triage),
  its tests and its CI workflow.
- **Versions chosen, not just latest:** TypeScript 5.9 rather than 7 (openapi-typescript supports
  5.x only), ESLint 9 rather than 10 (Next.js's lint config bundles a React plugin that doesn't
  support 10 yet), and jsdom 26 so the tests run on any Node 22. Node 20 is past end of life, so
  the README asks for Node 22.12+.
- **Checked by:** unit and component tests, typecheck, lint and a production build; then against the
  running mock API: the list shows incidents reported from the app, the URL filters and sort work,
  and "Reopen" on a resolved incident changed its status on the server.
- **One contract:** the dashboard's types are generated from `server/openapi.yaml`, and CI fails if
  the committed types drift from it.

### Documentation and demo

- **Used for:** drafting the README and ADRs, and scripting the emulator (adb and UI Automator) to
  capture the screenshots and record the demo the same way each time.
- **Checked by:** reading every document against the code it describes, and watching the recorded
  demo frame by frame before keeping it.

### Review of the merged pull requests

- **Used for:** a second, independent review of every merged pull request, after the history was
  complete, checked against the final code.
- **Checked by:** reading each finding against the code before acting on it. One claim was verified
  in the library itself: WorkManager's `CONNECTED` constraint does require a validated network on
  API 26+, so the app's online check was looser than the sync's.
- **Fixed:** a dashboard filter bar that kept old values after in-app navigation; photo paths and ids
  the server accepted without validation (a path such as `//other-host/x.jpg` made the dashboard load
  a third-party image); `PUT` accepting `RESOLVED` despite the status ownership rule; edits lost
  silently when the incident was deleted while being edited; and several smaller items, each with a
  test where it could regress.
- **Left as is, on purpose:** a delete whose undo snackbar was interrupted waits for the next sync
  instead of a timer (it is delayed, never lost), and the start-of-app sync also runs when
  WorkManager wakes the process (wasteful, not wrong). Both keep the scheduling simple.

## Risks considered

- **Invented or outdated APIs and versions.** Models can recall library APIs that changed or never
  existed. Mitigation: versions are looked up in the Maven and npm registries, everything compiles
  in CI with lint, and unfamiliar APIs were checked against their documentation.
- **Plausible but wrong concurrency code.** Sync bugs don't show up in a happy-path demo.
  Mitigation: the invariants are written down (`CLAUDE.md`, ADRs) and each race has an
  integration test that fails if the rule is broken.
- **Code nobody understands.** Mitigation: small pull requests that I read before merging; anything
  I couldn't explain was simplified or removed.
- **Secrets and personal data.** No credentials or personal data were given to the agent; the seed
  data and photos are made up.
- **Licensing.** Dependencies are well-known open-source libraries pulled through Gradle and npm;
  no code was copied from other projects, and the images are generated for this repository.
