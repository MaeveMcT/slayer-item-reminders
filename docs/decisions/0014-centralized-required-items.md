# ADR 0014: Load required items from centralized Wiki data

- Status: Accepted
- Date: 2026-09-16
- Amends: ADR 0002

## Context

The local catalog covered required items only for Gargoyles and Desert lizards. Requesting every monster page would increase load on public Wiki infrastructure and still require fragile prose interpretation.

The OSRS Wiki's `Slayer monsters` page contains one centralized table mapping Slayer monsters to required items. Its list-section wikitext is approximately 24 KB. The Wiki's Bucket interface can resolve all referenced item names to IDs in one batched query, including untradeable items.

The table is not a fully structured requirement schema. Most required-item cells are one set of alternatives, while a few monsters have multiple simultaneous requirements or misleading refill/access entries.

## Decision

- Fetch section 1 of `Slayer monsters` as wikitext once per RuneLite session, on the first active assignment.
- Parse its table into monster-scoped requirement groups while carrying rowspans to grouped monster variants.
- Treat ordinary item lists as alternatives within one condition.
- Preserve explicit compound handling for known non-flat cases, initially Warped Terrorbirds, Cave horrors, and mutated/ancient zygomites.
- Resolve every distinct item name in one `action=bucket` query against `infobox_item`, with reviewed local gameval IDs for table labels that have no matching Bucket `item_name` (initially `Bullseye lantern (lit)`).
- Expand Bucket-resolved IDs through RuneLite's `ItemVariationMapping` so recognized charged, imbued, and cosmetic variants satisfy a condition. Keep exact local IDs exact when nearby item states are not equivalent, such as lit versus unlit lanterns.
- Reject an entire monster rule if any required group cannot be resolved.
- Prefer a complete centralized rule over the local rule. Retain local Gargoyle and Desert lizard rules as offline/failure fallbacks.
- Apply the existing timeout, `maxlag`, identifiable User-Agent, session cache, and shutdown cancellation policies to both requests.

## Consequences

- Required-item coverage expands without one request per monster page.
- The first assignment in a session adds at most two small centralized requests: one table and one batched ID query.
- Required reminders can update after the asynchronous catalog arrives.
- Existing local reminders still work if the Wiki is unavailable.
- Complex new table rows may require a parser policy update. Ambiguous or partially resolved compound rules fail closed instead of showing an incomplete requirement.
- Access requirements deliberately omitted by `Slayer monsters` remain outside this source. The centralized `Slayer task` table can be considered separately.
- Bucket is documented as unstable, so its adapter remains isolated behind the required-item catalog module.
