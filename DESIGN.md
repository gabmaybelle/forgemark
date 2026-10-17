# Forgemark design

Oct 14, 2026 · Gabby

## Overview

Forgemark lets players temper the tools, weapons and armour they already have, from any mod, at a hearth and anvil for a small, capped quality bonus. It targets Minecraft 26.3 on Fabric and NeoForge.

It fills a gap: no forging mod has a Fabric 26.3 version, and existing ones either replace crafting (slow, conflicts with other mods) or need add-ons per mod. Forgemark leaves crafting alone, so it works beside Mythic Metals, vanilla gear and anything else with a repair material.

Audience: friends' survival servers first, then modpack makers.

## Design goals

Every feature has to pass these four tests.

1. **Compatible.** Works with any mod's gear automatically, with no add-on per mod and no new copies of copper, tin or silver.
2. **Optional.** Normal crafting still works. Forging is a reward for players who enjoy it, never a toll on every pickaxe.
3. **Fair.** Bonuses are small and capped below the next material tier, so a server never tips into overpowered gear.
4. **Satisfying.** Glowing metal, the ring of the anvil, sparks and a hiss on quenching. Learnable by feel, with no outside calculator needed.

## Core loop

One temper takes one matching ingot and five strikes, usually with one reheat, then a quench. About 30 seconds.

&#91;embedded content: forging loop · 4 steps, 2 loops\]

Each heat gives a set number of strikes (iron 3, gold 2, hard metals 4) while the metal still glows; then it goes back to the hearth. A missed strike lowers the final tier but never ruins the item.

## Quality tiers

Each stat has its own cap, so a tempered item never outperforms the next material up: +15% durability, +10% mining speed, +10% attack damage and +0.5 armour toughness. The caps sit below the vanilla gaps; diamond to netherite is about 12% for mining speed and 14% for sword damage.

| Tier | Durability | Mining speed (tools) | Attack damage (weapons) | Toughness (armour) | How it's reached |
| --- | --- | --- | --- | --- | --- |
| Rough | −5% | 0% | 0% | +0 | Several missed strikes |
| Standard | 0% | 0% | 0% | +0 | A few misses; same as untempered |
| Fine | +5% | +3% | +3% | +0.1 | Steady strikes in the wide zone |
| Superior | +10% | +6% | +6% | +0.2 | Mostly well-timed strikes |
| Masterwork | +15% | +8% | +8% | +0.4 | A clean sequence in the narrow zone; carries the maker's mark |

**Water quench** trades 3% durability for +2% mining speed or attack damage, or +0.1 armour toughness. With it, a Masterwork item lands exactly on each cap, and its durability ends at +12%.

Toughness is a flat bonus because iron, gold and chain armour have none to scale. Tempering an item again keeps the better of the old and new results.

## Compatibility

The forge reads each item's own repair material, the same one the vanilla anvil uses, so any mod's gear asks for the right ingot without per-mod code. A Mythic Metals adamantite sword asks for adamantite.

- **Loaders:** Fabric and NeoForge from one codebase, two jars.
- **Metals:** recognized through shared item tags such as `c:ingots/iron`. Forgemark adds no ores.
- **Metal behaviour in data files:** heat time and strikes needed per metal, so pack makers and other mods can tune or add metals.
- **Opt-out tag:** items in `forgemark:not_temperable` are skipped, for special gear such as Botania's relics.
- **Enchantments:** untouched. Quality uses no enchantment slots and doesn't multiply enchantment bonuses.
- **Recipe viewers:** JEI, REI and EMI show what each item needs.
- **No repair material:** an item with none can't be tempered unless a data file maps it.

How Forgemark treats vanilla mechanics and popular mods (26.3 availability as of 14 Oct 2026). Players need Forgemark on both client and server, since it adds blocks.

| Mod or mechanic | On 26.3 | How Forgemark handles it |
| --- | --- | --- |
| Vanilla anvil | — | Repairing or combining keeps the better tier |
| Grindstone | — | Keeps the temper; it isn't an enchantment |
| Smithing table (netherite) | — | Tier carries over; bonuses recalculated from netherite |
| Mending | — | Restores durability; tier unchanged |
| [Beyond the Dragon](https://github.com/gabmaybelle/beyond-the-dragon) | In development | Aphelite gear is repaired through a tag, so it's temperable with aphelite ingots; tested with a compatibility flag |
| [Durability Tooltip](https://modrinth.com/mod/durability-tooltip) | Yes | Shows the tempered maximum durability |
| [Legendary Tooltips](https://modrinth.com/mod/legendary-tooltips) | Yes | Forgemark never changes rarity or item names; the tier shows in its own tooltip line |
| [Jade](https://modrinth.com/mod/jade) | Yes | Plugin shows hearth heat and what's on the anvil |
| [JEI](https://modrinth.com/mod/jei), [REI](https://modrinth.com/mod/rei) | Yes | Show what each item needs |
| [EMI](https://modrinth.com/mod/emi), [WTHIT](https://modrinth.com/mod/wthit) | Not yet | Add once they reach 26.3 |
| [Better Combat](https://modrinth.com/mod/better-combat) | Yes | Works through attack attributes |
| [Tiered](https://modrinth.com/mod/tiered), [Apotheosis](https://modrinth.com/mod/apotheosis) | 26.2, 26.1.2 | Their bonuses stack with Forgemark's; a setting turns Forgemark's off, and the mod page explains the overlap |
| [Iris](https://modrinth.com/mod/iris) shaders | Yes | Emissive textures keep hot metal glowing |

## Balance and fairness

Each stat has a ceiling (+15% durability, +10% mining speed and damage, +0.5 toughness), and nothing stacks past it.

- **Capped below the next tier.** No tempered item outperforms the untempered material above it.
- **One temper per item.** Tempering again keeps the better result; bonuses never add together.
- **Separate from enchanting.** Quality and enchantments apply side by side, never multiplied.
- **Fixed cost.** One matching ingot per temper, consumed whether the result is good or bad.
- **No punishing failure.** A missed strike lowers quality; it never destroys the item.
- **Automation tops out early.** A future trip-hammer reaches Fine at most, so the best results need a player.
- **Server control.** Bonus sizes, PvP effects and eligible items are all configurable.

## Player experience

Anyone reaches Fine without frustration; Masterwork rewards practice.

- **Readable minigame.** A marker sweeps across a strike bar. The wide zone is easy; a narrow inner zone is needed for the top tiers. No hidden maths.
- **Feel.** The ingot glows brighter as it heats and dims with each strike. Sparks and an anvil ring on every hit, a hiss and steam on quenching.
- **Clear result.** The tooltip shows the tier and its bonuses. Masterwork items get a tinted name.
- **Maker's mark.** Masterwork items read "Forged by \[player\]". On by default; a setting turns it off.
- **Accessibility.** A no-timing mode reaches the same tiers at a higher ingot cost, which also covers laggy connections.
- **Teaching.** Tooltips on the hearth and anvil explain each step, plus an advancement trail. No wiki needed.

## Server and pack settings

Server settings win over client settings, so everyone on a server plays by the same rules.

| Setting | Default | What it controls |
| --- | --- | --- |
| Bonus scale | 100% | Multiplies every tier's bonus (e.g. 50% halves them) |
| Maximum tier | Masterwork | Highest tier players can reach |
| Affect PvP | On | Whether weapon bonuses apply against players |
| Maker's mark | On | Shows "Forged by \[player\]" on Masterwork items |
| No-timing mode | Allowed | Lets players skip the minigame for a higher cost |
| No-timing cost | 2 ingots | Ingots per temper in no-timing mode |
| Eligible items | Tools, weapons, armour | Which item types can be tempered |
| Bonuses alongside other quality mods | On | Turn off when Tiered or Apotheosis is installed, so bonuses don't stack |

Pack makers also get the opt-out tag and per-metal data files (see Compatibility).

## Version 1 scope

Three new blocks and items, one minigame and one quality system. Everything else waits.

| Piece | Kind | What it does |
| --- | --- | --- |
| Hearth | Block | Lit like a campfire (flint and steel or a fire charge; a shovel puts it out) and heats a matching ingot; glows as it heats |
| Forging anvil | Block | Holds the item and hot ingot; runs the minigame. Separate from the vanilla anvil |
| Smithing hammer | Item | Strikes the anvil; wears with use like any tool |
| Water quench | Vanilla cauldron | Finishes the item and sets its quality |
| Quality data | System | Stores tier, bonuses and maker on the item; shown in the tooltip |
| Settings, tags, data files | System | Server control and pack compatibility |
| Recipe viewer support | Integration | JEI, REI and EMI |

**Done when:** a two-player dedicated server on each loader can temper vanilla gear and at least one other mod's gear (Mythic Metals once it reaches 26.3) through every tier, restart without losing quality, and show no errors in the log.

## Later ideas

None of these are in version 1; each needs its own balance check.

- **Oil quenching:** a toughness option (more durability) beside water's hardness.
- **More item types:** bows, crossbows, shields, tridents, maces and spears.
- **Forging new tools:** an optional mode that forges vanilla tools from scratch.
- **Trip-hammer:** redstone automation that tops out at Fine.
- **Signature alloys:** a few metals that only reach their potential when forged well.
- **Edge wear:** an optional hardcore setting where the bonus fades and tools need re-honing.

## Technical notes

Modern Minecraft stores an item's stats on the item itself, which is what makes universal tempering possible. Verify each point against 26.3 before building on it.

- **Project setup:** copy Beyond the Dragon's build setup (common, fabric and neoforge folders, the `Platform` service, the client game test and its CI). Package `com.gabbymaybelle.forgemark`.
- **Licence:** MIT for code and textures.
- **Matching:** read the item's repair material (its `repairable` data, often a tag) to find the ingot it needs.
- **Storing quality:** a Forgemark data component holding only the tier, quench and maker's name. Nothing else is saved on the item.
- **Applying quality:** calculate bonuses from the stored tier and the item's current base stats whenever they're needed, so netherite upgrades and setting changes stay correct. Confirm the hooks for durability, mining speed and attributes on each loader.
- **Settings:** `config/forgemark.properties`, read like Beyond the Dragon's `Config`. The server's values win, and what tooltips need is sent to players when they join. No extra config library.
- **Minigame:** the server decides each strike's result, with a timing window that tolerates lag. The client only draws the bar.
- **Data files:** per-metal heat time and strike count, loaded like recipes so datapacks can change them; generated by scripts in `tools/`.
- **Text and colour:** translation keys for all text; colour-blind-safe strike bar colours.
- **Testing:** the Fabric client game test (tier maths, save and reload, netherite upgrade), with `-PtestX` flags for compatibility mods; the NeoForge server load check; a two-player dedicated server by hand for the minigame.

## Open questions

- [x] Heat source: the hearth is lit on its own, like a campfire (changed 2026-10-16 from "sits over a campfire, lava or magma"); no fuel cost
- [x] Strikes: five per temper, usually one reheat
- [x] Re-tempering keeps the better result
- [x] Caps: +10% mining speed and damage, +0.5 flat toughness for armour
- [x] Water quench: −3% durability for +2% main stat, inside the caps
- [ ] Playtest axes: a tempered iron axe nearly matches a diamond axe's damage (both are 9 in vanilla)
- [ ] Hearth, anvil and hammer textures (your pixel art)
- [ ] Publishing: read Modrinth's AI-content rule before planning a Modrinth release
