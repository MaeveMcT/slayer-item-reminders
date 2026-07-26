# ADR 0008: Discover task variants from Slayer task Wiki pages

- Status: Superseded by ADR 0010
- Date: 2026-07-23

## Context

A static configuration enum only supports variants compiled into the plugin. RuneLite configuration dropdown values cannot be changed dynamically, so that design cannot automatically include variants added to the OSRS Wiki.

The Wiki's `Slayer task/<task name>` pages contain a structured `Monster variants` wikitext table. Its first column links to monsters that count for the assignment.

## Decision

- Request the current assignment's `Slayer task/<task name>` page through the MediaWiki API after a bank closes. If that page is missing, try deduplicated singular/plural title candidates before falling back (for example, Cave kraken → Cave krakens).
- Parse distinct monster links from the first column of the `Monster variants` section.
- Merge discovered monsters with curated `TaskCatalog` entries, preferring curated entries when names overlap so local required-item rules are retained.
- Populate the existing chatbox selector from the merged assignment-specific list.
- Cache successful discovery results for the RuneLite session and deduplicate concurrent requests.
- Fall back to curated variants or the canonical task target if discovery fails or the page has no variants table.
- Replace the static configuration enum with an assignment-scoped text value. Menu selections populate it, and an advanced user may enter any exact monster Wiki page directly.
- Reset the value when the assignment changes or clears.

## Consequences

- Newly added Wiki table rows become available without a plugin release.
- Variant discovery remains generic and does not require a Java enum entry.
- Required-item rules still require curated local knowledge; Wiki discovery only supplies display names and drop-table page targets.
- Wiki structure changes fail safely but may temporarily leave only curated/default choices.
- Singular/plural fallback handles naming mismatches without maintaining a task-specific alias table, while preserving the exact game-provided title as the first choice.
- The standard RuneLite configuration panel cannot provide a dynamic dropdown. Selection remains a dynamic in-game chatbox menu, while configuration offers a generic Wiki-page override.
