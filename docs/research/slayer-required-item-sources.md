# Research: centralized Slayer required-item data

Date: 2026-09-16

## Question

Can Slayer monster requirements be derived generically without requesting every individual monster page?

## Finding

Yes, but the Wiki does not currently expose a single fully structured Slayer-requirement API. The best current source is the centralized **Slayer monsters** table, optionally supplemented by the centralized **Slayer task** table. Item names can be resolved to game item IDs in one additional structured Bucket API query.

A safe implementation should fetch a central table once per session (or generate a reviewed catalog at build time), rather than scrape every monster page. It should not infer complex AND/OR requirement semantics by flattening every item link in a cell.

## Centralized sources

### 1. Slayer monsters

Source: [Slayer monsters](https://oldschool.runescape.wiki/w/Slayer_monsters), revision [15345596](https://oldschool.runescape.wiki/w/index.php?title=Slayer_monsters&oldid=15345596).

The page has one central table with columns for the monster and **Required items**. Its source is approximately 29 KB; requesting only section 1 as wikitext is approximately 24 KB. This is far smaller than requesting rendered individual monster pages, which were roughly 300–360 KB in sampled cases.

The table already captures useful alternatives, including:

- Banshee: earmuffs or Slayer helmet
- Rockslug: bag of salt or brine sabre
- Basilisk: mirror shield or V's shield
- Gargoyle: rock hammer, rock thrownhammer, or granite hammer
- Turoth/Kurask: leaf-bladed weapons, Magic Dart, broad arrows, or broad bolts

Important limitations stated by the page itself:

- It covers Slayer-level monsters, not every broad assignment or every alternative that can count for a task.
- It directs readers to **Slayer task** for assignment alternatives.
- Access requirements such as light sources and Karuulm footwear are explicitly excluded from the Required items column and instead appear in location footnotes.

The table also contains compound requirements that cannot safely be interpreted as one flat alternative set. For example, a Warped Terrorbird needs a crystal chime **and** either earmuffs or a Slayer helmet. Cave horror and variant/location-dependent rows have similar contextual requirements.

### 2. Slayer task

Source: [Slayer task](https://oldschool.runescape.wiki/w/Slayer_task), revision [15343835](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task&oldid=15343835).

The central assignment table is approximately 44 KB and has a **Required Items** column keyed by assignment. It covers broad tasks and conditions omitted from Slayer monsters, for example:

- Lizards: ice cooler for Desert lizards; Karuulm footwear for Sulphur lizards
- Cave horrors: witchwood icon plus a light source alternative
- Dragon assignments: fire protection except for baby dragons
- Warped creatures: crystal chime plus earmuffs/Slayer helmet for Terrorbirds
- Location/access items such as light sources, boots, spades, and lockpicks

Its list indentation often carries useful grouping semantics: top-level bullets can represent separate requirements while nested bullets represent alternatives. Parenthetical text identifies variant-specific conditions. However, the table currently carries a Wiki cleanup notice, so its exact formatting should not be treated as a stable schema.

### 3. Slayer equipment

Source: [Slayer equipment](https://oldschool.runescape.wiki/w/Slayer_equipment), revision [15313141](https://oldschool.runescape.wiki/w/index.php?title=Slayer_equipment&oldid=15313141).

This is a small central page (approximately 9.5 KB) listing equipment and prose descriptions of its uses. It is useful as a validation source, but less suitable as the primary machine-readable mapping because it mixes required, protective, optional, convenience, and crafting items and does not directly key every entry to an assignment.

## Resolving Wiki item names to IDs

The OSRS Wiki runs its first-party [Bucket extension](https://github.com/weirdgloop/mediawiki-extensions-Bucket), which exposes structured data through [`api.php?action=bucket`](https://meta.weirdgloop.org/w/Extension:Bucket/Api).

The `infobox_item` bucket schema is published at [Bucket:Infobox item](https://oldschool.runescape.wiki/w/Bucket:Infobox_item?action=raw). It includes `item_name` and repeated `item_id` fields. Multiple required item names can be resolved in one query using `bucket.Or`, for example:

```lua
bucket('infobox_item')
  .select('page_name', 'item_name', 'item_id')
  .where(bucket.Or(
    {'item_name', 'Ice cooler'},
    {'item_name', 'Slayer helmet'},
    {'item_name', 'Crystal chime'},
    {'item_name', 'Rock thrownhammer'}
  ))
  .limit(100)
  .run()
```

The live query returned IDs for all four items in one request, including untradeable items absent from the Wiki price mapping endpoint.

Caveat: Bucket's own documentation says its API/schema are unstable and third-party production usage is not yet recommended. The API implementation also applies a request limiter and marks responses publicly cacheable. Any use should therefore retain the plugin's identifiable User-Agent, `maxlag`, session cache, and conservative request count.

The Wiki currently has no dedicated Slayer-requirement Bucket. The published Bucket schemas include monster/item infoboxes and recommended equipment, but nothing that directly models required Slayer items and their AND/OR/variant conditions.

## RuneLite/game data

RuneLite's `DBTableID.SlayerTask` exposes task identity, names, level requirements, unlocks, and related content, but no required-item field. `ItemManager.search` searches tradeable price data, so it cannot reliably resolve untradeable requirements such as Slayer helmets, ice coolers, and crystal chimes. The game task table therefore cannot replace the Wiki mapping, and RuneLite's item search cannot replace either a local item registry or the Wiki Bucket lookup.

Sources:

- [`DBTableID.SlayerTask`](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/gameval/DBTableID.java)
- [`ItemManager.search`](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/game/ItemManager.java)

## Recommended design

### Near term: one central fetch plus conservative parsing

1. Fetch `Slayer monsters`, section 1, as wikitext once per RuneLite session.
2. Parse monster names and Required items cells into an explicit model of requirement groups:
   - requirements in a group are alternatives (OR);
   - multiple groups must all be satisfied (AND);
   - retain variant/location qualifiers rather than discarding prose.
3. Accept only table shapes whose semantics are understood. Skip ambiguous rows instead of producing a misleading reminder.
4. Resolve known item names through a local registry backed by RuneLite `ItemID` constants.
5. Optionally make one batched Bucket query for names absent from that registry. Validate IDs and cache the result for the session.
6. Keep a small curated override layer for compound and conditional cases such as Warped Terrorbirds, lizards, cave horrors, and diary-based access exceptions.

This changes runtime load from one required-item request per monster to one approximately 24 KB central request per session, plus at most one batched ID query. Existing per-assignment requests for variant discovery and optional drop-table recommendations are separate concerns.

### Stronger coverage: supplement with Slayer task

Fetch the central Slayer task table once when assignment-level/access requirements are desired. Merge it with monster-level data using explicit precedence:

1. Curated local override
2. Qualified Slayer task rule matching the active variant
3. Slayer monsters rule matching the active variant
4. No required-item rule

Never union all requirements for a broad task, because that would combine mutually exclusive variant requirements.

### Lowest runtime load: build-time generation

A generator can fetch the two central pages and Bucket IDs during development, emit a reviewed local catalog, and make runtime required-item checks network-free. This best protects public infrastructure and keeps required reminders available offline, but updates require a plugin release and generated numeric IDs should be translated to RuneLite `ItemID` constants where available.

### Best long-term source

Ask OSRS Wiki editors whether a dedicated structured Bucket is appropriate, with fields such as:

- assignment/monster page
- variant qualifier
- requirement group
- acceptable item page/IDs
- requirement type (combat, finishing, protection, access)
- notes/exception

That would remove fragile wikitext parsing and allow one small structured query. Until such a schema exists, the centralized tables plus conservative local semantics are safer than individual-page scraping.
