# ADR 0018: Generalize task variant discovery without local alternative lists

- Status: Accepted
- Date: 2026-09-19

## Context

Locally curated alternative monsters make common choices available without the network, but require releases whenever assignments gain variants and cause inconsistent coverage. The assignment-specific Wiki tables already include ordinary monsters and bosses such as Vorkath under one shared concept: monsters that satisfy the assignment.

Some assignments may not have a usable `Slayer task/<task>` page. The Wiki's structured `infobox_monster` Bucket records expose repeated Slayer categories, but also contain duplicate NPC versions and alternate-game-mode pages.

## Decision

- Keep only the canonical monster as the local fallback; do not maintain local lists of alternative monsters or bosses.
- Treat every linked monster in the first cell of every `Monster variants` row as an assignment variant. Do not classify bosses separately.
- If all normalized task-page candidates are missing, or a found page has no parseable variant table, query `infobox_monster` records whose Slayer category matches the normalized task candidates.
- Deduplicate discovered variants by Wiki page and exclude alternate-game-mode Bucket records such as PvM Arena, Deadman, and Echo variants.
- Distinguish successful empty discovery from an unavailable source. Show the unavailable state in the sidebar while retaining the canonical fallback.
- Cache successful discovery for the RuneLite session. Do not cache failures, allowing a later assignment lookup to retry.

## Consequences

- New ordinary and boss alternatives can appear without a plugin release.
- Multi-link cells, including the shared Dagannoth Kings cell, are fully represented.
- Assignments without task pages can still obtain variants from structured Wiki data.
- Network failure degrades to the canonical target and is visible rather than appearing to prove there are no alternatives.
- Bucket filtering is generic but remains dependent on Wiki metadata and naming conventions.
