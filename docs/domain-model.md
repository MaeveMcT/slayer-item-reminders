# Slayer Item Reminders Domain Model

## Purpose

Remind a player about mechanically required and optionally useful items when a Slayer task is assigned or when the player finishes banking.

## Core concepts

### Slayer assignment

The current game-provided Slayer task:

- Task name
- Remaining count
- Initial count when available
- Optional location
- Identity/generation used to reject stale asynchronous responses

An assignment can be synchronized silently, newly assigned, updated, completed, cancelled, or replaced.

### Task definition

Local knowledge associated with a task name:

- One or more curated monster variants
- Monster variants discovered from the OSRS Wiki's Slayer task page
- A canonical default variant

Each variant has a display name, OSRS Wiki page, and zero or more required-item conditions. Curated entries take precedence when merging variant identities so local fallback rules are retained. Greater demons initially curates Greater demon, Tormented Demon, K'ril Tsutsaroth, and Skotizo; additional monsters in its Wiki variants table are discovered at runtime.

Required-item knowledge is loaded once per session from the centralized OSRS Wiki `Slayer monsters` table. Its item names are resolved in one batched Wiki Bucket query and expanded through RuneLite's item-variation mapping. Reviewed gameval IDs handle labels absent from Bucket and preserve exact item states where necessary, such as the lit lantern required for Cave horrors. The parser preserves explicit compound rules for monsters whose requirements are not one flat set of alternatives. A complete centralized rule takes precedence over its curated fallback.

The local catalog retains network-independent fallback rules for:

| Task | Requirement | Satisfied by |
| --- | --- | --- |
| Gargoyles | Finishing tool | Rock hammer or rock thrownhammer |
| Lizards (canonical Desert lizard) | Finishing consumable | Any positive quantity of ice coolers |

The local task-to-Wiki-page map identifies canonical monsters where known. Unmapped and generic tasks fall back to using the game-provided task name as the Wiki page title.

### Item condition

A condition has:

- A display name
- One or more equivalent item IDs
- A category: required or optional
- A future-facing consumable classification

A condition is satisfied when any equivalent item has a positive quantity in inventory or equipment. Moving an item between those containers must not briefly mark it missing.

### Drop-table analysis

For the active monster variant, Wiki data is reduced to two sets:

- Distinct herb-sack-compatible herb items
- Distinct seed-box-compatible seed items

Drop probability, dropped quantity, and duplicate rows are ignored.

Derived recommendations:

- Four or more eligible herb item types → Herb sack
- Four or more eligible seed item types → Seed box

Players opt in to each recommendation with **Own herb sack** and **Own seed box** config checkboxes. A recommendation is hidden when the player says they do not own that container. All functional open, closed, and cosmetic container variants satisfy an enabled recommendation.

### Reminder presentation

There are at most two infoboxes:

- **Required:** first missing required item's icon, red missing-item count, tooltip listing missing required items.
- **Optional:** first missing recommendation's icon, yellow missing-item count, tooltip listing missing recommendations.

Both tooltips identify the current Slayer task. Either infobox provides RuneLite's standard **Shift-right-click → Dismiss** menu action, which dismisses both.

The sidebar panel identifies the current assignment and provides a RuneLite-native text filter above a scrollable list of styled variant rows. It first shows curated/default choices, then refreshes with Wiki-discovered variants. Selecting a row immediately updates the assignment-scoped variant. A link opens the active variant's OSRS Wiki page through RuneLite's `LinkBrowser`; automatic mode links to the canonical default variant.

## Lifecycle state

The plugin tracks shared presentation state for the current assignment:

- `SILENT`: assignment known, but no user-visible trigger has occurred; login synchronization never opens the sidebar panel.
- `ACTIVE`: reminders may be shown for currently missing items.
- `BANKING`: both infoboxes are hidden while a regular bank or deposit box is open.
- `DISMISSED`: both infoboxes are hidden after explicit dismissal.
- `TIMED_OUT`: both infoboxes are hidden after the ten-second window.
- `CLEARED`: no active assignment exists.

Satisfied item conditions affect which infoboxes exist; they do not permanently alter the lifecycle state.

## Events and transitions

| Event | Result |
| --- | --- |
| Login with existing task | Synchronize assignment in `SILENT`; show nothing |
| Plugin enabled while already logged in with an existing task | Synchronize assignment in `SILENT`; show nothing |
| New task assignment | Clear old state, enter `ACTIVE`, start the ten-second window, evaluate the canonical variant, and discover variants in parallel; open the sidebar panel when multiple variants are found |
| Ordinary task-count change | Update assignment count without starting a new lifecycle |
| Regular bank/deposit box opens | Enter `BANKING`; hide both infoboxes |
| Bank/deposit box closes | Enter `ACTIVE`, clear dismissal, restart the ten-second window, reevaluate items, and start/reuse the drop-table lookup without opening the sidebar panel |
| Check selected on a Slayer helmet or enchanted gem | Enter `ACTIVE`, clear dismissal, restart the ten-second window, and reevaluate items |
| Inventory/equipment changes while active | Reevaluate both categories |
| Owned-container config changes | Immediately reevaluate optional recommendations |
| Dismiss selected on either infobox | Enter `DISMISSED`; hide both infoboxes |
| Ten-second window expires | Enter `TIMED_OUT`; hide both infoboxes |
| Wiki result returns for current assignment | Store session result and immediately update/show optional infobox, even if the earlier reminder was dismissed |
| Player selects a task variant from the sidebar panel | Invalidate older lookup generations and immediately evaluate the selected variant |
| Wiki result returns for an old assignment or variant | Ignore it |
| Wiki lookup fails or exceeds 30 seconds | Debug-log and leave optional recommendations unavailable |
| Task completed, cancelled, or replaced | Immediately remove both infoboxes; replacement then starts a new assignment lifecycle |

## Invariants

- Curated Gargoyle and Desert lizard required-item reminders never depend on the network; broader required-item coverage degrades safely when the centralized lookup fails.
- No Wiki request blocks the RuneLite client thread.
- At most one required and one optional infobox exist.
- Both infoboxes always refer to the same current assignment.
- An asynchronous response cannot affect a different assignment, completed assignment, or superseded variant selection.
- A manual dismissal normally wins until bank close or a new assignment; a newly returned optional result is the intentional MVP exception.
- The plugin never shows reminders while a supported bank interface is open.
- A task with no missing required items and no enabled derived optional recommendations produces no infobox.

## External boundary

The OSRS Wiki is an asynchronous third-party dependency:

- Use RuneLite's injected `OkHttpClient`.
- Route every Wiki call through one shared request adapter.
- Apply a 30-second timeout, a two-megabyte response limit, and MediaWiki's five-second `maxlag` safeguard.
- Send an identifiable User-Agent.
- Parse a stable API response rather than scraping visual page layout where possible.
- Discover variants from the `Monster variants` section of the assignment's `Slayer task/<task>` Wiki page.
- Fetch the `Slayer monsters` list section once and resolve all referenced item names through one batched Bucket query.
- Keep successful required-item, variant, and derived drop-table results in memory for the current RuneLite session.
- Deduplicate concurrent lookups for the same task or canonical monster.
- Cancel pending drop-table and variant lookups when a different assignment or variant supersedes them.
- Do not persist a cache in the MVP.
- Fail silently to the player and log diagnostics at debug level.

## Deferred work

- Broader curated variant coverage and more robust structured variant sources
- Optional remembered variant preferences
- Grouping or ranking for very large variant lists
- Persistent stale-while-revalidate Wiki cache
- Consumable quantities based on remaining task count
- Group Ironman shared storage as a bank trigger
- User-visible Wiki availability indicator
- Configurable duration, thresholds, networking, colors, and notifications
- Reconsidering whether item loss should override dismissal or timeout
