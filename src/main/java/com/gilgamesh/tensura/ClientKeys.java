package com.gilgamesh.tensura;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ClientKeys {
    private static final KeyMapping HOLD = new KeyMapping(
        "key.gilgamesh_tensura.hold", InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_J, "key.categories.gilgamesh_tensura");
    private static int ticks;
    private static boolean last;

    @EventBusSubscriber(modid=GilgameshMod.MODID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
    public static final class Register {
        @SubscribeEvent
        public static void add(RegisterKeyMappingsEvent e) { e.register(HOLD); }
    }

    @EventBusSubscriber(modid=GilgameshMod.MODID, value=Dist.CLIENT)
    public static final class Tick {
        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post e) {
            Minecraft mc = Minecraft.getInstance();
            boolean down = mc.player != null && mc.screen == null && HOLD.isDown();
            ticks++;
            if (mc.player == null) {
                last = false;
                return;
            }
            if (down != last || (down && ticks % 4 == 0)) {
                PacketDistributor.sendToServer(new HoldPacket(down));
                last = down;
            }
        }
    }
}
