# Gilgamesh Golden Gates v1.2.0 for Minecraft 1.21.1 / NeoForge 21.1.265

**Experimental independent recreation**: this is NOT the TRMysticism Ultimate Skill class and does not yet register with the ManasCore skill selection UI.

### Gameplay
- Insert one weapon into each **Ender Chest slot** (27 slots maximum).
- Press and hold **J** (customizable in Options > Controls > Key Binds > Gilgamesh).
- Five large oval gold gate item models appear **behind your character** and follow you.
- A golden opening animation runs for 16 ticks before the first volley.
- Fire up to **16 displayed weapons at once**. All items in that wave spawn on the same server tick.
- With 27 filled slots, the sequence is **16 → 11 → 16 → ...**
- Each visual weapon moves in a straight line, without gravity, at 2.7 blocks/tick; uses the snapshot of the look vector when spawned.
- Releasing the key despawns the gates and active projectiles; if heartbeats are lost, the server cleans up automatically.
- Your real Ender Chest items are never consumed.
- Item models are aligned so +Y points along travel. Some custom mod items have nonstandard built-in model axes.
- To troubleshoot, enter **/gilgamesh count**. Use /gilgamesh on/off for testing. For /gilgamesh on, sending /gilgamesh off turns it off, but keyboard packets also govern the hold state.

### Dependencies
NeoForge 21.1.265 on Minecraft 1.21.1. Original Tensura/ManasCore may remain installed but **this is a standalone keybind implementation**; do not install the old experimental gilgamesh_tensura JAR alongside this one.

### Status
The code was written against 1.21.1 API and requires a successful GitHub Actions build and in-game test before treating it as a working release. Save a backup of your world.

## v1.2.1 improvements
- Sword and axe ItemDisplay orientations use different local hilt-to-head diagonals instead of assuming vertical +Y. Third-party 3D item models may use different axes.
- Swept projectile hitboxes stop the 2.7-block-per-tick displays from skipping entities between tick positions.
- Small end-rod trails emphasize straight-line motion, and portal opening particles have a slightly longer reveal.
- A simple skill-icon-like PNG resource has been bundled as a pixel-art approximation; the original uploaded icon is not yet embedded verbatim.
