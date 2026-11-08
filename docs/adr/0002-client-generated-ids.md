# ADR-0002: Client-generated ids

## Context

An incident created offline needs an identity right away: the UI navigates to it, photos are
attached to it, and it may be edited or deleted before it ever reaches the server. Server-assigned
ids would need a temporary local id and a remapping step once the server answers, which is a
classic source of duplicates and broken references.

## Decision

Incidents and photos get a random UUID (version 4) on the device (`IdGenerator`, injected so tests
can predict ids). The server stores the id it is given.

## Consequences

- An incident has one id for its whole life, online or offline.
- Creating is idempotent when combined with `PUT /incidents/{id}` (ADR-0003): a retried create can't
  produce a duplicate.
- The server must trust client ids. For a prototype that's acceptable; a production API would
  validate the UUID format and scope ids per tenant.
