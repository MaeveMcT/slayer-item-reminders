# Research: bosses that count toward broader Slayer assignments

Date: 2026-09-19

## Question

Besides Vorkath for blue dragons, the King Black Dragon for black dragons, and K'ril Tsutsaroth for greater demons, which bosses appear as variants of broader Slayer assignments?

## Finding

The OSRS Wiki's current `Slayer task/Blue dragons` source already lists **Baby blue dragon**, **Blue dragon**, **Brutal blue dragon**, and **Vorkath** as separate rows in its `Monster variants` table. The plugin's generic variant parser is intended to discover all four. If only the locally curated Blue dragon fallback appears, the missing Baby blue dragon and Vorkath are not omissions in the current Wiki table; variant discovery did not populate the panel for that session.

The same Wiki tables identify the following repeatable bosses as broader-assignment variants:

| Assignment | Boss variants |
| --- | --- |
| Abyssal demons | Abyssal Sire |
| Araxytes | Araxxor |
| Aviansies | Kree'arra |
| Bears | Artio, Callisto |
| Black demons | Skotizo |
| Black dragons | King Black Dragon |
| Blue dragons | Vorkath |
| Cave krakens | Kraken |
| Dagannoth | Dagannoth Prime, Rex, and Supreme |
| Gargoyles | Dawn and Dusk (the Grotesque Guardians encounter) |
| Greater demons | Skotizo and K'ril Tsutsaroth |
| Hellhounds | Cerberus |
| Hill giants | Obor |
| Kalphites | Kalphite Queen |
| Moss giants | Bryophyta |
| Rats | Scurrius |
| Scorpions | Scorpia |
| Skeletons | Calvar'ion and Vet'ion |
| Smoke devils | Thermonuclear smoke devil |
| Spiders | Araxxor, Sarachnis, Spindel, and Venenatis |
| TzHaar | TzTok-Jad and TzKal-Zuk |
| Zombies | Vorkath |

The tables also contain one-off quest bosses such as Giant Scarab for Scabarites and Dad, Arrg, and the Ice Troll King for Trolls. These are technically variants in the source but are not useful repeatable targets, so they should not be prioritized as selectable farming variants.

## Implication for this plugin

Before ADR 0018, K'ril worked without a successful Wiki lookup because `TaskCatalog` locally curated Greater demon variants, while Blue dragons and almost all other assignments depended on runtime discovery. Since the current Blue dragon Wiki source contains both missing choices and the parser accepts their ordinary one-link rows, observing only the canonical choice points to discovery not completing successfully for that session rather than missing source data. ADR 0018 removes that inconsistent local alternative list.

The assignment page's `Monster variants` table is the best generic source because Wiki editors have already filtered it to useful assignment alternatives. The parser extracts **every** link from the first cell of each row rather than classifying bosses or maintaining a local boss list. Most rows have one monster, but the Dagannoth Kings share one first cell containing three links; parsing the complete cell captures Prime, Rex, and Supreme.

A second structured source exists: the Wiki Bucket `infobox_monster` schema publishes repeated `slayer_category` values and implicit `page_name` metadata. For example, querying `slayer_category = Blue dragons` returns Blue dragon, Baby blue dragon, Brutal blue dragon, and Vorkath. However, it also returns duplicate NPC versions and nonstandard pages such as PvM Arena, Deadman, and Echo variants. Bucket is therefore useful as a fallback or validation source, but replacing the assignment table with it would require generic game-mode filtering and could expose choices that Wiki editors intentionally omit from the task guide.

## Sources

Primary data source: the OSRS Wiki's assignment-specific `Monster variants` tables, retrieved through its MediaWiki API on 2026-09-19.

- [Blue dragons, revision 15319394](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Blue_dragons&oldid=15319394)
- [Black dragons, revision 15341652](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Black_dragons&oldid=15341652)
- [Greater demons, revision 15270275](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Greater_demons&oldid=15270275)
- [Abyssal demons, revision 15347479](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Abyssal_demons&oldid=15347479)
- [Araxytes, revision 15322536](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Araxytes&oldid=15322536)
- [Aviansie, revision 15299787](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Aviansie&oldid=15299787)
- [Bears, revision 15250961](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Bears&oldid=15250961)
- [Cave krakens, revision 15332650](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Cave_krakens&oldid=15332650)
- [Dagannoth, revision 15276899](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Dagannoth&oldid=15276899)
- [Gargoyles, revision 15316243](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Gargoyles&oldid=15316243)
- [Hellhounds, revision 15350164](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Hellhounds&oldid=15350164)
- [Hill giants, revision 15272947](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Hill_giants&oldid=15272947)
- [Kalphites, revision 15344284](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Kalphites&oldid=15344284)
- [Moss giants, revision 15250972](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Moss_giants&oldid=15250972)
- [Rats, revision 15251205](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Rats&oldid=15251205)
- [Scorpions, revision 15316864](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Scorpions&oldid=15316864)
- [Skeletons, revision 15346469](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Skeletons&oldid=15346469)
- [Smoke devils, revision 15341653](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Smoke_devils&oldid=15341653)
- [Spiders, revision 15250979](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Spiders&oldid=15250979)
- [TzHaar, revision 15316995](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/TzHaar&oldid=15316995)
- [Zombies, revision 15327782](https://oldschool.runescape.wiki/w/index.php?title=Slayer_task/Zombies&oldid=15327782)
