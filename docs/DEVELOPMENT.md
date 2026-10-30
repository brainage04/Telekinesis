# Development

Build and run the unit, Fabric server/client, and NeoForge server GameTests:

```shell
heavy status
heavy --label Telekinesis-tests -- ./gradlew --no-daemon build runAllGameTests
```

The conventions plugin manages headless client GameTests. The client-options property prevents it from copying a developer's Prism options into the test instance.

## How drops are captured

`DropCapture` keeps a per-thread stack of capture scopes. These mixins in `common` open one, and both loaders use the same code:

- `ServerPlayerGameModeMixin` wraps `ServerPlayerGameMode.destroyBlock` for the breaking player.
- `LivingEntityMixin` wraps the `dropAllDeathLoot` call in `LivingEntity.die` for `getLastHurtByPlayer()`, while `getLastHurtByPlayerMemoryTime()` is above 0 (the test vanilla uses for player kills). On NeoForge the drops pass `LivingDropsEvent` and are spawned inside that call.
- `EnderDragonMixin` records the dragon's killer on its first death tick and wraps the two `ExperienceOrb.award` calls in `EnderDragon.tickDeath` for that player, because the dragon releases its experience after the 100-tick kill credit has expired.
- `ServerExplosionMixin` wraps the `interactWithBlocks` call in `ServerExplosion.explode` for the explosion's indirect source entity, or else the attacker in its damage source, or else the player `BedBlockMixin` or `RespawnAnchorBlockMixin` recorded. Vanilla creates bed and respawn-anchor explosions without a source entity, so those two mixins record the player who used the block around the single `explode` call.
- `BedBlockMixin` wraps the `Level.explode` call in `BedBlock.destroyOnUse`.
- `RespawnAnchorBlockMixin` wraps the `explode` call in `RespawnAnchorBlock.useWithoutItem`.
- `ServerPlayerMixin` wraps the `dropAllDeathLoot` call in `ServerPlayer.die` (which does not call `LivingEntity.die`) in a scope for nobody, which hides any outer scope, so a dying player's inventory is never handed to someone else.

A mob death or explosion without a player cause opens no scope, so inside a block break its drops still go to the breaker, as before mob and explosion support existed. While a scope is open, `ServerLevelMixin` (`addFreshEntity`) and `ExperienceOrbMixin` (`awardWithDirection`) move item entities and experience to its player if that player is alive, in the same level, and has Telekinesis in effect; otherwise they stay in the world. A dead player still resolves as a killer or TNT owner until they respawn, and their corpse's inventory is discarded at respawn.

## Kill and explosion GameTests

`TelekinesisKillAndExplosionGameTests` summons husks with the test loot table `telekinesis:gametest/kill_probe` (one emerald plus one per Looting level), so every count is exact; husks do not burn in daylight. The explosion tests blow up a chest of 64 emeralds, because container contents always drop while explosions destroy block loot at random. They use real players joined through BrainageLib's `GameTestPlayer`, so the killer and the TNT owner are resolved as they are in play. The bed and respawn-anchor tests build their blast in the Nether or the test level and send a second player in creative standing nearer the blast than the player who used the block, to show the credit follows the user and not proximity.

## Preference-file recovery

Unreadable or unsupported-schema `telekinesis-players.json` files are left intact.
Personal commands still apply in memory and report that saving failed. Repair or
restore the file and restart the server before expecting preferences to persist.
