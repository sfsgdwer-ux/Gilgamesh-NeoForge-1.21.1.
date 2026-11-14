package com.gilgamesh.tensura;

import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegisterEvent;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Golden Gates: independent NeoForge recreation, not TRMysticism's Forge class. */
@Mod(GilgameshMod.MODID)
public final class GilgameshMod {
    public static final String MODID = "gilgamesh_tensura";
    public static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath(MODID, "gilgamesh");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<Item> GATE_FRAME = ITEMS.registerSimpleItem("gate_frame", new Item.Properties());

    public static final DeferredItem<Item> GILGAMESH_ICON = ITEMS.registerSimpleItem("gilgamesh_icon", new Item.Properties());

    public GilgameshMod(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(this::registerSkills);
    }

    private void registerSkills(RegisterEvent event) {
        event.register(SkillAPI.getSkillRegistryKey(), SKILL_ID, GilgameshSkill::new);
    }
}
