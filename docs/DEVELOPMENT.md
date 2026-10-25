# Development

Build and run the unit, Fabric server/client, and NeoForge server GameTests:

```shell
heavy status
heavy --label Telekinesis-tests -- ./gradlew --no-daemon build runAllGameTests
```

The conventions plugin manages headless client GameTests. The client-options property prevents it from copying a developer's Prism options into the test instance.

## How drops are captured

`DropCapture` keeps a per-thread stack of capture scopes. Three mixins in `common` open one, and both loaders use the same code:

- `ServerPlayerGameModeMixin` wraps `ServerPlayerGameMode.destroyBlock` for the breaking player.
- `LivingEntityMixin` wraps the `dropAllDeathLoot` call in `LivingEntity.die` for `getLastHurtByPlayer()` (nobody for a dying player). On NeoForge the drops pass `LivingDropsEvent` and are spawned inside that call.
- `ServerExplosionMixin` wraps the `interactWithBlocks` call in `ServerExplosion.explode` for the explosion's indirect source entity, or else the attacker in its damage source.

While a scope for an effective player is open, `ServerLevelMixin` (`addFreshEntity`) and `ExperienceOrbMixin` (`awardWithDirection`) move item entities and experience to that player. A scope opened for nobody hides any outer scope.

## Kill and explosion GameTests

`TelekinesisKillAndExplosionGameTests` summons husks with the test loot table `telekinesis:gametest/kill_probe` (one emerald plus one per Looting level), so every count is exact; husks do not burn in daylight. The explosion tests blow up a chest of 64 emeralds, because container contents always drop while explosions destroy block loot at random. They use real players joined through BrainageLib's `GameTestPlayer`, so the killer and the TNT owner are resolved as they are in play.

## Preference-file recovery

Unreadable or unsupported-schema `telekinesis-players.json` files are left intact.
Personal commands still apply in memory and report that saving failed. Repair or
restore the file and restart the server before expecting preferences to persist.
