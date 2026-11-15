package com.gilgamesh.tensura;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class WeaponVaultScreen extends AbstractContainerScreen<WeaponVaultMenu> {
    public WeaponVaultScreen(WeaponVaultMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title);
        imageWidth=176; imageHeight=204;
        inventoryLabelY=101;
        titleLabelY=6;
    }

    @Override
    protected void renderBg(GuiGraphics gfx,float partial,int mouseX,int mouseY) {
        int x=leftPos,y=topPos;
        gfx.fill(x,y,x+imageWidth,y+imageHeight,0xff181025);
        gfx.fill(x+3,y+3,x+imageWidth-3,y+imageHeight-3,0xff30213f);
        for(int i=0;i<WeaponVault.CAPACITY;i++) {
            int sx=x+43+(i%5)*18,sy=y+18+(i/5)*18;
            gfx.fill(sx,sy,sx+18,sy+18,0xffdfbd54);
            gfx.fill(sx+1,sy+1,sx+17,sy+17,0xff251b2d);
        }
        for(int i=0;i<36;i++){
            int sx=x+7+(i%9)*18;
            int sy=y+(i<27?111+(i/9)*18:169);
            gfx.fill(sx,sy,sx+18,sy+18,0xff76607b);
            gfx.fill(sx+1,sy+1,sx+17,sy+17,0xff24192f);
        }
    }

    @Override
    public void render(GuiGraphics gfx,int mouseX,int mouseY,float partial) {
        super.render(gfx,mouseX,mouseY,partial);
        renderTooltip(gfx,mouseX,mouseY);
    }

    @EventBusSubscriber(modid=GilgameshMod.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void screens(RegisterMenuScreensEvent e) {
            e.register(GilgameshMod.VAULT_MENU.get(),WeaponVaultScreen::new);
        }
    }
}
