# Slayer Item Reminders

A standalone RuneLite plugin that reminds players which required and useful items to bring for Slayer tasks.

The plugin shows short-lived required and optional infoboxes after a new assignment, bank visit, or explicit task check. Its sidebar discovers valid monster variants from the OSRS Wiki so recommendations can follow the monster the player intends to fight.

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
