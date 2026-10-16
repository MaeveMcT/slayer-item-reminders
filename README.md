# Slayer Item Reminders

A standalone RuneLite plugin that reminds players which required and useful items to bring for Slayer tasks.

The plugin shows short-lived required and optional infoboxes after a new assignment, bank visit, or explicit task check. Its sidebar discovers valid monster variants from the OSRS Wiki so recommendations can follow the monster the player intends to fight.

## Stored-task reminders

Open **Slayer Rewards → Tasks** once to learn your stored assignment. The plugin remembers it separately for each RuneScape profile and refreshes it whenever that tab is visible. Before the first observation, storage is shown as **unknown**, not empty. If storage changes while the plugin is disabled or on another client, reopen the tab to refresh it.

- The built-in Slayer counter's tooltip includes your stored task.
- The sidebar also shows storage, even without an active task. No separate stored-task infobox is created.
- Engaging a matching monster or boss produces a game message and RuneLite notification by default. Configure sound, screen flash, and desktop alerts through **Stored task notification**.
- Warnings are limited to once per minute. Identical current/stored tasks and monsters matching both assignments do not warn.

Monster matching reuses Wiki variant discovery. Warnings wait until current-task discovery succeeds, so an unavailable lookup does not cause a false overlap warning. Matching identifies monster compatibility; it does not validate location restrictions or guarantee task credit.

## Design

Start with the [documentation index](docs/README.md), then see the [domain model](docs/domain-model.md) for the complete current behavior.

## Development

Build the plugin:

```sh
./gradlew build
```

Launch a RuneLite development client with the plugin loaded:

```sh
./gradlew run
```

When using a Jagex Account, follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) development-client login instructions.

## License

BSD 2-Clause License. See [LICENSE](LICENSE).
