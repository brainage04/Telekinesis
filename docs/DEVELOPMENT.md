# Development

Build and run the unit, Fabric server/client, and NeoForge server GameTests:

```shell
heavy status
heavy --label Telekinesis-tests -- ./gradlew --no-daemon build runAllGameTests
```

The conventions plugin manages headless client GameTests. The client-options property prevents it from copying a developer's Prism options into the test instance.

`TelekinesisClientGameTest` is also the showcase film: one stage per feature, each with its caption, and each checks on the server that the drops went where the caption says. The stages are a mined ore, a broken chest, a felled tree, a killed husk, a credited kill and an uncredited kill, an item frame, an armour stand, a painting, a cushion, a chest boat, a chest minecart, lit TNT, a respawn anchor outside the Nether, a full inventory, the personal setting turned off with a broken chest and a killed husk, and a bed in the Nether. Every stage starts with the player already looking at the first block or entity it acts on. The tree stage opens two blocks from the trunk, holds the attack key from the first log to the last, and breaks the third log while the player walks in under the trunk; afterwards the player stands still and looks up at the leaves decaying. The test prints `STAGE_START`/`STAGE_END` lines around what a finished film keeps; everything between them (teleports, setup, chunk loading) is meant to be cut. Every stage plays like a person: the player walks with the movement keys, turns to the target and presses the attack or use key through `ClientGameTestPlayerActions`, which fails the stage unless the crosshair is on the target and the target is inside vanilla reach (4.5 blocks to a block, 3.0 to an entity). A stage that holds the attack key through several blocks checks the crosshair's target before each break. Never raise `BLOCK_INTERACTION_RANGE` and never break a block on the server for the camera. Record it on a host with PulseAudio:

```shell
GTR_RECORDING_PROFILE=showcase ./gradlew --no-daemon :fabric:recordClientGameTest
```

Record the film on a GPU display, not an Xvfb one: pass `-PrecordingDisplay=:1` on a host whose `:1` is a real card, and keep the take only when the recorder reports `gpu.active` true, an NVIDIA renderer, and 30 or more presented frames a second. The delivered film is that take trimmed to the stages: cut the world load before the first caption, the scene setups that leave the camera still for more than a second, the dimension change, and the world-save screen at the end, then re-encode at 30 fps CFR with no stage straddling a join.

## How drops are captured

The capture itself is BrainageLib's shared engine (`io.github.brainage04.brainagelib.drops`, see BrainageLib's
`docs/DEVELOPMENT.md`): its mixins open a capture scope for a player's block break, a mob's death loot credited to a
player, a hanging decoration or armour stand the player breaks, a boat or minecart the player breaks, an explosion's
block step, a bed or respawn anchor the player clicked, and a decaying leaf of a tree the player cut (every log a player
breaks is recorded, whoever they are). The mob scope asks the game's own `LivingEntity#getKillCredit`, whose last-hurt
record expires after the game's 100 ticks (`LivingEntity.java:484-488`), so a death the game credits to nobody, or to a
mob, keeps its loot in the world.
This mod has no mixins of its own. It registers `TelekinesisDropPolicy` at `DropPolicies.DEFAULT_PRIORITY`: it governs
every player and delivers to those whose server-wide and personal settings are both on, so a minigame that registers a
higher policy keeps its own rules for the players in its matches. What a policy does not deliver stays in the world.

## Kill, break and credit GameTests

`TelekinesisKillAndExplosionGameTests` summons husks with the test loot table `telekinesis:gametest/kill_probe` (one emerald plus one per Looting level), so every count is exact; husks do not burn in daylight. The explosion tests blow up a chest of 64 emeralds, because container contents always drop while explosions destroy block loot at random. They use real players joined through BrainageLib's `GameTestPlayer`, so the killer and the TNT owner are resolved as they are in play. The bed and respawn-anchor tests build their blast in the Nether or the test level and send a second player in creative standing nearer the blast than the player who used the block, to show that BrainageLib's credit follows the user and not proximity. `expiredKillCreditLeavesKillLootInWorld` lets the game's 100-tick credit actually run out before the mob dies, instead of setting the timer to zero, which the game still credits.

`TelekinesisEntityAndCreditGameTests` covers the item frames, glowing item frames, paintings and armour stands a player breaks, with the personal setting off as the negative case, and the credited deaths that do not come from a killing blow: a hit followed by a death from something else, and a kill the game credits through its last-hurt record alone. The negative credit cases are a death nobody caused and a death the game credits to a mob, which is set up the way the game does it, a husk's non-lethal blow followed by a lethal lava hit.

`TelekinesisVehicleGameTests` breaks a boat, a chest boat and every minecart with a diamond sword (a vehicle breaks once its accumulated damage passes 40, so the helper punches until it goes), and checks each item and the container variants' contents in the inventory and nothing on the ground. Its negative cases are the personal setting off, a creative player's punch, and lava breaking a chest minecart.

## Preference-file recovery

Unreadable or unsupported-schema `telekinesis-players.json` files are left intact.
Personal commands still apply in memory and report that saving failed. Repair or
restore the file and restart the server before expecting preferences to persist.
