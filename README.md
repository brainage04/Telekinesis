# Telekinesis

Telekinesis is a server-side Fabric and NeoForge mod for Minecraft 26.3 that places drops and experience directly into a player's inventory and experience total: from blocks the player mines, the leaves of trees the player cuts, mobs the game credits the player with killing, the item frames, glow item frames, paintings and armour stands the player breaks, and blocks broken by explosions the player causes. Vanilla clients can join without installing the mod.

Telekinesis is enabled for each player by default. Preferences are stored by UUID in `telekinesis-players.json` in the current world's directory and survive restarts.

## Features

- Uses Minecraft's actual loot results, so Fortune, Silk Touch, Looting, random loot, block-entity and container contents, equipment drops with their drop chances, and modded loot tables are all kept.
- Handles every item stack produced by one source, including non-guaranteed and multiple unique drops.
- Inserts as much as the player's inventory can accept. What does not fit is moved, as the game's own item entity, to that player's feet instead of staying where it was produced; vanilla always leaves it where it spawned.
- Credits the experience from the same source directly to that player, repairing Mending items first as an experience orb would.
- The server-wide and personal settings below cover every source; there is no separate switch per source.

### Which drops count

- **Mined blocks:** blocks the player breaks, including blocks other mods break through the normal player block-break path or break naming the player as the breaker.
- **Leaves of a tree the player cut:** when the player breaks a log, the leaves that log held up (up to six leaves away, as the game counts it) and that decay within the next minute drop for that player. A leaf belongs to whoever broke the last log that held it up, so when two players cut one tree, a leaf the second player's log held is theirs, and drops in the world if their Telekinesis is off. Leaves another tree's logs still hold, and leaves of trees nobody cut, drop in the world.
- **Mob kills:** the drops and experience of a mob the game credits to the player, which is the very player it awards the kill statistic and the `player_killed_entity` advancement to (`LivingEntity.getKillCredit`). That covers melee hits, the player's arrows, tridents, damaging thrown potions (Harming; Poison and Wither effects are not credited), fireworks, thorns damage from armour the player wears, TNT the player lit, and a kill by the player's tamed wolf. A mob the player hurt keeps that credit when it then dies from something else, such as a fall, lava, the void or another mob's blow: the game's 5-second timer only decides whether the death drops as a player kill, while the last-hurt record it keeps still awards the kill statistic, and that record names the player until another attacker hurts the mob. The mod asks the game at the moment of death instead of keeping its own timer, so those deaths are captured too. Other pets' kills are not credited to their owner by the game, so they do not count. The ender dragon's experience, which it releases during its death animation, goes to the player who killed it.
- **Item frames, paintings, cushions and armour stands:** the drops of an item frame (plain or glowing), painting, cushion or armour stand a player breaks: the frame's own item and the item it held, the painting or cushion itself, or the stand and the equipment it wore. A punch knocks an item out of a frame first, and that item goes to the player as well. A blast the player caused counts, through the player it names; a creeper's blast, a fire and a decoration that lost its support leave their drops in the world, and a creative break drops nothing at all.
- **Explosions:** the drops of blocks (and the contents of containers) broken by an explosion the player caused. The game records the cause as the explosion's source, and Telekinesis follows it:
  - TNT the player lit, directly or with a flaming arrow, and further TNT that this TNT sets off;
  - a projectile or fireball the player owns;
  - an end crystal the player hit;
  - a bed or charged respawn anchor the player used where it explodes (beds in the Nether and the End, anchors outside the Nether).

  The game itself names no cause for a bed or anchor explosion; Telekinesis credits the player who used it for that one explosion's block drops only, so its damage, knockback and kill credit stay the game's. Creepers and TNT that nobody lit (redstone, fire) have no player cause: the game records none, and Telekinesis does not add one. Mobs an explosion kills follow the mob rule, and a frame, painting or armour stand the player's own blast breaks follows the item frame rule; boats and minecarts drop in the world.
- **Not yet:** boats and minecarts a player breaks, and the contents of a chest minecart or chest boat, drop in the world.
- **Dead players:** while the player is dead, their kills and explosions drop in the world, because the inventory of a dead player is lost when they respawn.
- **Never captured:** items created by commands (`/give`, `/summon`, `/loot`), drops nobody caused (a mob the game credits to nobody, an explosion without a player cause), other players' death drops, and unrelated item and experience entities.

## Getting started

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer with Fabric API, or NeoForge 26.3.0.99 or newer
- BrainageLib 1.4.0 or newer for the matching loader
- Java 25 or newer
- Install Telekinesis and its dependencies on the server.

## Commands

- `/telekinesis` - same as `/telekinesis status`.
- `/telekinesis status` - show the server-wide, personal, and effective states.
- `/telekinesis enable` - enable your persistent preference.
- `/telekinesis disable` - disable your persistent preference.
- `/telekinesis toggle` - toggle your persistent preference.
- `/telekinesis help` - show the command summary.
- `/telekinesis config` - same as `/telekinesis config status` (operators only).
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

Mob drops are captured around the game's death-loot step, after NeoForge's `LivingDropsEvent`, so drops that mods add or change there go to the player too. Drops only reach a player in the same dimension; otherwise they stay in the world.

The capture itself is BrainageLib's shared drop engine, which Skyport Minigames uses too. When both are installed, a player in a Skyport Minigames match follows that match's rules (its mined-drops setting and its Telekinesis perks); Telekinesis' own settings decide everywhere else.

## Documentation

- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) - builds and tests.
- [docs/RELEASE.md](docs/RELEASE.md) - release automation.
- [docs/MODRINTH.md](docs/MODRINTH.md) - optional Modrinth publishing.
