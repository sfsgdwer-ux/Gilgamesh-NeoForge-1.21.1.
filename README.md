# Gilgamesh Golden Gates — Tensura Ultimate Skill bridge v1.3.0

Minecraft 1.21.1 · NeoForge 21.1.265 · Tensura + ManasCore + TensurJS + KubeJS

Golden Gates is now exposed as a **registered Tensura Ultimate Skill** via the documented TensurJS startup registry:
`gilgamesh_tensura:gilgamesh`.

### Install
Download the GitHub Actions artifact and copy its `mods/` and `kubejs/` folders to your Minecraft instance. Follow `INSTALL_TH.md` (Thai).

### Behavior
- Use the Tensura skill key, not a standalone J keybind.
- Uses 200 Magicules once per activation, via `SkillContext.consumeMagicule(200)`.
- On skill hold, sends a heartbeat directly to the NeoForge server-side gate engine.
- On skill release / interruption / skill removal, closes the gates and clears displays.
- 5 gold oval portals behind the owner, synchronized with movement
- 18-tick opening animation, waves of 16 then 11 (for a full 27-slot Ender Chest)
- Models launch on one tick per wave without real item consumption.
- `/gilgamesh count` is informational. Admin activation bypasses were removed.
- Skill icon: in-mod generated gold pixel icon at `gilgamesh_tensura:textures/skill/gilgamesh.png`.

### Technical integration
TensurJS `KubeJSSkillBuilder` supports `type('ultimate')`, `icon()`, `onPressed`, `onHeld`, `onRelease`, and `shouldReleaseOnInterrupt`. A JS startup script calls the public bridge class `com.gilgamesh.tensura.GoldenGates`.

The shipped ZIP is the complete distribution: `mods/*.jar` + `kubejs/startup_scripts/*.js`. Startup registry scripts require a restart.

### Important limits
- Build success validates compilation only. Real Minecraft UI / held-skill behavior is **not tested** in this environment.
- This adds a new Ultimate Skill rather than overriding a possibly installed TRMysticism Gilgamesh skill.
- It uses a visually similar original pixel-art icon, not the exact image that was supplied earlier.
