package com.gilgamesh.tensura;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * A single Ea belongs to its summoner. The initial skill unlock is paid only
 * once; subsequent presses stash / restore that *same* ItemStack.
 *
 * State is stored on the player, not in a static cache, and copied on death.
 * It also protects against manufacturing extra Ea swords when one has been
 * moved out of the inventory into a chest or another mod's inventory.
 */
public final class EaRecall {
    public static final String SAVE_KEY = "gilgamesh_tensura_ea_recall_v1";
    private static final String STORED_KEY = "stored_ea";

    private EaRecall() {}

    private static CompoundTag state(ServerPlayer player) {
        return player.getPersistentData().getCompound(SAVE_KEY);
    }

    private static void save(ServerPlayer player, CompoundTag data) {
        player.getPersistentData().put(SAVE_KEY, data);
    }

    public static boolean isUnlocked(ServerPlayer player) {
        return state(player).getBoolean("unlocked");
    }

    public static void unlock(ServerPlayer player) {
        CompoundTag data = state(player);
        data.putBoolean("unlocked", true);
        data.putBoolean("active", false);
        save(player,data);
    }

    private static int findEa(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++) {
            if(inventory.getItem(i).is(GilgameshMod.EA_SWORD.get())) return i;
        }
        return -1;
    }

    public static boolean hasEa(ServerPlayer player) {
        return findEa(player) >= 0;
    }

    private static int freeSlot(ServerPlayer player,CompoundTag data) {
        Inventory inv=player.getInventory();
        int selected=inv.selected;
        if(selected>=0 && selected<inv.getContainerSize() && inv.getItem(selected).isEmpty())
            return selected;
        int previous=data.getInt("last_slot");
        if(previous>=0 && previous<inv.getContainerSize() && inv.getItem(previous).isEmpty())
            return previous;
        // Inventory.getFreeSlot() searches the main 36 slots.
        int free=inv.getFreeSlot();
        if(free>=0) return free;
        int offhand=inv.getContainerSize()-1;
        if(offhand>=0 && inv.getItem(offhand).isEmpty()) return offhand;
        return -1;
    }

    public static boolean hasRoom(ServerPlayer player) {
        return freeSlot(player,state(player))>=0;
    }

    /**
     * Converts a summoned physical Ea to persistent data. On login or death,
     * the stored NBT keeps the item's enchantments, custom name, and damage.
     */
    public static boolean stash(ServerPlayer player) {
        int slot=findEa(player);
        if(slot<0) return false;
        CompoundTag data=state(player);
        if(data.contains(STORED_KEY,Tag.TAG_COMPOUND)) {
            player.displayClientMessage(Component.literal(
                "Ea is already sealed. Cannot overwrite a stored sword."),true);
            return false;
        }
        Inventory inventory=player.getInventory();
        ItemStack sword=inventory.getItem(slot);
        data.put(STORED_KEY,sword.copyWithCount(1).save(player.registryAccess()));
        data.putBoolean("unlocked",true);
        data.putBoolean("active",false);
        data.putInt("last_slot",slot);
        save(player,data);
        inventory.setItem(slot,ItemStack.EMPTY);
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
        flourish(player);
        player.displayClientMessage(Component.literal("Ea sealed in the Treasury"),true);
        return true;
    }

    /** Handles vanilla toss after the sword leaves the inventory. */
    public static boolean stashDropped(ServerPlayer player,ItemStack dropped) {
        if(!dropped.is(GilgameshMod.EA_SWORD.get())) return false;
        CompoundTag data=state(player);
        if(data.contains(STORED_KEY,Tag.TAG_COMPOUND)) return false;
        data.put(STORED_KEY,dropped.copyWithCount(1).save(player.registryAccess()));
        data.putBoolean("unlocked",true);
        data.putBoolean("active",false);
        save(player,data);
        flourish(player);
        player.displayClientMessage(Component.literal("Ea returned to the Treasury"),true);
        return true;
    }

    /**
     * On the first unlock: create Ea once; on subsequent use: restore the
     * exact saved sword. Never create an additional sword if one may already
     * exist outside the player's inventory.
     */
    public static boolean summon(ServerPlayer player) {
        if(hasEa(player)) return false;
        CompoundTag data=state(player);
        if(!data.getBoolean("unlocked")) return false;
        boolean stored=data.contains(STORED_KEY,Tag.TAG_COMPOUND);
        if(data.getBoolean("active") && !stored) {
            player.displayClientMessage(Component.literal(
                "Ea is still summoned elsewhere. Retrieve the original sword first."),true);
            return false;
        }
        int target=freeSlot(player,data);
        if(target<0) {
            player.displayClientMessage(Component.literal(
                "Make one empty inventory slot to summon Ea."),true);
            return false;
        }
        ItemStack sword=stored
            ? ItemStack.parseOptional(player.registryAccess(),data.getCompound(STORED_KEY))
            : new ItemStack(GilgameshMod.EA_SWORD.get());
        if(!sword.is(GilgameshMod.EA_SWORD.get())) {
            player.displayClientMessage(Component.literal(
                "Stored Ea data is invalid; summon cancelled to protect it."),true);
            return false;
        }
        sword.setCount(1);
        player.getInventory().setItem(target,sword);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        data.remove(STORED_KEY);
        data.putBoolean("active",true);
        data.putInt("last_slot",target);
        save(player,data);
        flourish(player);
        player.displayClientMessage(Component.literal("Ea, Sword of Rupture — Manifest"),true);
        return true;
    }

    /** Retain Ea rather than let it fall to the ground when the owner dies. */
    public static void onDeath(ServerPlayer player) {
        if(hasEa(player)) stash(player);
    }

    private static void flourish(ServerPlayer player) {
        var p=player.position().add(0,1.25,0);
        player.serverLevel().sendParticles(ParticleTypes.END_ROD,
            p.x,p.y,p.z,16,0.26,0.36,0.26,0.035);
    }
}
