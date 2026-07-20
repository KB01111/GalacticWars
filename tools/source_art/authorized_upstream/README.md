# Authorized upstream character sources

These files are retained as reproducible inputs for
`tools/import_authorized_character_assets.py`. Do not edit the upstream copies in place;
put Galactic Wars transformations in the importer.

## Forge-StarWarsCloneWars

- Repository: `https://github.com/Pyrix25633/Forge-StarWarsCloneWars`
- Revision: `c9555aa4966e9e63c22a59f488d4b05bc614569e`
- License: GPL-3.0
- Additional permission: the project owner explicitly confirmed permission to copy and
  adapt the repository's code, models, and textures for Galactic Wars on 2026-07-20.
- Retained inputs: the Blockbench B1 battle-droid model and its authored entity texture, plus
  the three 501st clone armor layers used to select the Phase II blue palette.

## Galaxies: Parzi's Star Wars Mod

- Repository: `https://github.com/Parzivail-Modding-Team/GalaxiesParzisStarWarsMod`
- Revision: `b91b4cc1a827eeb7c2ae16f0b703affd78c1c206`
- Code license: LGPL-3.0
- Non-code asset license: CC-BY-SA 4.0, as declared in the upstream README.
- Shipped inputs: Clone Trooper and Mandalorian Blockbench models, their corresponding textures,
  and the clone equipment sprites.
- Inspection-only input: the Jedi Commander overlay is retained for source evaluation but is not
  shipped. It is a partial clothing layer tied to the upstream legacy humanoid UV layout and cannot
  be safely pasted onto the current Jedi NPC model.

The distributable conversions remain attributed in `NOTICE.md`,
`docs/authorized-source-intake.md`, and `docs/galacticwars-asset-provenance.md`.
