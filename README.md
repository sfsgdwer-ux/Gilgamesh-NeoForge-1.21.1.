# Gilgamesh, Lord of Treasures — v1.5.0-dev
**Minecraft 1.21.1 / NeoForge 21.1.265 — Experimental game mod**

This is an original reimplementation inspired by the Gilgamesh feature list on the TRMysticism wiki, written against **ManasCore 4.0.0.2** and **Tensura: Reincarnated 2.0.1.4**.
The mod registers a native `gilgamesh_tensura:gilgamesh` Ultimate Skill with **2,000 mastery points**.

**Does not require KubeJS or TensurJS.** The JAR does not contain Tensura or ManasCore classes.

## 8 skill modes (development status)
1. **Territory Creation**: hold to claim a persistent 250 x 250 block region, once per player; deducts 1,000,000 **Max MP**. Every 3 seconds restores health and current MP for the owner and online teammates in the area. Persistence across player death is not yet validated; does not yet apply to subordinate NPCs.
2. **Enkidu**: press to bind the living entity in the crosshair for 240 seconds, 480 after mastery; cost 50,000 MP and 20-second cooldown. Immobilizes the entity and uses ManasCore's activation event to block new skill activation while bound. May have gaps for skills already held or for other mods' abilities.
3. **King's Storehouse**: opens a real **5-column x 4-row custom menu** with 20 weapon slots. One item per slot, player-specific NBT storage. Only a heuristic set of modded weapon items is accepted (name/type detection).
4. **Gate of Babylon — Direct Fire**: uses weapons copied from Storehouse, aiming where the player looks. 5–10 gates based on mastery, initial MP cost 25,000 and additional 5,000 MP per held second. Releases 16 items simultaneously per wave with a stable direction.
5. **Gate of Babylon — Surround Attack**: 7–12 gates placed in a ring around a world position selected on activation; portal volleys point towards the center. This is a preliminary target-selection algorithm.
6. **Ea: Sword of Rupture**: available after 2,000 mastery; subtracts 1,000,000 **Max AP** and grants a special item with base attack modifier ~200. The full Genesis damage type and anti-healing logic are **not** implemented.
7. **Create Axe**: grants a one-time special game item (attack modifier ~82), 1,000,000 MP. Marked unbreakable. One-time flag may reset after death until persistent player clone handling is verified.
8. **Enuma Elish**: held, requires Ea equipped. Costs 100,000 MP/sec, emits a beam-like visual and applies 2,000 generic magic damage/sec to a target, while draining 0.05% of its Max MP. True Genesis damage type still missing.

**Golden Rule**: Luck 5 (amplifier 4) reapplied while the skill is ticking.

## Differences from the Wiki
- Evolution from **Gatekeeper** and the **750,000 MP acquisition mechanism** have not been connected to Mysticism progression; use `/gilgamesh grant` in a test world.
- Territory Infamy/Disrepute branches, complete infinite energy regeneration for allied NPCs, and special Genesis/anti-healing rules need further work.
- Unverified in an actual Minecraft client. A passing Gradle build does not confirm that the menu, animation or skill hooks work in-game.

## Installation
Follow `INSTALL_TH.md` in the GitHub Actions artifact. Remove older versions of `gilgamesh_tensura` before installing this version.
