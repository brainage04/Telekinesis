# Development

Build and run the unit, Fabric server/client, and NeoForge server GameTests:

```shell
heavy status
heavy --label Telekinesis-tests -- ./gradlew --no-daemon build runAllGameTests
```

The conventions plugin manages headless client GameTests. The client-options property prevents it from copying a developer's Prism options into the test instance.

`TelekinesisClientGameTest` runs the showcase film, `TelekinesisShowcase` in `fabric/src/recording` (the film-only source set, with its captions in `telekinesis-captions.json`), so `fabric/src/gametest` holds only tests. The film shows every scenario twice, about two seconds each: first the negative take, then the positive one, then the next scenario. The scenarios are a mined ore, a broken chest, a felled tree, a killed husk, a husk that lava kills, a husk an iron golem kills, a blaze a snow golem kills, a husk the player's wolf kills, an item frame, an armour stand, a painting, a cushion, a chest boat, a chest minecart, lit TNT, a respawn anchor outside the Nether, a full inventory and a bed in the Nether. The negative take has Telekinesis turned off; the first one shows `/telekinesis disable` in chat, and later takes set the preference while the player is teleported into the bay. In the lava, iron golem and snow golem scenarios Telekinesis stays on and the negative take is the death the game does not credit to the player. Each scenario has two identical bays side by side, built before the client joins, and the player arrives in each bay already looking at the take's first target, so no take opens with a camera turn. Each take checks on the server that the drops went where its caption says. In a negative take the player stands far enough back that no drop can slide into its pickup area, the drops are checked once they have come to rest, and the take fails if anything reached the inventory by its last filmed tick. In the tree take the player looks straight up once the third log breaks, and the crosshair stays on the lowest log left while the rest of the trunk comes down. Punches on entities follow the held item's attack delay (`Player.getCurrentItemAttackStrengthDelay`); the item frame is punched bare-handed, so its two punches come five ticks apart. The bays' blocks are built by code before the recording starts, not loaded from a saved structure; the take's mobs and decorations are spawned during the teleport, before the take's caption.

```shell
GTR_RECORDING_PROFILE=showcase ./gradlew --no-daemon :fabric:recordClientGameTest
```

Record the film on a GPU display, not an Xvfb one: pass `-PrecordingDisplay=:1` on a host whose `:1` is a real card, and keep the take only when the recorder reports `gpu.active` true, an NVIDIA renderer, and 30 or more presented frames a second. The delivered film is that take trimmed to the stages: cut the world load before the first caption, the scene setups that leave the camera still for more than a second, the dimension change, and the world-save screen at the end, then re-encode at 30 fps CFR with no stage straddling a join.

`:fabric:pitest` runs mutation tests on `TelekinesisConfigManager` against its unit tests. It is not part of `check`; the report is written to `fabric/build/reports/pitest`.

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

`TelekinesisCompanionKillGameTests` pins the game's kill credit for kills an iron golem, a snow golem or a wolf finishes: a golem's blow after the player's hit is the player's kill, a golem's kill of a mob that only hit the player is the golem's and its loot stays, a snowball finishing a blaze the player punched is the player's, a snow golem's kill of a blaze nobody hit drops nothing, and a tamed wolf's kill is its owner's, also after another player's hit.

`TelekinesisEntityAndCreditGameTests` covers the item frames, glowing item frames, paintings, cushions and armour stands a player breaks, with the personal setting off as the negative case, and the credited deaths that do not come from a killing blow: a hit followed by a death from something else, and a kill the game credits through its last-hurt record alone. The negative credit cases are a death nobody caused and a death the game credits to a mob, which is set up the way the game does it, a husk's non-lethal blow followed by a lethal lava hit.

`TelekinesisVehicleGameTests` breaks a boat, a chest boat and every minecart with a diamond sword (a vehicle breaks once its accumulated damage passes 40, so the helper punches until it goes), and checks each item and the container variants' contents in the inventory and nothing on the ground. Its negative cases are the personal setting off, a creative player's punch, and lava breaking a chest minecart.

## Preference-file recovery

Unreadable or unsupported-schema `telekinesis-players.json` files are left intact.
Personal commands still apply in memory and report that saving failed. Repair or
restore the file and restart the server before expecting preferences to persist.
