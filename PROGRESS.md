# Progress

What's done, what's broken, what's next. Newest entry first. Each session adds a row before it
ends.

## Where things stand

- **Done:** the project skeleton (Session 1) and the quality system without the blocks
  (Session 2): the `forgemark:temper` component, bonuses worked out from the tier and the item's
  own stats on both loaders, the component tooltip, `/forgemark temper` and `untemper`, the
  anvil, grindstone and netherite-upgrade rules, the settings packet, and 80 game-test checks.
- **Broken:** nothing known.
- **Next:** Session 3, the blocks: the hearth (over a campfire, lava or magma; heats a matching
  ingot found through the item's `repairable` data), the forging anvil and the smithing hammer,
  with the strike minigame decided on the server (the client only draws the bar and sends the
  press) and the water quench in a cauldron. Still open from Session 2: the affect-PvP setting
  (read from the file but not yet applied in damage), per-metal heat and strike data files, and
  a Rough temper on a nearly worn-out item can leave its damage past the new maximum (clamp it
  when tempering).

## Log

| Date | Session | What happened |
| --- | --- | --- |
| 2026-10-14 | 2, quality system | `quality/Tier` (the design table's numbers), `quality/Temper` (the data component: tier, quenched, maker; registered through `Services.PLATFORM.register`; it is its own `TooltipProvider`), `quality/Quality` (the maths: scale, maximum-tier clamp, quench, and the item kinds: a Tool that can break blocks in creative is a tool, so swords count as weapons only; anything with an attack damage modifier gets the damage bonus, pickaxes included, and the tooltip says so). Hooks: one common `ItemStackMixin` on `getMaxDamage`, `getDestroySpeed` and both `forEachModifier` methods (the consumer is wrapped so equip, unequip and the tooltip all see the tempered modifiers; armour with no toughness modifier gets one with Forgemark's own id); the same mixin serves NeoForge because its patched `forEachModifier` still calls the consumer. `AnvilMenuMixin` at the end of `createResult` keeps the better temper from the right slot; the grindstone and smithing table keep components on their own (checked, no code). `ForgemarkCommands`, `network/SettingsPayload` (bonus scale, maximum tier, maker's mark sent on join; `Config.applyFromServer`, `Config.load()` on disconnect), the loaders' command, payload and component-tooltip registrations (Fabric `ItemComponentTooltipProviderRegistry`, NeoForge `RegisterTooltipAppendersEvent`, both before enchantments), client entrypoints on each loader, `en_us.json`, the empty `not_temperable` tag. Game test: tier maths on a diamond pickaxe, sword and chestplate, iron and bare armour, quench and caps, bonus scale and maximum tier, tooltip lines, eligibility, anvil repair and combining, grindstone, netherite upgrade through the real recipe, the command, the settings packet, and a save and reopen (`TestWorldSave.open()`); 80 checks pass in about 2 minutes, 32 fail with the mixin list emptied (code put back). NeoForge server load check green. Lesson: 26.3's diamond sword modifier is +6 (the tooltip adds the player's 1), not the +7 of older versions; check the merged jar for numbers too. |
| 2026-10-14 | 1, project setup | Skeleton copied from Beyond the Dragon's build setup rather than the MultiLoader Template (the design doc's newer decision): `common` compiled into each loader's jar, the `Platform` service (`Services.PLATFORM.register`, creative tabs), `Config` reading `config/forgemark.properties` with the eight settings from the design doc's settings table, empty mixin config, Fabric and NeoForge entrypoints, the Fabric client game test (settings file written, defaults, typo fallback, unknown keys kept, mod loaded in a new world, one screenshot) and CI (build both jars, run the game test on a virtual display). Versions: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, NeoForge 26.3.0.77, ModDevGradle 2.0.148, Java 25 (Temurin 25.0.4.1 installed). `./gradlew build` green (36 s); `:fabric:runClient` and `:neoforge:runClient` each reach the title screen with no errors in the log and the settings file written; `:fabric:runClientGameTest` passes its 8 checks in 55 s, and fails 3 of them when the bonus-scale default is changed to 90 (proof the checks run; code put back). Lesson: with another mod's game test running on the same machine at the same time, Fabric's harness deadlocked at the title screen twice; noted in CLAUDE.md. Not done: a `forgemark.png` icon, lang file, textures (nothing to show yet). |
