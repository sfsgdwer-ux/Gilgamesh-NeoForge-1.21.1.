package com.gilgamesh.tensura;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.registries.RegisterEvent;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import dev.architectury.event.EventResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Golden Gates: independent NeoForge recreation, not TRMysticism's Forge class. */
@Mod(GilgameshMod.MODID)
public final class GilgameshMod {
    public static final String MODID = "gilgamesh_tensura";
    public static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath(MODID, "gilgamesh");
    public static final ResourceKey<DamageType> GENESIS_DAMAGE =
        ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(MODID, "genesis"));
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<Item> GATE_FRAME = ITEMS.registerSimpleItem("gate_frame", new Item.Properties());
    // Invisible as inventory tools, visible only as ItemDisplay 3D energy assets.
    public static final DeferredItem<Item> ENUMA_BEAM_CORE =
        ITEMS.registerSimpleItem("enuma_beam_core", new Item.Properties());
    public static final DeferredItem<Item> ENUMA_BEAM_AURA =
        ITEMS.registerSimpleItem("enuma_beam_aura", new Item.Properties());
    public static final DeferredItem<Item> ENUMA_BEAM_RING =
        ITEMS.registerSimpleItem("enuma_beam_ring", new Item.Properties());

    public static final DeferredItem<Item> GILGAMESH_ICON = ITEMS.registerSimpleItem("gilgamesh_icon", new Item.Properties());

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredHolder<MenuType<?>,MenuType<WeaponVaultMenu>> VAULT_MENU =
        MENUS.register("kings_storehouse",()->
            new MenuType<>(WeaponVaultMenu::new,FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredItem<Item> EA_SWORD= ITEMS.register("ea_sword",()->
        new SwordItem(Tiers.NETHERITE,new Item.Properties()
            .attributes(SwordItem.createAttributes(Tiers.NETHERITE,200f,-2.4f))
            .stacksTo(1)));
    public static final DeferredItem<Item> GENESIS_AXE= ITEMS.register("genesis_axe",()->
        new AxeItem(Tiers.NETHERITE,new Item.Properties()
            .attributes(AxeItem.createAttributes(Tiers.NETHERITE,82f,-2.8f))
            .stacksTo(1)));

    public GilgameshMod(IEventBus bus) {
        ITEMS.register(bus);
        MENUS.register(bus);
        // Enkidu seals native skill activations while the spell is in effect.
        SkillEvents.ACTIVATE_SKILL.register((skill, owner, keyNumber, mode) ->
            GilgameshAbilities.isEnkiduBound(owner)
                ? EventResult.interruptFalse() : EventResult.pass());
        bus.addListener(this::registerSkills);
    }

    private void registerSkills(RegisterEvent event) {
        event.register(SkillAPI.getSkillRegistryKey(), SKILL_ID, GilgameshSkill::new);
    }
}
