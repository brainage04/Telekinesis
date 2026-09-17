# Telekinesis

Telekinesis is a server-side Fabric and NeoForge mod for Minecraft 26.2 that places player-mined block drops and experience directly into the breaking player's inventory and experience total. Vanilla clients can join without installing the mod.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer with Fabric API, or NeoForge 26.2.0.23-beta or newer
- BrainageLib 1.0.1 or newer for the matching loader
- Java 25 or newer

## Migrating from the Fabric-only release

- Install exactly one matching loader JAR: `telekinesis-<version>.jar` for Fabric or `telekinesis-neoforge-<version>.jar` for NeoForge. Remove the old Telekinesis JAR before switching loaders.
- Telekinesis remains server-side; vanilla clients do not install it. Install the matching Fabric or NeoForge BrainageLib release on the server.
- The mod ID remains `telekinesis`, and the existing `config/telekinesis.json` plus per-world `telekinesis-players.json` data paths are preserved.
- Maintainers can produce both loader artifacts with root `./gradlew build`; they are collected under `build/libs`.

## Behavior

- Uses Minecraft's actual block-loot result, preserving Fortune, Silk Touch, random loot, block-entity contents, and modded loot tables.
- Handles every item stack produced by one attributed player block break, including non-guaranteed and multiple unique drops.
- Inserts as much as the player's inventory can accept. Any remainder is dropped at that player's feet rather than left at the mined block.
- Credits experience produced by the same player block break directly to that player.
- Does not intercept mob drops, commands, explosions, or unrelated item and experience entities.

## Player controls

Telekinesis is enabled for each player by default. Preferences are stored by UUID in the current world's data directory and survive restarts.

- `/telekinesis status` — show the server-wide, personal, and effective states.
- `/telekinesis enable` — enable your persistent preference.
- `/telekinesis disable` — disable your persistent preference.
- `/telekinesis toggle` — toggle your persistent preference.
- `/telekinesis help` — show the command summary.

## Operator configuration

The server-wide setting defaults to enabled and is stored in `config/telekinesis.json`.

- `/telekinesis config status` — inspect the server-wide state.
- `/telekinesis config enable` — enable the mod server-wide.
- `/telekinesis config disable` — disable the mod server-wide.
- `/telekinesis config toggle` — toggle the server-wide state.
- `/telekinesis config reload` — reload `telekinesis.json` without restarting.

The global and personal settings must both be enabled for Telekinesis to take effect.

## Compatibility

Telekinesis wraps the normal server-player block-break path rather than replacing loot-table calculation. Mods that break additional blocks through that path inherit Telekinesis automatically. In particular, every connected block mined by Vein Miner sends its drops and experience to the breaking player when both mods are installed. Vein Miner continues to use normal world drops when Telekinesis is absent or ineffective for that player.

## Shared server help

Telekinesis registers with BrainageLib's combined first-join notice. Players can run `/servermods help`; operators can also run `/servermods config`.

## Development

```shell
./gradlew test runGameTest
```

Release automation is documented in [docs/RELEASE.md](docs/RELEASE.md). Optional Modrinth publishing is documented in [docs/MODRINTH.md](docs/MODRINTH.md).
