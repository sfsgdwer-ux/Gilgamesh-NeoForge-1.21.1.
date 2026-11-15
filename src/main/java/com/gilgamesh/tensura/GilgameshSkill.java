package com.gilgamesh.tensura;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Native Ultimate Skill with 8 configurable modes. No scripting addons. */
public final class GilgameshSkill extends Skill {
    public static final int TERRITORY = 0, ENKIDU = 1, STOREHOUSE = 2,
        DIRECT = 3, SURROUND = 4, EA = 5, AXE = 6, ENUMA = 7;

    public GilgameshSkill() { super(SkillType.ULTIMATE); }

    @Override
    public ResourceLocation getSkillIcon() {
        return ResourceLocation.fromNamespaceAndPath(
            GilgameshMod.MODID, "icons/skills/gilgamesh");
    }

    @Override
    public int getMaxMastery() { return 2000; }

    @Override
    public int getModes(ManasSkillInstance instance) { return 8; }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        return switch (mode) {
            case TERRITORY -> "territory";
            case ENKIDU -> "enkidu";
            case STOREHOUSE -> "storehouse";
            case DIRECT -> "direct";
            case SURROUND -> "surround";
            case EA -> "ea";
            case AXE -> "axe";
            case ENUMA -> "enuma";
            default -> "direct";
        };
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        return Component.translatable("gilgamesh_tensura.skill.mode." + getModeId(instance, mode));
    }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) {
        return Math.floorMod(mode + (reverse ? -1 : 1), 8);
    }

    @Override
    public int getMaxHeldTime(ManasSkillInstance instance, LivingEntity entity) {
        return 72000;
    }

    @Override
    public boolean canTick(ManasSkillInstance instance, LivingEntity entity) { return true; }

    @Override
    public void onTick(ManasSkillInstance instance, LivingEntity entity) {
        GilgameshAbilities.passiveGoldenRule(entity);
    }

    @Override
    public boolean shouldTriggerReleaseOnHeldInterrupt(
            ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        return true;
    }

    private static boolean spend(ServerPlayer player, double amount) {
        if (player.isCreative()) return true;
        boolean success = EnergyHelper.isOutOfMagiculeConsuming(player, amount, 0.0D) <= 0.0D;
        if (!success) player.displayClientMessage(Component.literal("Gilgamesh: insufficient Magicules (" + (long)amount + ")"), true);
        return success;
    }

    private static boolean spendPermanentMagicule(ServerPlayer player, double amount) {
        double maximum=EnergyHelper.getBaseMaxMagicule(player);
        if(maximum<amount) {
            player.displayClientMessage(Component.literal("Requires "+(long)amount+" Max MP"),true);
            return false;
        }
        EnergyHelper.setMaxMagicule(player,maximum-amount);
        return true;
    }

    private static boolean spendPermanentAura(ServerPlayer player, double amount) {
        double maximum=EnergyHelper.getBaseMaxAura(player);
        if(maximum<amount) {
            player.displayClientMessage(Component.literal("Requires "+(long)amount+" Max AP"),true);
            return false;
        }
        EnergyHelper.setMaxAura(player,maximum-amount);
        return true;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int keyNumber, int mode) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (instance.getCoolDown(mode) > 0) return;
        switch(mode) {
            case TERRITORY -> player.displayClientMessage(Component.literal("Hold to establish your territory (one use only)"),true);
            case ENKIDU -> {
                if (!GilgameshAbilities.hasEnkiduTarget(player)) return;
                if (!spend(player, 50_000)) return;
                if (GilgameshAbilities.enkidu(player, instance.getMastery()>=2000)) {
                    instance.setCoolDown(20,mode);
                    addMasteryPoint(instance, player, 3);
                }
            }
            case STOREHOUSE -> {
                WeaponVault.open(player);
                addMasteryPoint(instance, player, 1);
            }
            case DIRECT, SURROUND -> {
                if(GoldenGates.isSkillActive(player)) return;
                int gates=GoldenGates.gatesForStorehouse(player);
                if(gates==0) {
                    player.displayClientMessage(Component.literal(
                        "Gate of Babylon: put weapons in King's Storehouse first"),true);
                    return;
                }
                if (!spend(player,25_000)) return;
                GoldenGates.skillHeld(player,mode==SURROUND,gates);
            }
            case EA -> {
                if(instance.getMastery()<2000) {
                    player.displayClientMessage(Component.literal("Ea requires 2000 Mastery"),true);
                    return;
                }
                if(!spendPermanentAura(player,1_000_000)) return;
                GilgameshAbilities.giveEa(player);
            }
            case AXE -> {
                if(GilgameshAbilities.hasCreatedAxe(player)) return;
                if(!spend(player,1_000_000)) return;
                GilgameshAbilities.giveAxe(player);
                addMasteryPoint(instance,player,5);
            }
            case ENUMA -> {
                if(!GilgameshAbilities.holdsEa(player))
                    player.displayClientMessage(Component.literal("Hold Ea to use Enuma Elish"),true);
            }
            default -> {}
        }
    }

    @Override
    public boolean onHeld(ManasSkillInstance instance, LivingEntity entity, int heldTicks, int mode) {
        if (!(entity instanceof ServerPlayer player)) return true;
        switch(mode) {
            case TERRITORY -> {
                if(heldTicks>=18 && !GilgameshAbilities.hasTerritory(player)) {
                    if(!spendPermanentMagicule(player,1_000_000)) return false;
                    GilgameshAbilities.establishTerritory(player);
                    addMasteryPoint(instance,player,10);
                }
                return !GilgameshAbilities.hasTerritory(player);
            }
            case DIRECT, SURROUND -> {
                if(!GoldenGates.isSkillActive(player)) return false;
                int gates=GoldenGates.gatesForStorehouse(player);
                if(gates==0) {
                    GoldenGates.skillReleased(player);
                    return false;
                }
                if(heldTicks>0 && heldTicks%20==0 && !spend(player,5_000)) {
                    GoldenGates.skillReleased(player);
                    return false;
                }
                GoldenGates.skillHeld(player,mode==SURROUND,gates);
                if(heldTicks%40==0) addMasteryPoint(instance,player,1);
                return true;
            }
            case ENUMA -> {
                if(!GilgameshAbilities.holdsEa(player)) return false;
                if(heldTicks%20==0) {
                    if(!spend(player,100_000)) return false;
                    GilgameshAbilities.enumaElish(player);
                    addMasteryPoint(instance,player,2);
                }
                return true;
            }
            default -> { return false; }
        }
    }

    @Override
    public void onRelease(ManasSkillInstance instance, LivingEntity entity,
                          int heldTicks, int keyNumber, int mode) {
        GoldenGates.skillReleased(entity);
    }
}
