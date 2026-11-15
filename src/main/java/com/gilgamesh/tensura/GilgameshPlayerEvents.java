package com.gilgamesh.tensura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
        "gilgamesh_tensura_axe_created"
    };

    private GilgameshPlayerEvents() {}

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
