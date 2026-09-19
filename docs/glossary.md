# Domain Glossary

## Relevant Slayer task
A Slayer task for which the plugin knows about one or more items a player should bring.

## Task variant
A specific monster that can satisfy a broader Slayer assignment. Each curated variant has its own display name, Wiki page, and required-item rules.

## Canonical monster
The ordinary default variant used when the player has not selected another monster for an ambiguous task.

## Generic task
A broad Slayer assignment that can be completed using multiple materially different monsters, such as Birds, Dogs, or Bears. The MVP looks up its task-name Wiki page, which may be less accurate than a player-selected monster variant.

## Reminder item
An item associated with a relevant Slayer task. A reminder item is classified as either required or recommended.

## Required item
An item mechanically needed to complete or safely fight a relevant Slayer task. The plugin expects it to be in either the inventory or equipment.

## Recommended item
An optional item that improves task convenience or loot collection, such as a herb sack or seed box. Missing it does not prevent completing the task.

## Loot-container recommendation
A recommendation derived from the task monster's drop table. Tasks with substantial herb drops may recommend a herb sack; tasks with substantial seed drops may recommend a seed box.

## Satisfied requirement
A required-item condition for which every needed item is currently present in the inventory or equipment. Satisfaction is not permanent: losing or banking an item makes the condition unsatisfied again.

## Reminder
A persistent, visible prompt listing items associated with the player's current relevant Slayer task.

## Dismissal
The player intentionally hides the current reminder by clicking it. Dismissal is temporary and is reset by a subsequent bank visit.

## Bank visit
The period from opening through closing a regular bank interface or bank deposit box. The reminder stays hidden while either interface is open. Group Ironman shared storage is outside the MVP.

## Bank close
The closing of a regular bank interface or bank deposit box. It resets temporary dismissal and re-evaluates whether the player is missing reminder items.

## Reminder timeout
The ten-second period after a new assignment, bank close, or task check during which missing-item reminders may be shown.
