# Domain glossary

## Slayer assignment
The current game-provided task: its name, remaining count, and optional location. An assignment begins when a genuinely new task is detected and ends when it is completed, cancelled, or replaced.

## Task variant
A specific monster that can satisfy a broader Slayer assignment. A variant has a display name and Wiki page and may have required-item conditions.

## Canonical monster
The default task variant used when the player has not explicitly selected another variant or variant discovery is unavailable.

## Broad assignment
A Slayer assignment that can be completed by materially different monsters, such as Blue dragons or Greater demons. The player can select the intended variant in the sidebar.

## Item condition
One or more equivalent items that satisfy one requirement. A condition is satisfied while any equivalent item has a positive quantity in inventory or equipment.

## Required item
An item mechanically needed to finish, protect against, or fight the selected task variant.

## Optional recommendation
An item that improves task convenience or loot collection but is not mechanically necessary.

## Loot-container recommendation
An optional herb sack or seed box recommendation derived from the selected variant's drop table. A recommendation is shown only when its ownership setting is enabled and the container is absent.

## Reminder window
The ten-second period after a new assignment, bank close, or explicit task check during which missing-item infoboxes may be shown.

## Dismissal
The player intentionally hides both reminder infoboxes through either infobox's menu. Dismissal lasts until the next bank close or new assignment, except that a newly returned optional Wiki result may appear while an earlier reminder is dismissed.

## Bank visit
The period from opening through closing a regular bank interface or deposit box. Infoboxes remain hidden while the interface is open; closing it resets dismissal and starts a new reminder window.

## Silent synchronization
Learning an existing assignment during plugin startup, login, hopping, or reconnection without opening the sidebar or starting a reminder window.

## Variant discovery
Resolving the monsters that satisfy an assignment from the OSRS Wiki. The assignment's Monster variants table is preferred, structured monster Slayer-category data is the fallback, and the canonical monster remains available if discovery fails.
