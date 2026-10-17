# Domain glossary

## Slayer assignment
The current game-provided task: its name, remaining count, and optional location. An assignment begins when a genuinely new task is detected and ends when it is completed, cancelled, or replaced.

## Stored assignment
A Slayer assignment set aside through unlocked task storage. Knowledge of the slot is unknown, confirmed empty, or a remembered assignment; unknown and empty are never interchangeable. The remembered assignment belongs to one RuneScape profile.

## Engagement warning
A reminder that the player is interacting with an attackable monster compatible with their stored assignment but not their current assignment. It does not select an intended variant or guarantee location-specific task credit.

## Task variant
A specific monster that can satisfy a Slayer assignment, subject to any assignment restrictions. A variant has a display name and Wiki page and may have required-item conditions or an eligibility note.

## Boss substitute
A monster explicitly allowed in place of a directly assigned boss. This is a directional relationship, not an interchangeable boss family. A conditional substitute may be offered with an eligibility note without being safe to use for stored-task alerts.

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
Resolving the monsters that satisfy an assignment from the OSRS Wiki. Explicit Boss Slayer substitute groups take precedence for directly assigned bosses. Otherwise, the assignment's Monster variants table is preferred and structured monster Slayer-category data is the fallback. The canonical monster remains available if discovery fails.
