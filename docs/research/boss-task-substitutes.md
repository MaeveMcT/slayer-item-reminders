# Research: directly assigned boss substitutes

## Question

How can directly assigned bosses expose substitutes without a locally maintained boss-pair list?

## Findings

The OSRS Wiki [Boss Slayer page](https://oldschool.runescape.wiki/w/Boss_Slayer), retrieved through its [MediaWiki API](https://oldschool.runescape.wiki/api.php?action=parse&page=Boss_Slayer&prop=wikitext&format=json), has an `Assignable bosses` table. Its first cell identifies the assigned boss through `{{ilinkt|Boss|pic=...}}` and sometimes explicitly identifies a substitute:

- Callisto → Artio: `can be killed instead`.
- Venenatis → Spindel: `may be killed instead`.
- Vet'ion → Calvar'ion: `may be killed instead`.
- Crazy archaeologist → Deranged archaeologist: `can be killed instead`.

The Overview explicitly excludes Deranged archaeologist as a substitute for a Konar assignment. These are directional assignment relationships, not universally interchangeable boss families.

Not every link in the first cell is a variant: the Barrows brothers row also links to Barrows chests to say that chests do **not** count. Other columns contain locations and quest requirements, not substitutes. The `Unassignable bosses` table describes a separate relationship and must not be read as direct boss assignments.

A request for `Slayer task/Callisto` with redirects enabled returned `missingtitle`. Before this change, `WikiTaskVariantClient` searched only `Slayer task/<candidate>` and then monster Bucket `slayer_category`; it did not consult Boss Slayer. `WikiTaskVariantParser` requires a `Monster variants` section and reads ordinary links, not the boss table's primary `ilinkt` template. Supporting this source needs its own parser rather than passing it through that parser unchanged.

`TaskCatalog` already maps the game-facing `Crazy Archaeologists` to Wiki page `Crazy archaeologist`. This normalization is important when joining assignment names to boss-table rows.

## Implemented design

- Add a dedicated Boss Slayer table adapter returning assigned boss → canonical boss plus explicit substitutes. Keep the existing public task-variant lookup interface.
- Read only the `Assignable bosses` table and accept substitute annotations whose semantics are understood (`can/may be killed instead`). Do not collect arbitrary links or introduce hardcoded boss pairs. Unknown markup must not produce guessed relationships.
- Resolve task names using existing canonical Wiki-page mapping and case/underscore normalization, not loose substring matching or an undirected equivalence graph.
- Load the shared table lazily and cache successful results once per session; coalesce concurrent current/stored task requests. Reuse shared HTTP policy and preserve cancellation, reset, and stale-result protection.
- Treat this as a separate source before task-page discovery: a one-variant result must not permanently hide an explicit boss substitute. Explicit boss/substitute groups take precedence over task-page/Bucket results. Boss rows without substitutes continue through ordinary discovery to preserve multi-monster groups.
- Retain canonical fallback on source/network failure; a failed source must not be represented as proof that no substitute exists.
- Keep broad monster assignments on their existing task-page/Bucket discovery path.
- New rows or substitute annotations using the supported schema should work without a plugin release. Changed or newly conditional prose may need parser/policy updates; this is not an arbitrary-prose inference engine.

## Conditional eligibility and game-data investigation

Deranged archaeologist is not valid for a Konar boss assignment. Its variant carries an explicit `Not for Konar assignments` note, rendered in the sidebar. This is a reviewed exception policy, not a hardcoded boss-pair discovery list. Additional conditional rules may require reviewed updates rather than arbitrary-prose inference.

RuneLite exposes `VarbitID.SLAYER_MASTER`, `VarPlayerID.SLAYER_AREA`, and the `SlayerMasterTask` database table (including a `KONAR_BOSS` row). However, the available evidence does not establish that the master field certifies the origin of an assignment restored from task storage. The game's [helper_slayer_area script](https://github.com/runelite/cs2-scripts/blob/master/scripts/%5Bproc%2Chelper_slayer_area%5D.cs2) reads `slayer_area`; [slayer_areaname](https://github.com/runelite/cs2-scripts/blob/master/scripts/%5Bproc%2Cslayer_areaname%5D.cs2) resolves its display name. Neither establishes the origin of the remembered slot, which currently retains only name/count. The built-in Slayer plugin uses `SLAYER_MASTER` for streak selection, not as proof of stored assignment eligibility.

Do not guess a stored assignment's master from the player's latest master field. Conditional candidates remain visible with their warning but do not establish stored-task alert matches. They remain in current-task matching to conservatively suppress a potentially overlapping warning. This favors missed reminders over false warnings and does not promise task credit.

## Regression coverage

- All four explicit substitute pairs from captured source cells.
- A synthetic future boss pair, proving there is no identity-specific allowlist.
- Barrows chest, pet-image parameters, locations, quest links, and unassignable bosses excluded.
- Game-facing `Crazy Archaeologists` joins the canonical table row.
- Konar restriction is visibly qualified and conditional stored-task alert matches are suppressed.
- Existing task-page variants remain intact; a canonical-only result cannot mask boss substitutes.
- Shared fetch, cancellation/reset, retry after failure, and stale current/stored callbacks.
