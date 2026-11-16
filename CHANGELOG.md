# Changelog

## Unreleased

### Changed

- The server-wide switch is now the `telekinesis:enabled` game rule (boolean, on by default) instead of `config/telekinesis.json`. Set it with `/gamerule telekinesis:enabled <true|false>`; it appears in the gamerules screen of a client that also has Telekinesis installed, and a change applies to the next drop with no restart. **A server that had Telekinesis turned off in `telekinesis.json` must run `/gamerule telekinesis:enabled false` after updating**: the old file is not read, so the mod is on until the rule is set.

### Removed

- `config/telekinesis.json`, its loader and the five `/telekinesis config` subcommands (`status`, `enable`, `disable`, `toggle`, `reload`). `/telekinesis status` still reports the server-wide state. The per-player preference and its commands (`/telekinesis enable|disable|toggle`, `telekinesis-players.json`) are unchanged.
