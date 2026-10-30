# Development

Build and run the unit, Fabric server/client, and NeoForge server GameTests:

```shell
heavy status
heavy --label Telekinesis-tests -- ./gradlew --no-daemon build runAllGameTests
```

The conventions plugin manages headless client GameTests. The client-options property prevents it from copying a developer's Prism options into the test instance.

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
