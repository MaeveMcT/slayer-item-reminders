# ADR 0011: Use the sidebar panel as the only variant picker

- Status: Accepted
- Date: 2026-07-23

## Context

Chatbox text menus do not scale well for assignments with many valid monsters. Pagination would add navigation state and scrolling is not available through that API. The sidebar panel can own a dynamic, filterable variant list without relying on the game chatbox.

Maintaining both chatbox and panel selectors also duplicates state transitions and can leave a stale chatbox menu open after a panel selection.

## Decision

- Remove the chatbox variant menu and its pending/open lifecycle state.
- When variant discovery finds multiple choices for a new or unresolved assignment, automatically open the Slayer Item Reminders sidebar panel.
- Keep all choices in the panel's dynamic filterable list, including automatic/default and Wiki-discovered variants.
- Route panel selections onto the RuneLite client thread and reject a selection if the panel's displayed task is no longer current.
- Continue reopening the panel after bank close while an ambiguous assignment remains unresolved.

## Consequences

- Variant selection uses a RuneLite `IconTextField` filter and scrollable RuneLite-styled rows instead of custom chatbox pagination.
- The plugin controls the complete selection UI and no longer replaces game chatbox dialogue.
- Variant selection has one user-facing source of truth.
- Infobox presentation remains unchanged and independent of the panel.
