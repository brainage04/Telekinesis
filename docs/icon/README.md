# Telekinesis icon

## What this is

`docs/icon/icon.png` — the mod's icon: 1024x1024 PNG, 8-bit RGBA, non-interlaced,
1,399,287 bytes, sha256
`ce455da29766644688936df069b6eef0144121ed2871b9618bc8633b8b299b81`.

## How it was made

Blender render (Blender **5.1.1**, headless CLI, **Cycles on CPU**, 64 samples, 2 fixed
render threads, 1024x1024, `-noaudio`, no display server, no GPU; 89.6 s).

The scene is a reconstruction of a **real generated Minecraft world region** plus vanilla
entity/item models, and it is the only input to the render:

- world `Round3 Pebbles Only`, **Minecraft 1.21.1**, **seed 20260910**, generator
  `minecraft:noise` / `minecraft:overworld`, overworld dimension;
- extracted region (inclusive) x 143..163, y -27..-4, z 101..119, anchor (154, -22, 110);
- mapping Blender (X, Y, Z) = Minecraft (x-154, z-110, y+22), one block = one unit;
- the full per-cell block census, palette and vein data are in `provenance/telekinesis/source-extraction.json`;
- 36 image assets (vanilla block/entity/item textures) are packed inside
  `approved-source.blend`, so no client jar is needed to re-render it.

The displayed tableau — Steve on the left, a zombie and a skeleton behind him, three XP
orbs, an item and an arrow — is a **staged illustrative scene**, not an in-game screenshot.
There is no Minecraft shader pack: the lighting, materials and world are the ones authored
in the approved scene, rendered by Blender Cycles.

The only difference from the approved base scene is the camera, rotated **+10 deg in
azimuth** (0 deg elevation, 1.0x distance) about the look-at point (-0.7, 0, 1.0), plus an
identical `COPY_ROTATION` constraint on the three vanilla XP billboards so they keep facing
the camera. Nothing else changed (non-camera payload sha256
`253650117a3a5405b2072265d3b1b4e4d74dbf659720511cd8d744f06e41c69a`, re-saved payload
`53bff809a3b55122681488b83b51667be747e636a4a4f163d72ecf2db4f74908`).

Camera (Blender coordinates, as rendered):

| parameter | value |
|---|---|
| location XYZ | -0.078834, -2.318222, 3.5 |
| rotation, XYZ Euler degrees | 43.830857, -0.0000015, 15.000002 |
| look-at | -0.7, 0, 1.0 |
| projection | PERSP, 24 mm lens, 36x24 mm sensor (horizontal), FOV 73.740 deg |
| clip | 0.05 .. 150 |

## Provenance files

`provenance/` mirrors the round-3 authoring tree `round3/blender-o/`:

| file | what it is |
|---|---|
| `render_variants.py` | author script: loads the approved scene, rebinds the XP billboards, applies the camera variant, verifies geometry and renders |
| `telekinesis/02-right-sweep.py` | entrypoint for this icon (`main('telekinesis', '02-right-sweep')`) |
| `telekinesis/approved-source.blend` | approved base scene, sha256 `69da3ce9a5de6e81083249820f8213f77a8183d404dbc49566f29db533c3b25d`, textures packed |
| `telekinesis/02-right-sweep.blend` | the rendered scene exactly as Blender saved it (camera variant) |
| `telekinesis/02-right-sweep-metadata.json` | camera, geometry, payload hashes, packed textures, renderer settings, PNG sha256 |
| `telekinesis/02-right-sweep-verification.json` | renderer record, saved camera matrix, projected geometry checks, packed image count (36), PNG sha256 |
| `telekinesis/source-extraction.json` | the world extraction manifest: seed, version, bounds, anchor, coordinate mapping, census, palette, cells |

## How to regenerate

From `docs/icon/provenance` (Blender 5.1.1, CPU Cycles, ~90 s):

```
nix shell nixpkgs#blender --command blender --background -noaudio --python telekinesis/02-right-sweep.py
```

It rewrites `telekinesis/02-right-sweep.{blend,png}`, `-metadata.json` and
`-non-camera.json`; verify with
`sha256sum telekinesis/02-right-sweep.png` (expect the sha256 above).

## Notes

- Deliberately not copied from the round-3 tree: the three non-selected camera candidates
  (`01-left-sweep`, `03-higher`, `04-wider` — scripts, packed scenes, PNGs, metadata,
  verification) and the two ~3 MB non-camera payload dumps
  (`02-right-sweep-non-camera.json`, `02-right-sweep-saved-non-camera.json`); their hashes
  are recorded in the metadata and verification files above.
- `render_variants.py` is shared with a *vein-miner* variant builder from the same round-3
  milestone; that path also needs `blender-o/assets/` and
  `blender-o/vein-miner/source-extraction.json` (not shipped here, not used by this icon).
- The render was made inside a shared CPU cgroup (CPUWeight 20, <=3 cores); timings will
  differ on other machines, pixels should not.

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
