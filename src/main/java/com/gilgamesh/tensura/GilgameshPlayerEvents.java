package com.gilgamesh.tensura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The default player persistent-data tag is not automatically guaranteed to
 * survive every player clone / death event. Carry over only this mod's data.
 */
@EventBusSubscriber(modid = GilgameshMod.MODID)
public final class GilgameshPlayerEvents {
    private static final String[] PERSISTED_KEYS = {
        "gilgamesh_tensura_vault_v1",
        "gilgamesh_tensura_territory_v1",
        "gilgamesh_tensura_axe_created",
        EaRecall.SAVE_KEY
    };

    private GilgameshPlayerEvents() {}

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if(event.getEntity() instanceof ServerPlayer player) EaRecall.onDeath(player);
    }

    // ItemTossEvent fires after the hotbar item leaves the inventory;
    // cancelling the spawn and persisting the original stack prevents loss.
    @SubscribeEvent
    public static void onEaToss(ItemTossEvent event) {
        if(event.getPlayer() instanceof ServerPlayer player &&
            EaRecall.stashDropped(player,event.getEntity().getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        CompoundTag previous = event.getOriginal().getPersistentData();
        CompoundTag current = event.getEntity().getPersistentData();
        for (String key : PERSISTED_KEYS) {
            if (current.contains(key)) continue;
            Tag saved = previous.get(key);
            if (saved != null) current.put(key, saved.copy());
        }
    }
}
