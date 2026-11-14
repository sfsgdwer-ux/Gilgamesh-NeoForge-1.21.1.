# Gilgamesh Golden Gates (NeoForge 1.21.1)

**Current status:** Build scaffolding only. The generated JAR does not yet implement the Golden Gates skill. Do **not** replace the existing v1.1.0 mod with it.

The project targets Java 21, Minecraft 1.21.1, NeoForge 21.1.265.

## Planned v1.2.0
- Five gold dimensional gates behind and following the player while the skill is held
- Portals open before a batch launches
- All 1–16 weapons in the current batch launch simultaneously, rotating through 16-slot batches
- Each sword/axe travels in a straight line with its tip leading, not as a spinning thrown item
- Skill icon based on the user-provided image
- Inventory items remain untouched

## Build
Go to Actions > Build Gilgamesh NeoForge > Run workflow; download the JAR artifact if the run succeeds.

## Provenance
The user-provided TRMysticism 1.1.0 binary is a reference, but legacy Forge/GeckoLib 3 classes cannot be loaded directly by NeoForge 1.21.1.
