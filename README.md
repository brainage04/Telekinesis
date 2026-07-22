# Telekinesis

Telekinesis is a server-side Fabric mod that places player-mined block drops directly into the player's inventory.
Clients do not need to install it.

## Behavior

- Uses Minecraft's actual block-loot result, so Fortune, Silk Touch, random loot, block-entity contents, and modded loot tables keep their normal behavior.
- Handles every item entity produced by one block break, including multiple stacks and different item types.
- Inserts as much as the inventory can accept; any remainder spawns in the world normally.
- Applies only to drops produced while a player breaks a block. Mob drops, experience orbs, commands, explosions, and unrelated item spawns are unchanged.

## Configuration

Telekinesis is enabled by default through the `telekinesis:enabled` game rule.

Operators can inspect or change it with:

```text
/telekinesis
/telekinesis enable
/telekinesis disable
/telekinesis <true|false>
/gamerule telekinesis:enabled <true|false>
```

The `/telekinesis` command requires game-master permission.

## Compatibility

Telekinesis wraps the normal server-player block-break path rather than replacing loot-table calculation.
Mods that break additional blocks through that path inherit Telekinesis automatically.
In particular, Vein Miner's connected block drops enter the breaking player's inventory when both mods are installed.

## Development

Build and run the server GameTests with:

```shell
./gradlew build
./gradlew runGameTest
```

Release automation is documented in [docs/RELEASE.md](docs/RELEASE.md).
Optional Modrinth publishing is documented in [docs/MODRINTH.md](docs/MODRINTH.md).
