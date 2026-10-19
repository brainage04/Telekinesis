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
wrappers are entrypoints, not duplicate tests to prune. Kept the integrated-client
scenario because it also exercises a real client's inventory and server commands.
No tests met the redundant criterion.
