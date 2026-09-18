# ADR 0012: Show reminders when enabled during an existing task

- Status: Accepted
- Date: 2026-09-07
- Amends: ADR 0003

## Context

ADR 0003 treated plugin startup and login identically: an existing assignment was synchronized silently. That made a newly installed or manually enabled plugin appear not to provide its primary infobox behavior. The sidebar appeared immediately, but reminders remained hidden until a later bank close or assignment.

A normal client login should still avoid interrupting a player who happens to have an old task. Explicitly installing or enabling the plugin while already logged in is different: the player has just requested its behavior and needs immediate feedback that it works.

## Decision

- When the plugin starts while the client is already logged in, treat the existing assignment as a visible reminder trigger.
- Keep game login, hopping, and reconnect synchronization silent.
- Preserve the existing five-minute window, item checks, variant discovery, and bank behavior.

## Consequences

- A player installing or enabling the plugin during a relevant assignment can see an applicable infobox immediately.
- Ordinary login does not surface a stale assignment reminder.
- Tasks with no missing required items or derived recommendations still produce no infobox.
