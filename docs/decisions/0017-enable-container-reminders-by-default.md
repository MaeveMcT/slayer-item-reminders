# ADR 0017: Enable loot-container reminders by default

- Status: Accepted
- Date: 2026-09-19

## Context

Herb sack and seed box reminders were opt-in, which hid derived recommendations until players found and enabled both ownership settings. The settings describe the expected default ownership while still needing to support players who do not own a container or do not want its reminders.

## Decision

- Enable **Own herb sack** and **Own seed box** by default for users without stored values.
- Keep the existing config group and keys unchanged.
- Do not write or reset these values during startup, assignment changes, or shutdown.

## Consequences

- New users receive applicable herb sack and seed box reminders without configuring the plugin first.
- RuneLite's stored `false` value continues to override the default, so disabling either setting persists across restarts and plugin updates.
