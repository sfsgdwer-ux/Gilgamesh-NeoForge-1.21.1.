# Gilgamesh Golden Gates v1.4.0 — Native Tensura Ultimate Skill

**Single NeoForge JAR. No KubeJS, no TensurJS.**

- Minecraft 1.21.1 / NeoForge 21.1.265
- Compile-only dependencies: official Tensura Reincarnated 2.0.1.4 and ManasCore 4.0.0.2
- Registers `gilgamesh_tensura:gilgamesh` directly using NeoForge's `RegisterEvent` and ManasCore `SkillAPI.getSkillRegistryKey()`
- Extends Tensura's `Skill` with `SkillType.ULTIMATE`
- Hooks native skill `onPressed`, `onHeld`, `onRelease`, `onHeldStop` and regular Tensura hotkeys
- Consumes 200 Magicules once when skill starts.
- 5 golden portals following behind the player; up to 16 ItemDisplays in a simultaneous wave.
- Original Ender Chest contents are never consumed.
- For testing, run `/gilgamesh grant` in a world with cheats.
- Removes the separate J-key interaction. Old JS startup script must be removed to avoid duplicate ID registration.

The compiled mod does **not** bundle copies of Tensura or ManasCore.

See `INSTALL_TH.md` for Thai installation and testing steps.

## Build

GitHub Actions runs `gradle clean build` and packages only the JAR and Thai instructions.
Build success does not prove runtime compatibility; test with a backed-up world.
