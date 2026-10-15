# Progress

What's done, what's broken, what's next. Newest entry first. Each session adds a row before it
ends.

## Where things stand

- **Done:** the project skeleton (Session 1). Both jars build, both clients reach the main menu
  with Forgemark in the mod list, the settings file is written and checked by the game test.
- **Broken:** nothing known.
- **Next:** Session 2, the quality system with no blocks yet: the Forgemark data component (tier,
  quench, maker's name), bonuses calculated from the tier and the item's base stats, tooltip
  lines, the `/forgemark temper <tier>` test command, the anvil, grindstone and netherite-upgrade
  rules, and game-test checks for each.

## Log

| Date | Session | What happened |
| --- | --- | --- |
| 2026-10-14 | 1, project setup | Skeleton copied from Beyond the Dragon's build setup rather than the MultiLoader Template (the design doc's newer decision): `common` compiled into each loader's jar, the `Platform` service (`Services.PLATFORM.register`, creative tabs), `Config` reading `config/forgemark.properties` with the eight settings from the design doc's settings table, empty mixin config, Fabric and NeoForge entrypoints, the Fabric client game test (settings file written, defaults, typo fallback, unknown keys kept, mod loaded in a new world, one screenshot) and CI (build both jars, run the game test on a virtual display). Versions: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.162.0+26.3, Loom 1.18.4, NeoForge 26.3.0.77, ModDevGradle 2.0.148, Java 25 (Temurin 25.0.4.1 installed). `./gradlew build` green (36 s); `:fabric:runClient` and `:neoforge:runClient` each reach the title screen with no errors in the log and the settings file written; `:fabric:runClientGameTest` passes its 8 checks in 55 s, and fails 3 of them when the bonus-scale default is changed to 90 (proof the checks run; code put back). Lesson: with another mod's game test running on the same machine at the same time, Fabric's harness deadlocked at the title screen twice; noted in CLAUDE.md. Not done: a `forgemark.png` icon, lang file, textures (nothing to show yet). |
