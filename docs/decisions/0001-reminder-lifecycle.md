# ADR 0001: Initial reminder lifecycle

- Status: Accepted
- Date: 2026-07-18

## Context

A reminder should remain noticeable after a relevant Slayer task is assigned, but it must not remain indefinitely when the player chooses to do another activity.

## Decision

- Assigning a relevant Slayer task creates a persistent reminder.
- The player can use an infobox's **Shift-right-click → Dismiss** action to dismiss both reminder categories temporarily.
- The reminder remains hidden while the bank is open.
- Closing a regular bank interface or bank deposit box resets a previous dismissal and shows the reminder again if items are still missing.
- The requirement is satisfied only while every required item is present in either the inventory or equipment.
- If a required item leaves both inventory and equipment, the requirement becomes unsatisfied.
- Manual dismissal takes priority over item changes: a dismissed reminder stays hidden until the next bank closes.
- A visible reminder times out after five minutes.
- A bank close or newly assigned task resets dismissal and starts a fresh five-minute reminder window.

## Consequences

- Dismissal does not permanently suppress reminders for the current task.
- Closing the bank becomes an explicit lifecycle trigger.
- The implementation must distinguish visible, temporarily dismissed, timed out, and currently satisfied reminder states.
- Inventory and equipment changes must be observed, while ordinary movement between those two containers must not produce a false reminder.

## Future considerations

- Reconsider whether losing a required item should override manual dismissal.
- Reconsider whether losing an item should override a timeout.
- Investigate Group Ironman shared storage as an additional bank-close trigger.
