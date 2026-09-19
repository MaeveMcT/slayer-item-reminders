# Slayer Item Reminders documentation

These documents describe the plugin as it exists now. Superseded experiments and transitional decisions are available in Git history rather than kept in the active documentation set.

## Start here

- [Domain model](domain-model.md) — current behavior, lifecycle, invariants, and external dependencies
- [Glossary](glossary.md) — canonical product terminology

## Active decisions

- [ADR 0003: Task synchronization and reminder triggers](decisions/0003-task-and-reminder-triggers.md)
- [ADR 0014: Load required items from centralized Wiki data](decisions/0014-centralized-required-items.md)
- [ADR 0015: Share Wiki request policy without global scheduling](decisions/0015-share-wiki-request-policy.md)
- [ADR 0016: Limit automatic sidebar opening to new assignments](decisions/0016-limit-automatic-panel-opening.md)
- [ADR 0018: Generalize task variant discovery without local alternative lists](decisions/0018-generalize-task-variant-discovery.md)

Decision numbers intentionally retain gaps because removed documents are still available in Git history.

## Research

Research notes capture source investigation rather than current product behavior:

- [Centralized Slayer required-item data](research/slayer-required-item-sources.md)
- [Bosses that count toward broader Slayer assignments](research/slayer-task-boss-variants.md)
