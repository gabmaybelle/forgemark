# Changelog

## Unreleased

- An advancement trail that teaches the forge: Into the Fire (heat an ingot), Tempered, Hiss (the
  quench), A Steady Hand (a Fine temper), The Patient Way (no-timing mode) and Masterwork.
- The hearth now heats any other mod's ingot or gem by its name, not only ones in the shared
  `c:ingots` and `c:gems` tags, so gear from mods that skip those tags can be tempered too.
  Leather, planks, cobblestone and membrane still can't be heated.
- Works with Beyond the Dragon: aphelite tools and armour temper with an aphelite ingot, which
  counts as a slow, hard metal like netherite.
- Works with Easy Anvils: combining or repairing tempered gear in its anvil keeps the better
  temper, the same as in vanilla's. The Fabric build now wants Fabric API 0.163.0 or newer.
- Apotheosis and Tiered bonuses on a tempered item are left exactly as they are: Forgemark's
  percentages apply to the item's own stats only, so the two never multiply each other.
- Botania's terrasteel counts as a slow, hard metal like netherite: 20 seconds to heat, four
  strikes per heat. Manasteel and elementium behave like iron.
- With JEI or REI installed, a Tempering tab lists every ingot and gem the hearth takes and the
  gear each one tempers, other mods' gear included. Looking up a sword's recipes shows which
  ingot to bring.
- With Jade installed, looking at a hearth shows the ingot and how hot it is, and looking at a
  forging anvil shows what's on it and how many strikes are in.
- A mod icon.
- The forge: a hearth that heats an ingot over a campfire, lava or magma; a forging anvil that
  holds your gear and the hot ingot; and a smithing hammer. Strike when the marker is in the
  zone, five times, with a reheat when the ingot dims, then quench in a water cauldron. Timing
  decides the tier; sneaking while striking skips the timing for a second ingot. Recipes for
  all three.
- Metals behave differently through data files: gold and copper heat quickly, netherite and
  diamond slowly; gold takes fewer strikes per heat, netherite and diamond more. Pack makers
  can put any ingot in the `forgemark:heats_quickly`, `heats_slowly`, `soft_metal` and
  `hard_metal` tags, and `forgemark:heatable` decides what the hearth takes at all.
- The affect-PvP setting works: with it off, a tempered weapon's bonus doesn't apply to hits on
  other players.
- The quality system, without the forge yet: a tempered tool, weapon or piece of armour carries
  a tier (Rough, Standard, Fine, Superior or Masterwork) and gets a small, capped bonus to
  durability, mining speed, attack damage or toughness, worked out from its own stats. The
  tooltip shows the tier, the bonuses and, on Masterwork items, who made it.
- Until the hearth and anvil arrive, admins can temper the item in their hand with
  `/forgemark temper <tier>` (add `quenched` for a water quench) and clear it with
  `/forgemark untemper`.
- The vanilla anvil keeps the better temper when repairing or combining, the grindstone leaves
  it alone, and a netherite upgrade carries it over with the bonuses recalculated.
- Servers decide: the bonus scale, maximum tier and maker's mark settings are sent to players
  when they join. The `forgemark:not_temperable` item tag opts gear out.
- The mod loads on Fabric and NeoForge and writes its settings file (`config/forgemark.properties`).
