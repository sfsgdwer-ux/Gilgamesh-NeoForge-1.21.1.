# Gilgamesh, Lord of Treasures — v1.7.3 Experimental

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

## Enuma Elish Overdrive + stable projectile flight in v1.7.3
- **Enuma Elish** features an original 12-tick charging stage with three rotating rune rings and crimson/gold particles converging near the source. No beam damage or block breaking occurs before the charge completes.
- While held after charging, a narrow translucent core and aura are joined by **two independently counter-rotating 3D helical crimson/black ribbons**, powered by original generated RGBA textures.
- Two layered, rotating shockwave meshes pulse at the beam's impact endpoint. Particle arcs accent the strike without an opaque white screen flash.
- Native ItemDisplay elements are reused while held, and removed on release/timeout. Beam visuals begin safely forward of the camera, and the old 100,000 MP/second charge remains unchanged.
- **Gate of Babylon axes, pickaxes, shovels and hoes** now have a stable, world-up projectile orientation: their business ends point forward and they do not corkscrew or tumble like thrown items. Swords retain forward flight.
- Keeps the previous stored-weapon damage, adaptive 3×3 gates, Ea seal/recall, and seven skill modes; no KubeJS dependency.
- This is a native-model VFX upgrade, **not** an AAA volumetric light shader. In-game testing of item model orientations and transparency is needed.

## Cinematic, camera-safe Enuma Elish visuals in v1.7.2
- Replaces solid white/glowstone-style rectangular Enuma beam models with **translucent original crimson/amber 3D meshes** and four generated RGBA gradient textures.
- The visible effect begins 4.35 blocks in front of the near-eye collision origin. It is intentionally slender (visual radius max 0.84) so it doesn't obscure the first-person or third-person view.
- Three rings rotate at different speeds, one near the impact, and the red/amber aura breathes slightly.
- Removes screen-filling white FLASH particles, halves ambient particle traffic, and gives the impact fine alternating crimson/gold spark arcs.
- Keeps attack damage, block destruction, MP drain and hold/release behavior from 1.7.1. The visual diameter is smaller than the gameplay hit region.
- Textures are created by a **Gradle task** with Java ImageIO; no Python, OptiFine, shaders, KubeJS or added runtime dependencies.
- The look is a native translucent Minecraft effect, not a full AAA HDR bloom or post-processing shader. Test in-game, especially transparency sorting with other shader mods.

## Ea sword recall and re-summon in v1.7.1
- The first unlock of the **Ea** skill at mastery 2000 charges 1,000,000 maximum AP exactly once. The sword's summon is thereafter a free toggle with the same skill button.
- Press Ea while the sword is in the inventory to **seal the exact ItemStack**, including custom components, name, enchantments and durability into player-persistent NBT. Press Ea again to summon the same sword back to a free hand/inventory slot.
- Ea is automatically sealed on player death; dropping it with Q also stores it without spawning an item. Player clone copies this mod's Ea state.
- Ea items already present when upgrading from an older version are recognized and adopted on first recall without another permanent-AP cost.
- Prevents creation of duplicates when a summoned sword was moved into a chest or another storage mod; bring that original sword back into the inventory to recall it.
- If inventory is full, the stored sword stays safely sealed until a slot is available.
- Other seven modes, 3D Enuma beam, damage, and 3×3 Gate of Babylon behavior remain unchanged.

## Native 3D Enuma Elish VFX in v1.7.0
- **Enuma Elish** now maintains two elongated ItemDisplay models (hot core + red aura) and three independently rotating 3D energy rings while the key is held, adjusting orientation and size to the player's aim. The displays are reused per tick and cleaned on release, MP exhaustion, death/dimension change, or timeout.
- Includes custom original model JSONs in the mod asset directory, with vanilla texture fallbacks. A separate, locally repacked JAR adds higher-detail original red/gold textures and preserves the user's downloaded Ea 3D model without publishing that third-party asset in the repository.
- The underlying hit detection and block break system is still experimental: 320 Genesis damage per 2 ticks on intersected entities, breakable blocks along the beam processed every 5 ticks with a cap of 32 per sweep. Unbreakable blocks and blocks with block entities are excluded. **Back up the world before testing**.
- Existing 100,000 MP per second drain is maintained. Beam objects stop immediately when the key is released.
- **Create Axe mode remains removed** (7 active modes). The registered older axe item may still exist for world compatibility.
- No KubeJS, OptiFine or GeckoLib dependency is required for this effect.

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
