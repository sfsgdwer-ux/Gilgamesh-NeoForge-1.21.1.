package com.gilgamesh.tensura;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class WeaponVaultMenu extends AbstractContainerMenu {
    private final Container vault;

    public WeaponVaultMenu(int id,Inventory inventory) {
        this(id,inventory,new SimpleContainer(WeaponVault.CAPACITY));
    }

    public WeaponVaultMenu(int id,Inventory inventory,Container vault) {
        super(GilgameshMod.VAULT_MENU.get(),id);
        this.vault=vault;
        checkContainerSize(vault,WeaponVault.CAPACITY);
        vault.startOpen(inventory.player);
        for(int i=0;i<WeaponVault.CAPACITY;i++) {
            addSlot(new Slot(vault,i,44+(i%5)*18,19+(i/5)*18) {
                @Override
                public boolean mayPlace(ItemStack stack) { return WeaponVault.isWeapon(stack); }
                @Override
                public int getMaxStackSize() { return 1; }
            });
        }
        for(int row=0;row<3;row++)
            for(int col=0;col<9;col++)
                addSlot(new Slot(inventory,col+row*9+9,8+col*18,112+row*18));
        for(int col=0;col<9;col++)
            addSlot(new Slot(inventory,col,8+col*18,170));
    }

    @Override
    public ItemStack quickMoveStack(Player player,int slotIndex) {
        if(slotIndex<0 || slotIndex>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(slotIndex);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack existing=slot.getItem();
        ItemStack original=existing.copy();
        if(slotIndex<WeaponVault.CAPACITY) {
            if(!moveItemStackTo(existing,WeaponVault.CAPACITY,slots.size(),true))return ItemStack.EMPTY;
        } else {
            if(!WeaponVault.isWeapon(existing))return ItemStack.EMPTY;
            if(!moveItemStackTo(existing,0,WeaponVault.CAPACITY,false))return ItemStack.EMPTY;
        }
        if(existing.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) { return vault.stillValid(player); }

    @Override
    public void removed(Player player) {
        super.removed(player);
        vault.stopOpen(player);
    }
}
