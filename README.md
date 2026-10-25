# Telekinesis

Telekinesis is a server-side Fabric and NeoForge mod for Minecraft 26.3 that places drops and experience directly into a player's inventory and experience total: from blocks the player mines, mobs the player kills and blocks broken by explosions the player causes. Vanilla clients can join without installing the mod.

Telekinesis is enabled for each player by default. Preferences are stored by UUID in `telekinesis-players.json` in the current world's directory and survive restarts.

## Features

- Uses Minecraft's actual loot results, so Fortune, Silk Touch, Looting, random loot, block-entity and container contents, equipment drops with their drop chances, and modded loot tables are all kept.
- Handles every item stack produced by one source, including non-guaranteed and multiple unique drops.
- Inserts as much as the player's inventory can accept. Any remainder is dropped at that player's feet rather than left where it was produced. Mined blocks already behaved this way, and mob kills and explosions do the same.
- Credits the experience from the same source directly to that player, repairing Mending items first as an experience orb would.
- There is no separate switch per source: the server-wide and personal settings below cover all three.

### Which drops count

- **Mined blocks:** blocks the player breaks, including blocks other mods break through the normal player block-break path.
- **Mob kills:** the drops and experience of a mob the game credits to the player, which is the same record vanilla uses for player-kill loot and experience. That covers melee hits, the player's arrows, tridents and damaging thrown potions (Harming; Poison and Wither effects are not credited), and a kill by the player's tamed wolf. A mob the player hurt in the last 5 seconds before it died from something else also counts, as in vanilla. Other pets' kills are not credited to their owner by the game, so they do not count. The ender dragon's experience, which it releases during its death animation, goes to the player who killed it.
- **Explosions:** the drops of blocks (and the contents of containers) broken by an explosion the player caused. The game records the cause as the explosion's source, and Telekinesis follows it:
  - TNT the player lit, directly or with a flaming arrow, and further TNT that this TNT sets off;
  - a projectile or fireball the player owns;
  - an end crystal the player hit (not further crystals that one sets off: the game credits nobody for those).

  Creepers, beds, respawn anchors and TNT that nobody lit (redstone, fire) have no player cause: the game records none, and Telekinesis does not add one. Mobs an explosion kills follow the mob rule; entities it breaks, such as item frames, armor stands and minecarts, drop in the world.
- **Not mobs:** armor stands, item frames, paintings, boats and minecarts the player breaks drop in the world.
- **Dead players:** while the player is dead, their kills and explosions drop in the world, because the inventory of a dead player is lost when they respawn.
- **Never captured:** items created by commands (`/give`, `/summon`, `/loot`), drops nobody caused (a mob that died without being hurt by a player, an explosion without a player cause), other players' death drops, and unrelated item and experience entities.

## Getting started

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer with Fabric API, or NeoForge 26.3.0.99 or newer
- BrainageLib 1.2.0 or newer for the matching loader
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

Telekinesis wraps the normal server-player block-break path rather than replacing loot-table calculation. Mods that break additional blocks through that path inherit Telekinesis automatically, including mob deaths and explosions without a player cause that happen during that break. In particular, every connected block mined by Vein Miner sends its drops and experience to the breaking player when both mods are installed. Vein Miner continues to use normal world drops when Telekinesis is absent or ineffective for that player.

Mob drops are captured around the game's death-loot step, after NeoForge's `LivingDropsEvent`, so drops that mods add or change there go to the player too. Drops only reach a player in the same dimension; otherwise they stay in the world. The ender dragon's experience is released during its death animation rather than with its loot, so it stays in the world.

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - builds and tests.
- [docs/RELEASE.md](docs/RELEASE.md) - release automation.
- [docs/MODRINTH.md](docs/MODRINTH.md) - optional Modrinth publishing.
