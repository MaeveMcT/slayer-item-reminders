# ADR 0003: Task synchronization and reminder triggers

- Status: Accepted
- Date: 2026-07-18

## Context

The plugin must know the current task without depending on chat wording or requiring RuneLite's built-in Slayer plugin. It should avoid presenting a stale reminder merely because the player logged in while planning another activity.

## Decision

- Read the current Slayer target and remaining count from game varps and database tables.
- Detect a newly assigned task during the session and evaluate its reminder immediately.
- Re-evaluate the current task when the bank closes.
- Synchronize an existing task silently after login or plugin startup; do not show a reminder until the next bank close.
- A new assignment and a bank close are the two ordinary user-visible reminder triggers.
- Task completion, cancellation, or replacement clears both required and optional infoboxes immediately. A replacement task then begins a new reminder lifecycle.
- Item presence checks run locally. Required-item knowledge and drop-table recommendations are loaded asynchronously from centralized OSRS Wiki data, with local fallback rules for Gargoyles and Desert lizards.
- Successful Wiki results are cached only for the current RuneLite session.
- When an asynchronous Wiki response produces an optional recommendation, show the optional infobox immediately if the response still belongs to the current task.
- A newly returned optional recommendation may appear even if the player dismissed the earlier reminder while the request was pending; this is an explicit exception to dismissal priority.

## Consequences

- The plugin does not depend on chat-message wording or the built-in Slayer plugin.
- Login by itself does not interrupt the player with an existing-task reminder.
- Bank close resets manual dismissal, starts a ten-second reminder window, and causes current inventory and equipment to be evaluated.
- Task assignment must be distinguished from initial synchronization and ordinary task-count changes.

