package com.gilgamesh.tensura;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record HoldPacket(boolean pressed) implements CustomPacketPayload {
    public static final Type<HoldPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(GilgameshMod.MODID, "hold"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HoldPacket> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.BOOL, HoldPacket::pressed, HoldPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    @EventBusSubscriber(modid = GilgameshMod.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE, STREAM_CODEC, (data, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    GoldenGates.hold(player, data.pressed());
                }
            });
        }
    }
}
