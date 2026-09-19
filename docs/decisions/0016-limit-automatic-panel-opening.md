# ADR 0016: Limit automatic sidebar opening to new assignments

- Status: Accepted
- Date: 2026-09-19
- Amends: ADR 0011

## Context

The sidebar is useful when a newly assigned task has multiple monster variants, but opening it during login synchronization or after an ordinary bank visit interrupts the player. Login task data can also arrive over several events, after an initial silent synchronization attempt, and a variant request started before a login transition can complete afterward.

## Decision

- Automatically open the Slayer Item Reminders sidebar only when variant discovery finds multiple choices for a genuinely new assignment.
- Never automatically open it while synchronizing an existing assignment at login, including task data that arrives after the `LOGGED_IN` event.
- Do not automatically reopen it after a bank closes or after a task check.
- Invalidate automatic-open intent from variant requests started before a login, hop, or reconnect transition.
- Keep the navigation button available so the player can open the sidebar manually at any time.

## Consequences

- Login, hopping, reconnecting, bank visits, and task checks do not steal sidebar focus.
- A new ambiguous assignment still opens the variant selector once discovery completes.
- An unresolved assignment remains unresolved after banking unless the player opens the sidebar manually.
