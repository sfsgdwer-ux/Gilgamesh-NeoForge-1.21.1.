# Gilgamesh, Lord of Treasures — v1.6.1 Experimental

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
| Gate of Babylon — Direct Fire | 5–10 portals based on mastery, first cost 25,000 MP then 5,000 MP/s, 16 weapons launched per wave without consuming originals |
| Gate of Babylon — Surround Attack | 7–12 portals in a ring centered on a ray-selected point or entity; fires inward |
| Ea: Sword of Rupture | Requires mastery 2,000; deducts 1,000,000 maximum AP; grants weapon |
| Create Axe | Grants one indestructible axe at cost 1,000,000 MP |
| Enuma Elish | Requires Ea held, costs 100,000 MP/s, raycast 2,000 Genesis damage and drains 0.05% of target's maximum MP per second |

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
