# ADR-0005: Status ownership (technician vs operator)

## Context

The app is for technicians who report what they find. Deciding that an incident is resolved is a
back-office decision, made by an operator who can verify the work.

## Decision

- Technicians create incidents as `OPEN` and can move them to `IN_PROGRESS`.
- Only operators set `RESOLVED`, from the dashboard (`PATCH`). They can also reopen an incident.
- A resolved incident is read-only in the app: no edit and no delete. The rule is enforced in the
  use cases (`UpdateIncidentUseCase`, `DeleteIncidentUseCase`) and on the server (`409`); the UI
  hides the actions and explains why.

## Consequences

- Clear responsibilities, and the rule holds even for changes queued offline.
- A technician who finds a resolved problem again has to report a new incident (or ask an operator
  to reopen it). That matches how maintenance teams usually track recurrences.
