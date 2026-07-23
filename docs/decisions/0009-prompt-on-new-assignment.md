# ADR 0009: Prompt for a variant on new assignment

- Status: Superseded by ADR 0011
- Date: 2026-07-23

## Context

ADR 0006 delayed variant selection until the player closed a bank to avoid interrupting Slayer-master dialogue. With Wiki-driven discovery, players expect to choose their intended monster as soon as the plugin receives an ambiguous assignment.

Variant discovery is asynchronous and should not delay the canonical required-item and drop-table evaluation that begins for a new assignment.

## Decision

- Start Wiki variant discovery immediately when a genuinely new assignment is detected.
- Continue evaluating the canonical variant in parallel so the network request does not delay initial reminders.
- Open the variant chatbox menu on the next game tick after discovery returns when multiple variants exist.
- Keep startup/login synchronization silent; an assignment already present when the plugin starts must not prompt.
- Retain the bank-close prompt for an ambiguous assignment that remains unresolved.

## Consequences

- A new ambiguous task offers variant selection without requiring a bank visit.
- The picker may replace Slayer-master dialogue if Wiki discovery returns while that dialogue is still open; this is an accepted tradeoff requested for immediate selection.
- Selecting a variant still invalidates any canonical drop-table response through the assignment generation check.
