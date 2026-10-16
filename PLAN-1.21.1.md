# Plan: Forgemark for Minecraft 1.21.1

Written 2026-10-16. Forgemark is on Minecraft 26.3 for Fabric and NeoForge (`main`). This plan adds
**1.21.1 on Fabric and NeoForge** on a branch of its own, `1.21.1`, the way Beyond the Dragon and
Nightfall Clock carry their older versions.

## Why 1.21.1, and not older

- Most modpacks still play 1.21.1 (NeoForge and Fabric), so it reaches the most players.
- Forgemark keeps an item's temper in a data component. Components exist from 1.20.5 on, so 1.21.1
  can keep the same design. On 1.20.1 the quality system would have to move to NBT, plus a Forge
  version: a rewrite rather than a port. Not planned.
- Every compatibility mod has a 1.21.1 build on both loaders: Jade, JEI, REI and Easy Anvils.

## What changes

Forgemark is about 4,100 lines of Java and uses 105 Minecraft classes. 16 of those don't exist on
1.21.1:

| Area | 26.3 | 1.21.1 |
|---|---|---|
| Names | `Identifier` | `ResourceLocation` |
| Advancement triggers | `advancements.triggers.*` | `advancements.*` |
| Strike bar drawing | `GuiGraphicsExtractor` | `GuiGraphics` |
| Saving block entities | `ValueInput` / `ValueOutput` | `CompoundTag` with a registry lookup |
| Block-entity rendering | render states, `SubmitNodeCollector`, `ItemModelResolver` | `BlockEntityRenderer.render` with `PoseStack` and `MultiBufferSource` |
| Tooltips | `TooltipDisplay` | not needed |
| What repairs an item | the `repairable` component (`Repairable`) | the tool tier's or armour material's repair ingredient |
| Data files | loot `modifier`/`condition`, item definitions in `assets/*/items` | loot `functions`/`conditions`, item models only |

## Steps

### 0. Release the new art on 26.3 first
Done: 0.1.1 is a GitHub pre-release with the real textures. Gallery screenshots still to take.

### 1. Branch and build setup (one session)
- New branch `1.21.1` from `main`, in its own worktree.
- Build setup from Beyond the Dragon's `1.21.1` branch (Loom with Mojang names, ModDevGradle for
  NeoForge 21.1, Java 21), with 1.21.1 versions of Fabric API, NeoForge, Jade, JEI and REI.
- Done when: the project configures and the loader parts compile.

### 2. The mechanical renames (one session)
- `ResourceLocation`, the advancement trigger package, `GuiGraphics` for the strike bar, and
  block entities saving to `CompoundTag`.
- Done when: everything outside step 3 compiles.

### 3. The hard parts (two or three sessions)
1. **Which ingot tempers what.** This is the core rule: the ingot is whatever repairs the item.
   On 1.21.1 that comes from the tool tier or armour material, and mods declare it in several
   ways (tags, single items, custom materials). Test with vanilla gear and other mods' metals.
2. **Showing the item on the hearth and the anvil.** `ForgeRenderers` is rewritten on 1.21.1's
   block-entity renderer.
3. **Tooltips and the stat bonuses.** Check the durability and mining-speed hooks
   (`ItemStackMixin`) and the attribute changes against 1.21.1's code.
4. **Data files.** The generators in `tools/` get a 1.21.1 format switch (as Beyond the Dragon's
   `generate-data.py` has); nothing is edited by hand.
- Done when: both jars build and a dedicated server loads with no data or mod errors.

### 4. Tests (one or two sessions)
- Fabric's client game test doesn't exist for 1.21.1: bring over Beyond the Dragon's small
  harness from its `1.21.1` branch, and Forgemark's checks with it: tempering through every tier,
  saving and reloading, the anvil, the grindstone and the netherite upgrade.
- Compatibility runs with Jade, JEI, REI and Easy Anvils.
- Proof runs: each key check fails with its feature switched off.
- CI for the branch.
- Done when: every check passes on both loaders, and a short hand play.

### 5. Release
- `0.1.1+1.21.1` (or the version `main` is on then): the release checks, the GitHub release, and
  the two files added on Modrinth and CurseForge.

## Size and risks

- About five to eight working sessions for the port, then the release.
- Biggest risk: the "which ingot" rule. Mods on 1.21.1 declare repair materials in different
  ways, so some modded gear might not be recognised at first.
- Every later fix lands on `main` first and is carried to `1.21.1` by hand, where only the
  version differences are edited.
- The game tests open a real game window, so they wait for a quiet machine.
