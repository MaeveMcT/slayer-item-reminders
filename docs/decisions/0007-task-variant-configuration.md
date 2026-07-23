# ADR 0007: Expose current task variant in plugin configuration

- Status: Accepted
- Date: 2026-07-23

## Context

ADR 0006 introduced a chatbox menu for ambiguous assignments. Players also need to inspect and change the active variant without returning to a bank or waiting for that prompt.

RuneLite configuration enum choices are static, so the configuration lists the curated variants known to the plugin rather than generating a task-specific list at runtime.

## Decision

- Add a plugin-specific `slayer-item-reminders` configuration group.
- Expose a **Current task variant** dropdown in RuneLite's Slayer Item Reminders configuration panel.
- Include an automatic option and the curated Greater demons variants.
- Ignore a choice which does not apply to the current assignment and use that assignment's canonical default.
- Apply a valid change immediately, invalidating an older asynchronous Wiki lookup and refreshing reminders.
- Keep the chatbox menu from ADR 0006 and synchronize choices made there into the configuration dropdown.
- Reset the dropdown to automatic when the assignment changes or clears; it is assignment state, not a remembered preference.

## Consequences

- Players can change variants from the plugin list's configuration panel at any point during an assignment.
- Adding curated variants requires adding corresponding enum choices because RuneLite configuration dropdowns are not dynamic.
- Existing assignment-scoped selection and stale-response protections remain in effect.
