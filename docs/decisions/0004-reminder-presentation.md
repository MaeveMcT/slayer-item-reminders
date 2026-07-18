# ADR 0004: Separate required and optional reminder infoboxes

- Status: Accepted
- Date: 2026-07-18

## Context

A task can have both mechanically required items and optional loot-container recommendations. Combining them in one infobox makes an optional suggestion look as urgent as a mechanical requirement.

## Decision

- Display at most two aggregate RuneLite infoboxes for the current task:
  - One infobox listing missing required items.
  - One infobox listing missing optional recommendations, such as a herb sack or seed box.
- Show each category's missing-item details in its infobox tooltip.
- Add a standard RuneLite infobox menu entry: **Dismiss**.
- Dismissing either infobox dismisses both required and optional reminders until the next bank closes or another lifecycle reset occurs.
- The required infobox uses the first missing required item's icon and red count text.
- The optional infobox uses the first missing recommended item's icon and yellow count text.
- Each count is the number of distinct missing items in that category.
- Each tooltip names the current task and lists every missing item in its category.

## Consequences

- The reminder occupies no more than two infobox slots regardless of item count.
- Required items and optional recommendations have visibly different urgency.
- Individual items are visible on hover rather than continuously.
- Each category needs a stable icon and concise text/color semantics.


