# Development

Build and run the unit, Fabric server/client, and NeoForge server GameTests:

```shell
heavy status
heavy --label Telekinesis-tests -- ./gradlew --no-daemon -Pfabricmoddingconventions.devClientOptions=/nonexistent build runAllGameTests
```

The conventions plugin manages headless client GameTests. The client-options property prevents it from copying a developer's Prism options into the test instance.

## Test review

Frozen pruning rubric (2026-10-19):

- Keep: real production behaviour with a distinct input, failure mode, or loader/runtime.
- Redundant: another named test exercises the same path and assertions without a distinct case.
- Tautological: constants, registration/wiring, copied logic, or mock echoes without behaviour.

Removed as tautological:

- `fabricLoaderBootsInServerModeForTests`: checks the unit-test runner's environment.
- `fabricLoaderCanResolveModMetadata`: checks metadata constants and file presence.
- `commandIsRegistered` (both loaders): checks command-tree wiring, not command behaviour.

Kept the seven shared server tests: direct item pickup, direct experience, random loot,
container contents, full inventory, global disable, and personal disable. Their loader
wrappers are entrypoints, not duplicate tests to prune. Kept the dedicated-server/client
scenario because it exercises a connected player's inventory. No tests met the
redundant criterion.

## Coverage gaps

The pre-wave suite had no behavioural command/permission tests, reload/error-path
tests, partial-stack overflow checks, enchanted-tool tests, nested-break attribution,
out-of-scope spawn checks, or preference lifecycle tests.

Added shared server tests for personal commands and permission denial, console and
operator commands, saved reload, partial overflow conservation, Silk Touch, Mending,
nested attribution, unrelated items/experience after a break, and preference save,
shutdown cleanup and reload. Both loaders run these bodies synchronously, restoring
command-test settings in `finally` blocks. The lifecycle test invokes the production
callbacks with a real server and player; it is not a second JVM restart.

Unit tests cover config persistence, malformed/empty reload preservation, temporary
file cleanup and save failure. The existing client scenario remains the connected
Fabric client check. There is no custom rendering, gamerule, config screen or keybind.

Still manual: actual Vein Miner/modded-loot integration, Fortune's random distribution,
NeoForge client presentation, and preference persistence across a separate process.
