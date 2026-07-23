# ADR 0006: Explicit selection for ambiguous Slayer tasks

- Status: Superseded by ADR 0011
- Date: 2026-07-19

## Context

A Slayer assignment can permit monsters with materially different drop tables. The game supplies only the broad assignment, so the plugin cannot reliably infer the player's intended monster before they leave the bank. Combining every variant's drops would produce excessive recommendations.

## Decision

- A task definition may contain one or more curated monster variants.
- Each variant owns its display name, OSRS Wiki page, and required-item rules.
- The first variant is the ordinary canonical default.
- A newly assigned ambiguous task initially uses the canonical variant so assignment reminders are not blocked by a prompt during Slayer-master dialogue.
- When the player next closes a bank or deposit box, present a RuneLite chatbox menu if no variant has been selected for that assignment.
- Selecting a variant immediately invalidates older Wiki results and evaluates the selected variant.
- The selection lasts for the current assignment and clears when that assignment ends or is replaced.
- Closing the menu without selecting leaves the variant unresolved and prompts again after the next bank close.
- Do not persist a preferred variant in the MVP.

The initial curated ambiguous task is **Greater demons**:

- Greater demon (default)
- Tormented Demon
- K'ril Tsutsaroth
- Skotizo

## Consequences

- Recommendations reflect player intent instead of a union of unrelated drop tables.
- Bank close can temporarily open a local chatbox menu for ambiguous tasks.
- Async Wiki callbacks require assignment/selection generation checks so old variants cannot update current reminders.
- Variant lists must be maintained locally.

## Future considerations

- Add curated variants for other ambiguous tasks.
- Remember the last choice per task as an optional default.
- Learn from recently fought task NPCs without overriding an explicit selection.
- Discover candidate variants from structured Wiki data if a reliable source becomes available.
