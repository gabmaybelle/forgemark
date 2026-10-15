# Forgemark

A Fabric and NeoForge mod for Minecraft 26.3. Temper the tools, weapons and armour you already
have, from any mod, for a small quality bonus.

Forgemark leaves crafting alone. Make your gear the usual way, then take it to a hearth and a
forging anvil with one matching ingot: heat the ingot, strike it in time with the glow, and quench
the piece in water. A steady hand earns a Fine temper; a clean run in the narrow zone earns
Masterwork, with your name on it. A missed strike lowers the result but never ruins the item.

Bonuses are small and capped below the next material up, so a tempered iron sword never beats a
plain diamond one: at most +15% durability, +10% mining speed, +10% attack damage, +0.5 armour
toughness. Any mod's gear works as long as it can be repaired with an ingot or gem, with no
add-ons: the hearth knows a metal by its name.

## How to forge

1. **Hearth.** Craft one from bricks and stone and set it over a lit campfire, lava or magma.
   Right-click it with the ingot that repairs your gear (iron for iron, diamond for diamond, and
   so on for any mod's metal). It glows brighter as it heats: about 10 seconds for iron, 6 for
   gold or copper, 20 for netherite or diamond. Right-click with an empty hand to take it out.
2. **Forging anvil.** Craft one from iron. Right-click it with the tool, weapon or armour to lay
   it down, then with the hot ingot.
3. **Strike.** Hold a smithing hammer and look at the anvil: a marker sweeps along a bar. Right-
   click when it's in the blue zone for a good strike, in the orange centre for a perfect one.
   Five strikes make a temper. Each strike cools the ingot; when it dims, take it back to the
   hearth (your strikes are kept) and bring it back hot.
4. **Quench.** Right-click a water cauldron with the finished piece while it still glows for a
   water quench: a little durability traded for a little more edge. Or let it cool as it is.

Five good strikes earn a Fine temper, perfect ones Masterwork with your name on it. Tempering
again keeps the better result. Sneak while striking to skip the timing for a second ingot.

The advancements screen has a Forgemark tab that walks you through these steps. With
[Jade](https://modrinth.com/mod/jade) installed, looking at a hearth or anvil shows what's in it.
With [JEI](https://modrinth.com/mod/jei) or [REI](https://modrinth.com/mod/rei), a Tempering tab
shows which ingot tempers what.

**It's an early release.** The forge works in singleplayer and on servers, and the look is
placeholder art until the real textures land. Jars are on the
[releases page](https://github.com/gabmaybelle/forgemark/releases); the Fabric one needs Fabric API.

## Building

`./gradlew build` makes both jars (`fabric/build/libs/`, `neoforge/build/libs/`).
`./gradlew :fabric:runClientGameTest` plays the in-game test. See `CLAUDE.md` for the layout.

Made by Gabby with help from Claude, an AI assistant. MIT licence.
