# ADR-0004: Last-write-wins conflict policy, with one server-enforced exception

## Context

An incident can be changed by its technician (from the app, possibly offline for a while) and by
an operator (from the dashboard). Concurrent changes need a rule.

## Decision

- Last write wins: the server stores the latest `PUT` or `PATCH` it receives. Timestamps are set by
  the client and stored as received.
- Exception: once an operator has resolved an incident, the server rejects the app's `PUT` and
  `DELETE` with `409 INCIDENT_RESOLVED`. Without it, a technician's queued edit (which carries
  status `IN_PROGRESS`) would silently un-resolve the incident.
- The app marks such a change `FAILED` with the reason "Resolved by an operator" and offers
  **Discard my changes**, which fetches the server's copy.

## Consequences

- Simple and predictable for a prototype; the one conflict that would really hurt is handled.
- Other concurrent edits can still overwrite each other. A production system would add optimistic
  concurrency (a version or ETag sent with `If-Match`, `412` on mismatch) and a merge or "keep
  mine / keep theirs" UI.
