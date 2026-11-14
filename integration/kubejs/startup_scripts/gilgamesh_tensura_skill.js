// Put in kubejs/startup_scripts/. Requires TensurJS, KubeJS, Tensura and this mod.
// Restart Minecraft after editing this file; /reload is not sufficient.
const GoldenGates = Java.loadClass('com.gilgamesh.tensura.GoldenGates')

// True while the player should keep channeling this Ultimate Skill.
function gilgameshChannel(ctx) {
  if (ctx.entity.level().isClientSide()) return true

  // The gate mod is server-authoritative. Initial activation costs 200 magicules.
  // Pulses after activation are free, so holding does NOT drain 200 every tick.
  if (!GoldenGates.isSkillActive(ctx.entity)) {
    if (!ctx.consumeMagicule(200)) return false
  }
  GoldenGates.skillHeld(ctx.entity)
  return true
}

StartupEvents.registry(TensuraKubeJS.SKILL_REGISTRY, event => {
  event.create('gilgamesh_tensura:gilgamesh')
    .name('Gilgamesh, Lord of Treasures')
    .description('Ultimate Skill — Golden Gates. Hold the skill key to open five golden gates and launch copies of items from your Ender Chest in waves of up to 16.')
    .type('ultimate')
    .icon('gilgamesh_tensura:skill/gilgamesh')
    .modes(1)
    .mode(0, 'golden_gates', 'Golden Gates', 'Hold to open five portals and release the weapons in your treasury.')
    .maxHeldTime(72000)
    .shouldReleaseOnInterrupt(ctx => true)
    .onPressed(ctx => { gilgameshChannel(ctx) })
    .onHeld(ctx => gilgameshChannel(ctx))
    .onRelease(ctx => {
      if (!ctx.entity.level().isClientSide()) GoldenGates.skillReleased(ctx.entity)
    })
    .onForget(ctx => {
      if (!ctx.entity.level().isClientSide()) GoldenGates.skillReleased(ctx.entity)
    })
})
