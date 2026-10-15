# Tools

Generators for data files (per-metal heat and strike counts, tags, recipes, lang) and for
placeholder textures. Edit the scripts, not their output; the output lands under
`common/src/main/resources`. Nothing here ships in the jars.

- `gen-textures.js`: 16x16 textures for the hearth, forging anvil and hammer, built from the vanilla
  textures they sit beside (bricks, stone, coal, the anvil, the iron ingot and stick). A file
  with the same name in `art/` is copied instead of generated, so Gabby's art always wins.
- `gen-data.js`: the per-metal tags (`heats_quickly`, `heats_slowly`, `soft_metal`,
  `hard_metal`, `heatable`), block states, models, item definitions, loot tables and recipes.
