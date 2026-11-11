# Development

Build and run the unit, Fabric server/client, and NeoForge server GameTests:

```shell
heavy status
heavy --label Telekinesis-tests -- ./gradlew --no-daemon build runAllGameTests
```

The conventions plugin manages headless client GameTests. The client-options property prevents it from copying a developer's Prism options into the test instance.

`TelekinesisClientGameTest` is also the showcase film: one stage per feature (a mined ore, a broken chest, a felled tree, a mob kill, TNT the player lit, a respawn anchor outside the Nether, a full inventory, the personal setting turned off, a bed in the Nether), each with its caption, and each checks on the server that the drops went where the caption says. It prints `STAGE_START`/`STAGE_END` lines around what a finished film keeps; everything between them (teleports, setup, chunk loading) is meant to be cut. Every stage plays like a person: the player walks with the movement keys, turns to the target and presses the attack or use key through `ClientGameTestPlayerActions`, which fails the stage unless the crosshair is on the target and the target is inside vanilla reach (4.5 blocks to a block, 3.0 to an entity). Never raise `BLOCK_INTERACTION_RANGE` and never break a block on the server for the camera. Record it on a host with PulseAudio:

```shell
GTR_RECORDING_PROFILE=showcase ./gradlew --no-daemon :fabric:recordClientGameTest
```

## How drops are captured

The capture itself is BrainageLib's shared engine (`io.github.brainage04.brainagelib.drops`, see BrainageLib's
`docs/DEVELOPMENT.md`): its mixins open a capture scope for a player's block break, a mob's death loot credited to a
player, an explosion's block step, a bed or respawn anchor the player clicked, and a decaying leaf of a tree the player
cut (every log a player breaks is recorded, whoever they are). This mod has no mixins of its own. It registers
`TelekinesisDropPolicy` at `DropPolicies.DEFAULT_PRIORITY`: it governs every player and delivers to those whose
server-wide and personal settings are both on, so a minigame that registers a higher policy keeps its own rules for the
players in its matches. What a policy does not deliver stays in the world.

## Kill and explosion GameTests

`TelekinesisKillAndExplosionGameTests` summons husks with the test loot table `telekinesis:gametest/kill_probe` (one emerald plus one per Looting level), so every count is exact; husks do not burn in daylight. The explosion tests blow up a chest of 64 emeralds, because container contents always drop while explosions destroy block loot at random. They use real players joined through BrainageLib's `GameTestPlayer`, so the killer and the TNT owner are resolved as they are in play. The bed and respawn-anchor tests build their blast in the Nether or the test level and send a second player in creative standing nearer the blast than the player who used the block, to show that BrainageLib's credit follows the user and not proximity.

## Preference-file recovery

Unreadable or unsupported-schema `telekinesis-players.json` files are left intact.
Personal commands still apply in memory and report that saving failed. Repair or
restore the file and restart the server before expecting preferences to persist.
