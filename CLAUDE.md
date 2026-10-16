# Forgemark

A Fabric and NeoForge mod for Minecraft 26.3 and, on the `1.21.1` branch, 1.21.1 (mod ID `forgemark`): temper the tools, weapons and
armour you already have, from any mod, at a hearth and forging anvil, for a small, capped quality
bonus. Crafting is left alone; forging is an optional upgrade. The design doc with every decision
is Gabby's Claude doc "Forgemark design" (exported as `DESIGN.md` for sessions that can't open
it); this file holds what a session needs to work here.

## Layout

The same as Beyond the Dragon's and Cozy Core's: one codebase, two jars.

- `common/`: everything that doesn't care which loader it runs on. `Forgemark.init()` is the one
  entrypoint; `Config` is the settings file; `platform/` is the tiny loader abstraction
  (`Platform`, found through `ServiceLoader`); `mixin/` the Mixins (listed in
  `forgemark.mixins.json`). All data files (recipes, tags, loot, lang, models) live in
  `common/src/main/resources`. Never import Fabric or NeoForge here.
- `fabric/` and `neoforge/`: the entrypoints and a `Platform` each. Their `build.gradle` compiles
  `common`'s sources straight into their jar.
- `fabric/src/gametest/`: the client game test.
- `tools/`: generators for data files (tags, models, recipes, loot, the advancement trail),
  textures and the mod icon. Edit the scripts, not their output.
- `forge/TemperedTrigger` is the mod's one advancement trigger (`forgemark:tempered`), fired
  from the anvil's finish and the quench; the trail's data files are generated.
- `art/`: Gabby's own textures. Never replace them with generated art.

Registering anything (blocks, items, block entities, menus, data components) goes through
`Services.PLATFORM.register(...)`, never `Registry.register` directly: NeoForge only accepts
registrations in its registry event, so its `Platform` queues them and the result is a
`Supplier`. The same goes for creative tabs.

## Build and test

- `./gradlew build`: jars in `fabric/build/libs/` and `neoforge/build/libs/`.
- `./gradlew :fabric:runClientGameTest`: the in-game checks in a real game window. Tempering runs
  through every tier on vanilla gear, then saves, reloads and checks the tier is still there.
  Add `-PtestX` flags to play it with another mod installed (fetched from Modrinth's maven, never
  bundled), the same way Beyond the Dragon does. `-PtestBeyondTheDragon` takes that mod's jar
  from its GitHub release through an ivy repository in `fabric/build.gradle`
  (`beyond_the_dragon_version` is the release tag without the `v`); its checks look items up by
  id and skip when the mod is absent. Planned: Mythic Metals (once it's on 26.3), Tiered.
- Jade, JEI and REI are compile-only dependencies on all three projects (`*_version` in
  `gradle.properties` are Modrinth version ids; REI also needs architectury, cloth-config and
  cloth's `basic-math` from shedaniel's maven). The plugins are `common/.../compat/*Plugin`:
  Fabric finds them through entrypoints in `fabric.mod.json` (`jade`, `jei_mod_plugin`,
  `rei_client`), NeoForge through annotations (REI's lives only in its NeoForge jar, so
  `neoforge/.../compat/ReiNeoForgePlugin` carries it). Keep anything client-only in nested
  classes reached from the client registration methods, so a dedicated server never loads
  the viewers' client classes. `TemperingEntries` (ingot to the items it tempers) is shared by
  JEI and REI and imports neither. Compatibility runs: `-PtestJade`, `-PtestJei`, `-PtestRei`,
  `-PtestEasyAnvils` (the anvil checks open a real anvil block's menu, so a mod that replaces it
  is what's under test).
- NeoForge load check: start a NeoForge server with `neoforge/build/libs/<jar>` and fail on any data or mod error.

## Design rules the code must keep

- **Store the tier, calculate the stats.** The item carries only the Forgemark component: tier,
  quench and maker's name. Bonuses are worked out from the tier and the item's current base stats
  whenever they're needed, so a netherite upgrade or a changed setting is always right. Never
  save stat numbers on the item.
- **Caps, quench included:** +15% durability, +10% mining speed, +10% attack damage, +0.5 armour
  toughness (flat). Exact steps per tier are in the design doc. Modifiers whose id is in another
  quality mod's namespace (`Quality.OTHER_QUALITY_MODS`: apotheosis, tiered) are never scaled.
- **Never change an item's rarity or name.** The tier shows in Forgemark's own tooltip line, so
  tooltip mods that style by rarity keep working.
- **Matching:** the ingot is whatever repairs the item: its `repairable` data, which is often a
  tag (aphelite gear is repaired through `beyond_the_dragon:aphelite_tool_materials`). Skip items
  in `forgemark:not_temperable`. The hearth takes anything in `forgemark:heatable` (vanilla
  plus `c:ingots` and `c:gems`) or whose id ends in `ingot` or `_gem`, so a mod that skips the
  shared tags still works; leather, planks and membrane must never heat. The game test's own
  mod registers a "testium" metal to prove this.
- **Vanilla interactions:** the anvil keeps the better tier when repairing or combining, the
  grindstone keeps the temper, a netherite upgrade carries the tier over.
- **The server decides every strike.** The client draws the bar and sends the press; the server
  checks the timing with a lag-tolerant window and validates every packet.
- **Client code stays client-side.** The minigame screen, tooltips and rendering live in client
  packages reached only from client entrypoints. A dedicated server must never load them.

## Gotchas

- **The common sources are compiled twice**, once per loader. A class that compiles on Fabric but
  not NeoForge (or the reverse) is a common class that leaked a loader import.
- **Mixins** need no refmap on either loader: Minecraft 26.x ships with Mojang names at runtime.
  Prefer loader events; keep any mixin small and say why in a comment.
- **Settings** are `config/forgemark.properties`, read the way Beyond the Dragon's `Config` reads
  its file. The server's values win; anything the client needs for tooltips (bonus scale,
  maximum tier) is sent to players when they join.
- **Stat hooks differ per loader.** Durability, mining speed and attributes are calculated at
  runtime; confirm the hook on each loader in the merged jar and loader sources before writing it.
- Vanilla names changed a lot in 26.x: check the merged jar rather than older docs. Data files
  too: loot entries take `"modifier": [...]` and `"condition": ...`, not the old
  `functions`/`conditions`, and unknown keys are ignored without a warning. Copy the shape from a
  vanilla file in the merged jar before writing a new kind of data file.
- Decompiled 26.3 sources (for reading vanilla, not for copying): ModDevGradle leaves them in
  `~/.gradle/caches/neoformruntime/intermediate_results/mergeWithSources_*_output.jar`.
- **Never teleport the player server-side in the client game test.** After a
  `ServerPlayer.teleportTo(...)`, closing the world deadlocks (render thread halting the server,
  server thread at the harness's tick barrier, test thread waiting on both). Build what the test
  needs in front of where the player already stands, and turn the view with `ctx.runOnClient`.
  Many strikes or finishes in one tick also pile up their particles in the screenshot.
- **One client game test at a time on this machine, and only when it's quiet.** With another
  mod's game test or a heavy build running at the same time, Fabric's test harness can deadlock:
  at the title screen (the test thread never starts) or when a world closes (26.3's integrated
  server halts with a blocking call the harness doesn't cover, so it only works when the
  client's disconnect lands while the server thread is still mid-tick). Either way the window
  goes "not responding" and the log falls silent. Stop the game (`Stop-Process` on the java
  process whose command line has `forgemark` and `client.gametest`), wait for the other work to
  finish, and run again. It's timing, not the mod.

## Gabby's mods: who does what (the same in every repo)

One job per mod, decided 2026-10-15, so nothing is done twice and each repo tells the same story:

- **Cozy Core** (cozy-core): the builder rules, each an ordinary game rule: builds stay built (mob
  explosions, endermen, fire), no phantoms, keep your hotbar on death, the Hearth Lantern, cheap
  wood in the stonecutter, re-dyeing and builder trades. Fabric and NeoForge jars, plus a plain
  data pack of the recipes.
- **Espresso Flavoured Core** (espresso-flavoured): the café: coffee you grow and brew, mugs,
  baristas, Bean Coins, the café charms, and Drowsy (coffee's reason to exist). Longer days stay a
  Core setting for now. The survival rules Cozy Core now covers left Core in 0.4.0 (its PLAN-0.4.0.md).
- **Spawn Gazebo** (spawn-gazebo): the gazebo at the world spawn and its starter chest.
- **Nightfall Clock** (nightfall-clock): the clock above the hotbar and the dusk warning.
- **Pocket List** (pocket-list): the gathering checklist.
- **Not Now, Shield** (not-now-shield): the shield drawn lower in first person; on 26.x also
  tools that change blocks while a shield is in the off-hand.
- **Beyond the Dragon** (beyond-the-dragon): the end-game after the Ender Dragon: ruins at the
  edge of the End, a dragon that comes back harder, and the aphelite metal.
- **Forgemark** (forgemark): tempering the tools, weapons and armour you already have, from any
  mod, for a small capped quality bonus.
- **Espresso Flavoured** (espresso-flavoured-pack): the modpack that puts them together. Since
  pack 0.5.0 (2026-10-15) it carries all five: Cozy Core, Espresso Flavoured Core (back as the café
  once it stopped doing Cozy Core's jobs), Spawn Gazebo, Nightfall Clock and Pocket List.
- **Cozy Core Pack** (cozy-core-pack): the builder-friendly modpack around Cozy Core, with Spawn
  Gazebo, Nightfall Clock, Pocket List and Not Now, Shield.

A new feature goes in the mod whose job it is. If none fits, ask Gabby before adding it anywhere.

## Working on Gabby's mods (the same in every repo)

Gabby makes these mods with help from Claude. Several Claude sessions work on them: one on her
computer and others in the cloud. These rules keep their work fitting together.

**One session per repo at a time.**
- A cloud session works on a `claude/...` branch and never pushes to `main`. Gabby's local
  session reviews the branch, runs its tests and merges it as one commit.
- Start from the latest `main` (`git pull`), and say in the branch's last commit or a short note
  what is finished and what isn't.

**Commits are made one at a time, by hand.** Never commit or push from a chained script. Run
the test, read its result (and the screenshot, for anything visual), then commit that one
branch, then push it. A cherry-pick that conflicts stops the work until it is resolved by hand.
A worldgen or balance change is tested on every terrain in the branch's matrix before it goes
to any other branch. (Learned on Beyond the Dragon, 2026-10-14, when scripted chains committed
a conflicted cherry-pick, put one branch's files on another, and sent an untested ore change to
all three branches.) For a Gradle game test use a game watcher that reports a verdict (in the world, error screen, crashed or hung),
not a wait loop.

**Commits.**
- Author and committer are always `gabmaybelle <211247938+gabmaybelle@users.noreply.github.com>`:
  `git -c user.name=gabmaybelle -c user.email=211247938+gabmaybelle@users.noreply.github.com commit ...`.
  Never a personal email, and never `Claude <noreply@anthropic.com>` as the author. Claude goes in
  a `Co-Authored-By:` line at the end of the message.
- Ask Gabby before committing or pushing when she's around. Finish the build and the tests first.
- Commit messages say what changed for players, in plain sentences.

**Privacy.** These repos are public. Don't add personal email addresses, real names other than
"Gabby", or anything about her other private projects and modpacks: not in code, docs, commits
or test names. Check the diff for these before every push.

**Tests.**
- `./gradlew build` and `./gradlew :fabric:runClientGameTest` must pass before a commit. The game
  test opens a real game window for a minute or two.
- A new or changed check should fail with the feature switched off: run it that way once, then
  put the code back. A check that compares against the setting it tests proves nothing.
- A data change (worldgen, loot, a template, a pack's mod list) gets the same proof: run the part
  of the test that sees it, and quote the numbers in the progress row.
- On Linux without a screen, run game tests under `xvfb-run -a`. 26.3 also needs
  `SDL_VIDEO_FORCE_EGL=1` with `libegl1 libegl-mesa0` installed, or the game hangs at startup.

**Bugs.** Reported through the repo's bug form. One bug per branch: reproduce it, add a check
that fails because of it, fix it, and commit with `Fixes #<number>`. A second bug found along the
way gets its own issue.

**Writing.**
- README and CHANGELOG are for players: plain, friendly, no jargon. New player-facing changes
  go in the CHANGELOG's newest section.
- Keep the credit line: "made by Gabby with help from Claude, an AI assistant".
- Gabby publishes on Modrinth herself. Don't upload anything there.

**Minecraft.** Fabric and NeoForge on Minecraft 26.3 (Mojang names; Loom 1.18.4, ModDevGradle
2.0, Java 25). The `1.21.1` branch builds for 1.21.1 instead: classic `fabric-loom` with Mojang
mappings, NeoForge 21.1, Java 21, and its own small client game-test harness in
`fabric/src/gametest/.../harness` (Fabric's client game test API doesn't exist for 1.21.1). On
1.21.1 vanilla armour keeps its stats in `ArmorItem.getDefaultAttributeModifiers()`, not the item's
attribute component, so read modifiers through `Quality.modifiers`. `tools/gen-data.js` has a
`FORMAT` switch for the data files: '1.21.1' on that branch, '26.3' on main. Vanilla names changed a lot in 26.x: check the merged jar rather than older docs.
