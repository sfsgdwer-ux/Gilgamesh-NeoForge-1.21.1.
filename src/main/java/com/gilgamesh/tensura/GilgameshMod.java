package com.gilgamesh.tensura;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Golden Gates: independent NeoForge recreation, not TRMysticism's Forge class. */
@Mod(GilgameshMod.MODID)
public final class GilgameshMod {
    public static final String MODID = "gilgamesh_tensura";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<Item> GATE_FRAME = ITEMS.registerSimpleItem("gate_frame", new Item.Properties());

    public GilgameshMod(IEventBus bus) {
        ITEMS.register(bus);
    }
}
