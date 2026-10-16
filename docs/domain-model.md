# Slayer Item Reminders Domain Model

## Purpose

Remind a player about mechanically required and optionally useful items at the moments they prepare for a Slayer assignment, and warn when they engage a monster compatible with a stored assignment instead of their current assignment, without becoming a general combat or gear-planning assistant.

## Product goals

- Surface short, actionable reminders after a new assignment, bank visit, or explicit task check.
- Keep required items visually distinct from optional loot-container recommendations.
- Let the player choose the monster they intend to fight for a broad assignment.
- Discover variant and item knowledge generically from centralized Wiki sources rather than maintaining local monster lists.
- Degrade safely: preserve reviewed required-item fallbacks, ignore stale responses, and expose unavailable variant discovery without interrupting gameplay.
- Avoid stealing sidebar focus during login synchronization, banking, hopping, reconnecting, or task checks.

## Non-goals

- Recommending full equipment or inventory setups
- Predicting combat mechanics or providing encounter assistance
- Inferring the player's intended variant from nearby or recently fought NPCs
- Persisting Wiki data or preferred variants between RuneLite sessions
- Treating banks or storage interfaces beyond regular banks and deposit boxes as reminder triggers

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

Each variant has a display name, OSRS Wiki page, and zero or more required-item conditions. The assignment's canonical monster is retained as a local fallback, while alternatives are discovered at runtime. The assignment-specific Wiki `Monster variants` table is authoritative when available; if no task page or parseable table exists, structured Wiki Bucket monster records matching the assignment's Slayer category provide a fallback. Alternate-game-mode records are excluded and duplicate pages are collapsed.

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

The **Own herb sack** and **Own seed box** config checkboxes are enabled by default. A player's disabled choice persists through RuneLite configuration, and its recommendation remains hidden until re-enabled. All functional open, closed, and cosmetic container variants satisfy an enabled recommendation.

### Reminder presentation

There are at most two missing-item infoboxes:

- **Required:** first missing required item's icon, red missing-item count, tooltip listing missing required items.
- **Optional:** first missing recommendation's icon, yellow missing-item count, tooltip listing missing recommendations.

Both tooltips identify the current Slayer task. Either infobox provides RuneLite's standard **Shift-right-click → Dismiss** menu action, which dismisses both.

The sidebar panel identifies the current assignment and provides a RuneLite-native text filter above a scrollable list of styled variant rows. It first shows the canonical fallback, then refreshes with Wiki-discovered variants. It distinguishes loading, loaded, and unavailable discovery states. Selecting a row immediately updates the assignment-scoped variant. A link opens the active variant's OSRS Wiki page through RuneLite's `LinkBrowser`; automatic mode links to the canonical default variant.

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
| Check selected on a Slayer helmet, ring, or enchanted gem | Enter `ACTIVE`, clear dismissal, restart the ten-second window, and reevaluate items |
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
- A manual dismissal normally wins until bank close or a new assignment; a newly returned optional result is the intentional exception.
- The plugin never shows reminders while a supported bank interface is open.
- A task with no missing required items and no enabled derived optional recommendations produces no missing-item infobox.

## External boundary

The OSRS Wiki is an asynchronous third-party dependency:

- Use RuneLite's injected `OkHttpClient`.
- Route every Wiki call through one shared request adapter.
- Apply a 30-second timeout, a two-megabyte response limit, and MediaWiki's five-second `maxlag` safeguard.
- Send an identifiable User-Agent.
- Parse a stable API response rather than scraping visual page layout where possible.
- Discover variants from every monster link in the first column of the assignment's `Slayer task/<task>` Wiki page's `Monster variants` section.
- When no task page or parseable variant table exists, query the Wiki Bucket `infobox_monster` Slayer category using normalized singular/plural task candidates.
- Fetch the `Slayer monsters` list section once and resolve all referenced item names through one batched Bucket query.
- Keep successful required-item, variant, and derived drop-table results in memory for the current RuneLite session.
- Deduplicate concurrent lookups for the same task or canonical monster.
- Cancel pending drop-table and variant lookups when a different assignment or variant supersedes them.
- Do not persist Wiki caches between RuneLite sessions.
- Fail silently to the player and log diagnostics at debug level.

## Stored assignments and engagement warnings

Storage has three distinct states: unknown, confirmed empty, or a known assignment. Unlocking task storage does not reveal its contents. The initialized, visible Slayer Rewards Tasks tab exposes the stored count/category/boss through temporary interface varps (`IF4`, `IF5`, `IF6`), as used by RuneLite's [decompiled script9736](https://github.com/runelite/cs2-scripts/blob/master/scripts/%5Bproc%2Cscript9736%5D.cs2). Never interpret those generic varps outside that interface. Resolve assignment names through the same game database mapping as current assignments. The game's challenge count of `-1` means a task with no numeric remaining count, not an empty slot.

Persist observed storage through RuneLite's RuneScape-profile configuration. Restore it at login, clear in-memory knowledge on logout/hop/reconnect, and reject asynchronous results from superseded observations. Unknown storage remains unknown until observed. Previously remembered storage may be stale if changed with this plugin disabled or in another client; opening Tasks refreshes it. Current assignments remain game-synchronized rather than inferred from the remembered slot.

The built-in Slayer counter is identified through its public infobox name (`SlayerPlugin_TaskCounter`), since its class and plugin accessor are not public. Append storage to its tooltip and restore our suffix on shutdown. Reapply after Slayer rebuilds the tooltip. Storage is presented through that tooltip and a sidebar label, which remains visible without a current assignment. No dedicated stored-task counter or counter setting exists. Check on a Slayer helm, ring, or enchanted gem continues to trigger missing-item reminders only.

Reuse the shared session-cached Wiki variant discovery for stored assignments. Cancellation preserves both the current and stored task requests. Normalize exact NPC names, Wiki display names, page disambiguation, and canonical task singular/plural forms; do not use broad substring matching. Local-player interaction with an attackable matching NPC is an engagement, not a nearby-NPC or scene-scan trigger.

Suppress warnings for identical assignments, an NPC compatible with both assignments, unknown storage, locked storage, another player's engagement, or unavailable/loading current-task discovery. With no current task, a stored-task match can warn. Game messages and RuneLite notifications are enabled by default and independently configurable. Apply a shared 100-game-tick cooldown (about one minute), including engagements with different NPCs. Compatibility is monster-based and deliberately does not promise location-specific task credit.

## Deferred work

- More robust structured variant filtering and sources
- Optional remembered variant preferences
- Grouping or ranking for very large variant lists
- Persistent stale-while-revalidate Wiki cache
- Consumable quantities based on remaining task count
- Group Ironman shared storage as a bank trigger
- User-visible Wiki availability indicator
- Configurable item-reminder duration, thresholds, networking, colors, and notifications
- Reconsidering whether item loss should override dismissal or timeout
