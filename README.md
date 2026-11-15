# Gilgamesh, Lord of Treasures — v1.6.7 Experimental

**Minecraft Java 1.21.1 · NeoForge 21.1.265 · Native Ultimate Skill**

This GitHub project is an original implementation of Gilgamesh inspired by the TRMysticism Wiki. No KubeJS or TensurJS code or runtime is required. The game must still have Tensura: Reincarnated 2.0.1.4, ManasCore 4.0.0.2, and their normal dependencies.

The Gilgamesh skill is registered under `gilgamesh_tensura:gilgamesh` with **8 modes** and a maximum of 2,000 mastery.

## Modes
| Mode | Implementation status |
|---|---|
| Golden Rule (passive) | Luck 5 effect, refreshed periodically while learned |
| Territory Creation | Persistent 250×250 owner/team-player area, costs 1,000,000 maximum MP, replenishes current MP and health every 3 seconds |
| Enkidu | Golden chain particle helixes, traps a line-of-sight target for 240 seconds (480 mastered), blocks ManasCore skill activation, costs 50,000 MP, 20-second cooldown |
| King's Storehouse | True 5-column, 4-row (20 slot) custom GUI and player-persistent item storage; one weapon per slot |
| Gate of Babylon — Direct Fire | 1–9 portals based on stored weapons, 25,000 MP activation then 5,000 MP/s; up to 16 shots per wave, with spread trajectory and damage from the particular stored item |
| Gate of Babylon — Surround Attack | 1–9 portals in a ring around a ray-selected point or entity; fires inward with a small spread |
| Ea: Sword of Rupture | Requires mastery 2,000; deducts 1,000,000 maximum AP; grants weapon |
| Create Axe | Grants one indestructible axe at cost 1,000,000 MP |
| Enuma Elish | Requires Ea held, costs 100,000 MP/s, raycast 2,000 Genesis damage and drains 0.05% of target's maximum MP per second |

## Ea energy charge effects in v1.6.7
- **Enuma Elish (held)** now displays three red-gold energy particle coils that counter-rotate around an axis near the sword, charging from small to full radius.
- This is a **particle-based rotation effect**, not true mechanical rotation of the third-party Sword of Rupture mesh.
- All MP consumption, beam damage, cooldown and Gate of Babylon gameplay remain unchanged.
- The third-party **188-element downloaded Ea model** and the animated pulse texture for the blade are packaged only in the local distributable JAR, not the public GitHub source, pending confirmation of the model creator's reuse/redistribution license.
- No KubeJS or Tensura KubeJS runtime requirement added.

## Weapon-specific damage and spread volleys in v1.6.4
- Each flying sword or other stored weapon copies its own main-hand attack-damage item modifiers (flat, multiplied base, multiplied total). Vanilla Sharpness level on that particular ItemStack contributes extra damage. No more fixed +2 damage or cap of 250; damages are bounded for numerical safety at 1,000,000.
- Direct Fire targets an area rather than a single intersecting point. The first shot tracks the crosshair; other shots fan left/right and upward/downward. A portal's own 3×3 grid position also spreads its stream so multiple gates do not collapse into one spot.
- Surround Attack distributes impacts around the selected target instead of converging to the exact same point.
- Damage is snapshot per fired projectile and does **not** duplicate all mod-specific on-hit effects, special custom combat systems or conditional enchantment bonuses. Armor and normal game damage rules still affect final received damage.
- Golden Portal 3D V2 was distributed as a separately repacked experimental 1.6.3 JAR, not yet checked into this GitHub source. Gameplay CI compiles the native classes; the separately packaged 3D JAR includes the original 1.6.3 OBJ and vortex assets.
  
## Adaptive 3×3 Golden Gates in v1.6.2
- **Direct Fire**: arrange up to nine portals behind the caster in a three-column, three-row grid with 3.65-block horizontal and 3.45-block vertical spacing to avoid overlap.
- Gate deployment is **based on real weapons stored** in the 20-slot King's Storehouse, not player mastery: 1–3 weapons = 1 portal; 4–5 = 2; 6–7 = 3; then +1 for each two additional weapons (maximum 9 from 18 weapons).
- Starting with one centered gate, add left/right gates, followed by top and bottom gates and their corners. Each gate starts its opening animation 3 ticks after the previous gate, rather than all opening at once.
- Both Direct Fire and Surround Attack obey weapon-count scaling. Surround Attack retains its circular formation but expands the ring to prevent overlap.
- No stored weapons = no portal and **no initial MP charge**. Continuous firing continues through existing portals; the added gates do not reset already-open gates.
- Portal counts can update mid-hold without recreating previous portal entities. The initial wave fires only after the first set has finished opening.

## Persistent Gate of Babylon portals in 1.6.1
- **Direct Fire** and **Surround Attack** open the portals once when the skill key is pressed and keep the same portal display entities visible while it is held.
- Successive weapon waves now fire every 22 ticks through those persistent portals, without restarting the 18-tick opening animation after each volley.
- A mastery increase during the held cast no longer destroys and recreates all portals. The new gate count is applied on the next activation.
- Releasing the key, running out of MP, changing dimension or disconnecting closes and cleans up the portals. MP costs are unchanged.

## Improved in 1.6.0
- **King's Storehouse restore**: suppresses disk writes while loading each saved slot, avoiding partial-state saves.
- **Player clone/death data**: transfers this mod's vault, territory and one-time axe unlock flags when NeoForge clones the player.
- **Gate projectile raycasting**: prevents shots passing through solid blocks, sweeps full movement segments to detect actual entity intersection, and skips team allies.
- **Surround Attack**: locates a crosshair entity or a solid surface rather than always selecting a fixed point 12 blocks ahead.
- **Genesis**: separate damage type for Enuma Elish.
- **Enkidu**: two intertwined gold particle chains. Line-of-sight targeting now stops at blocks.
- **Survival unlock command** `/gilgamesh obtain`: spends 750,000 MP and grants Gilgamesh. The admin test command `/gilgamesh grant` remains.

## Known limitations
- Not yet linked to **Gatekeeper** evolution. `/gilgamesh obtain` provides the 750,000 MP unlock cost but is not a progression integration.
- Enkidu's skill seal is based on ManasCore's native activation event; pre-existing held skills and unrelated mod systems may be unaffected.
- Ea and Axe melee hits do not yet apply the custom Genesis damage type; Enuma Elish does.
- Territory's Infamy/Disrepute branches, ally NPC registration and healing immunity are incomplete.
- A passing Gradle build checks compilation, **not Minecraft runtime behavior**. Back up worlds before testing.
- Some custom mod weapon models may have their own orientation/attributes and need individual adjustments.

See `INSTALL_TH.md` for Thai instructions. GitHub Actions packages a single JAR and this guide.
