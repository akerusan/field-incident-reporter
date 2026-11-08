# ADR-0003: PUT as an idempotent upsert for creates and updates

## Context

Mobile requests fail in ambiguous ways: the server may have applied a write whose response never
arrived. The sync must be able to retry any write safely. json-server's default `POST /incidents`
rejects a duplicate id, so a retried create would fail even though the first attempt succeeded.

## Decision

- The app writes with `PUT /incidents/{id}` only. The server creates the incident if the id is new
  (`201`) and replaces it otherwise (`200`). `POST /incidents` is rejected with `405`.
- `DELETE` treats `404` as success on the client, so a retried delete is safe too.
- The contract is written down in `server/openapi.yaml` and checked by `npm run smoke` in CI.

## Consequences

- Every write the sync sends can be repeated with the same result, so the retry logic needs no
  special cases.
- A full replace means the app sends the whole incident each time. Incidents are small, so this is
  cheaper than the complexity of partial updates on the client.
- The operator dashboard uses `PATCH` for triage, which changes only the status (ADR-0005).
