package com.gilgamesh.tensura;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Native ManasCore/Tensura registry skill. Does not depend on KubeJS or TensurJS.
 * Holding the regular Tensura activation key channels the existing portal effect.
 */
public final class GilgameshSkill extends Skill {
    private static final double OPENING_MAGICULES = 200.0D;

    public GilgameshSkill() {
        super(SkillType.ULTIMATE);
    }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(
            GilgameshMod.MODID, "icons/skills/gilgamesh");
    }

    @Override
    public boolean shouldTriggerReleaseOnHeldInterrupt(
            ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        return true;
    }

    @Override
    public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (GoldenGates.isSkillActive(player)) return;

        // Opening fee, paid once. Never spend magicules every held tick.
        if (EnergyHelper.isOutOfMagiculeConsuming(player, OPENING_MAGICULES, 0.0D) > 0.0D) return;
        GoldenGates.skillHeld(player);
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity living, int heldTicks, int mode) {
        if (!(living instanceof ServerPlayer player)) return true;
        // Insufficient magicules onPress: deny hold rather than opening for free.
        if (!GoldenGates.isSkillActive(player)) return false;
        GoldenGates.skillHeld(player);
        return true;
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity,
                          int heldTicks, int keyNumber, int mode) {
        GoldenGates.skillReleased(entity);
    }

    @Override
    public void onHeldStop(ManasSkillInstance instance, LivingEntity entity,
                           int heldTicks, int mode) {
        GoldenGates.skillReleased(entity);
    }
}
