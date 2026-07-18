# ADR 0002: Initial task and item catalog

- Status: Accepted
- Date: 2026-07-18

## Context

The plugin needs a catalog connecting Slayer tasks to items worth bringing. Some items are mechanically required for a monster, while others improve loot collection based on the monster's drop table.

## Decision

The MVP has local required-item rules for only two tasks:

- Gargoyles
- Desert lizards

Every non-generic Slayer task that uniquely identifies a canonical monster is eligible for a drop-table lookup and loot-container recommendations. The MVP uses a local task-to-Wiki-page map. Generic tasks that can be completed using materially different monsters, such as Birds, Dogs, or Bears, are omitted from Wiki recommendations.

The catalog can contain two kinds of entries:

- Required items, such as a rock hammer or ice coolers.
- Loot-container recommendations are based on drop-table variety, not probability or expected quantity. Recommend a herb sack when the canonical monster's table contains more than four herb items. Recommend a seed box when it contains more than four seed items.

The MVP fetches drop-table information from the OSRS Wiki by default whenever a mapped non-generic Slayer task is retrieved; this is not configurable. Required-item rules remain local and are evaluated only for the initial two-task pool, so a network failure cannot suppress mechanically necessary items for those tasks.

## Consequences

- Reminder presentation must distinguish required items from recommendations.
- A recommendation can be satisfied by the corresponding container being in inventory or equipment, although these containers will ordinarily be in inventory.
- All functional item variants count as equivalent: regular, open, and cosmetic herb sacks satisfy the herb-sack recommendation; open and closed seed boxes satisfy the seed-box recommendation.
- The MVP treats any positive quantity as satisfying an item requirement, including consumables.
- Only items accepted by the corresponding container count toward the more-than-four threshold.
- Distinct eligible item types are counted; repeated rows for the same item count once.
- For Gargoyles, either a rock hammer or rock thrownhammer satisfies the required-item rule.

## Runtime lookup

It is technically possible to query the OSRS Wiki asynchronously when a task is assigned and cache the derived recommendations under RuneLite's data directory. This option has important consequences:

- The request must use RuneLite's injected `OkHttpClient` and must never block the client thread.
- Requests disclose the player's IP address to the OSRS Wiki. Making the lookup mandatory rather than configurable may create Plugin Hub privacy/review risk and must be verified against current submission policy.
- The plugin needs a stable Wiki API/data source, task-to-monster mapping, and parsing failure behavior.
- Persistent cache design is deferred until after the MVP. A future design should consider stale-while-revalidate behavior, expiration, schema versioning, and offline use.
- The MVP retains successful derived recommendations in memory for the RuneLite session, keyed by canonical monster, and deduplicates concurrent requests for the same monster.
- The parser must identify distinct herb and seed items, including entries represented through nested drop tables.
- The initial reminder may appear before the asynchronous recommendation is available.
- Wiki requests have a 30-second network timeout.
- Timeout, network, HTTP, and parsing failures are silent to the player and logged at debug level; local required-item reminders continue to work.

## Future considerations

- Revisit generic tasks. Consider Wiki-assisted discovery or player selection of an eligible monster variant; the selected variant should determine the drop table.
- Model consumable requirements explicitly and consider the remaining task length when recommending a quantity.
- Consider a user-visible indicator when optional Wiki recommendations are unavailable.
- Add persistent stale-while-revalidate caching with expiration, schema versioning, and offline behavior.
