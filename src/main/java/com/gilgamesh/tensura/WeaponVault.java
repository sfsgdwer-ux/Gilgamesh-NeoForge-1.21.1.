package com.gilgamesh.tensura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;

public final class WeaponVault {
    public static final int CAPACITY=20;
    private static final String KEY="gilgamesh_tensura_vault_v1";

    public static boolean isWeapon(ItemStack item) {
        if(item.isEmpty())return false;
        return item.getItem() instanceof SwordItem
            || item.getItem() instanceof AxeItem
            || item.getItem() instanceof TridentItem
            || item.getItem() instanceof BowItem
            || item.getItem() instanceof CrossbowItem
            || item.getDescriptionId().contains("sword")
            || item.getDescriptionId().contains("axe")
            || item.getDescriptionId().contains("weapon");
    }

    private static void save(ServerPlayer player, SimpleContainer container) {
        ListTag list=new ListTag();
        for(int i=0;i<CAPACITY;i++){
            ItemStack stack=container.getItem(i);
            if(stack.isEmpty())continue;
            CompoundTag entry=new CompoundTag();
            entry.putInt("slot",i);
            entry.put("stack",stack.save(player.registryAccess()));
            list.add(entry);
        }
        CompoundTag root=new CompoundTag();
        root.put("weapons",list);
        player.getPersistentData().put(KEY,root);
    }

    public static SimpleContainer get(ServerPlayer player) {
        CompoundTag root=player.getPersistentData().getCompound(KEY);
        ListTag list=root.getList("weapons",Tag.TAG_COMPOUND);
        // Suppress writes while loading to avoid re-saving a partially restored vault.
        final class PersistentVault extends SimpleContainer {
            private boolean loading=true;
            PersistentVault() { super(CAPACITY); }
            @Override public void setChanged() {
                super.setChanged();
                if(!loading) save(player,this);
            }
            void ready() { loading=false; }
        }
        PersistentVault container=new PersistentVault();
        // Read each entry first to avoid rewriting the saved inventory while loading.
        ItemStack[] loaded=new ItemStack[CAPACITY];
        for(int i=0;i<CAPACITY;i++)loaded[i]=ItemStack.EMPTY;
        for(int j=0;j<list.size();j++){
            CompoundTag entry=list.getCompound(j);
            int slot=entry.getInt("slot");
            if(slot<0||slot>=CAPACITY)continue;
            loaded[slot]=ItemStack.parseOptional(player.registryAccess(),entry.getCompound("stack"));
        }
        for(int i=0;i<CAPACITY;i++) container.setItem(i,loaded[i]);
        container.ready();
        return container;
    }

    public static void open(ServerPlayer player) {
        SimpleContainer container=get(player);
        player.openMenu(new SimpleMenuProvider((id,inventory,viewer)->
            new WeaponVaultMenu(id,inventory,container),
            Component.literal("King's Storehouse")));
    }
}
