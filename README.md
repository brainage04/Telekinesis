# Telekinesis

Telekinesis is a server-side Fabric and NeoForge mod for Minecraft 26.2 that places player-mined block drops and experience directly into the breaking player's inventory and experience total. Vanilla clients can join without installing the mod.

Telekinesis is enabled for each player by default. Preferences are stored by UUID in `telekinesis-players.json` in the current world's directory and survive restarts.

## Features

- Uses Minecraft's actual block-loot result, preserving Fortune, Silk Touch, random loot, block-entity contents, and modded loot tables.
- Handles every item stack produced by one attributed player block break, including non-guaranteed and multiple unique drops.
- Inserts as much as the player's inventory can accept. Any remainder is dropped at that player's feet rather than left at the mined block.
- Credits experience produced by the same player block break directly to that player.
- Does not intercept mob drops, commands, explosions, or unrelated item and experience entities.

## Getting started

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer with Fabric API, or NeoForge 26.2.0.88 or newer
- BrainageLib 1.1.0 or newer for the matching loader
- Java 25 or newer
- Install Telekinesis and its dependencies on the server.

## Commands

- `/telekinesis status` - show the server-wide, personal, and effective states.
- `/telekinesis enable` - enable your persistent preference.
- `/telekinesis disable` - disable your persistent preference.
- `/telekinesis toggle` - toggle your persistent preference.
- `/telekinesis help` - show the command summary.
- `/telekinesis config status` - inspect the server-wide state (operators only).
- `/telekinesis config enable` - enable the mod server-wide (operators only).
- `/telekinesis config disable` - disable the mod server-wide (operators only).
- `/telekinesis config toggle` - toggle the server-wide state (operators only).
- `/telekinesis config reload` - reload `telekinesis.json` without restarting (operators only).

### BrainageLib commands

Telekinesis registers with BrainageLib's combined first-join notice.

- `/servermods help` - prints the help lists for all mods which depend on BrainageLib.
- `/servermods config` - (operators only) prints the settings command for each mod which depends on BrainageLib.

## Configuration

The server-wide setting defaults to enabled and is stored in `config/telekinesis.json`. If that file or a world's `telekinesis-players.json` cannot be read, Telekinesis leaves it untouched: changes made with commands apply until the server stops and the command says they could not be saved. Fix the file and restart the server; for `telekinesis.json`, `/telekinesis config reload` also works.

The global and personal settings must both be enabled for Telekinesis to take effect.

## Compatibility

Telekinesis wraps the normal server-player block-break path rather than replacing loot-table calculation. Mods that break additional blocks through that path inherit Telekinesis automatically. In particular, every connected block mined by Vein Miner sends its drops and experience to the breaking player when both mods are installed. Vein Miner continues to use normal world drops when Telekinesis is absent or ineffective for that player.

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - builds and tests.
- [docs/RELEASE.md](docs/RELEASE.md) - release automation.
- [docs/MODRINTH.md](docs/MODRINTH.md) - optional Modrinth publishing.
