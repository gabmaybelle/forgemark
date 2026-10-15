# Changelog

## Unreleased

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
