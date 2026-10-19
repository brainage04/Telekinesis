# Development

Build and run the unit, Fabric server/client, and NeoForge server GameTests:

```shell
heavy status
heavy --label Telekinesis-tests -- ./gradlew --no-daemon -Pfabricmoddingconventions.devClientOptions=/nonexistent build runAllGameTests
```

The conventions plugin manages headless client GameTests. The client-options property prevents it from copying a developer's Prism options into the test instance.

## Preference-file recovery

Unreadable or unsupported-schema `telekinesis-players.json` files are left intact.
Personal commands still apply in memory and report that saving failed. Repair or
restore the file and restart the server before expecting preferences to persist.
