# ADR 0010: Use a sidebar panel for dynamic variant selection

- Status: Accepted (amended by ADR 0011)
- Date: 2026-07-23

## Context

RuneLite's standard plugin configuration UI builds enum dropdowns from a Java enum. Their values cannot change with the current assignment or with Wiki discovery. A free-text Wiki-page override is generic but is not a good primary selection experience.

RuneLite sidebar plugin panels can own ordinary Swing controls and update their models at runtime.

## Decision

- Add a Slayer Item Reminders sidebar panel and navigation button.
- Follow RuneLite panel conventions: six-pixel outer spacing, dark card surfaces, bordered content cards, RuneScape fonts, `JShadowedLabel` text, a `PluginErrorPanel` empty state, and orange in-progress status text.
- Use RuneLite's `IconTextField` as a filter above a fully controlled, scrollable list of RuneLite-styled variant rows rather than constructing a `JComboBox`; wrap long task names and expose the selected variant separately.
- Show the active assignment and a `JComboBox` task-variant picker.
- Initially populate the picker with curated variants or a canonical fallback.
- Refresh it with the merged Wiki-discovered variants when discovery completes.
- Include an automatic option which uses the assignment's canonical default.
- Show a RuneLite `LinkBrowser` link to the selected variant's Wiki page, or the canonical page in automatic mode.
- Apply panel selections through the same assignment-generation and recommendation-refresh path as chatbox selections.
- Keep the assignment-scoped configuration key as hidden internal state so it remains stable but is no longer presented as an editable text field.
- Remove the navigation button and panel callback during plugin shutdown.

## Consequences

- The filterable variant list always reflects the current assignment and can support newly discovered Wiki variants.
- Large lists scroll inside the panel and filter immediately as the user types; richer grouping remains future work.
- The plugin gains a small optimized PNG icon and a persistent sidebar entry while enabled.
- Swing model updates are scheduled on the Swing event-dispatch thread.
