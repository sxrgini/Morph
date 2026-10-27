package com.iiampanda.morph.fabric;

import com.iiampanda.morph.client.HudOverlay;
import com.iiampanda.morph.client.MorphClient;
import com.iiampanda.morph.common.packet.Packets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class MorphFabricClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        MorphClient.init();
        for(KeyMapping key : MorphClient.createKeys())
        {
            KeyMappingHelper.registerKeyMapping(key);
        }
        register(Packets.SyncMorph.TYPE);
        register(Packets.SyncData.TYPE);
        register(Packets.SyncBiomass.TYPE);
        register(Packets.Acquire.TYPE);

        ClientTickEvents.END_CLIENT_TICK.register(MorphClient::tick);
        HudElementRegistry.addLast(net.minecraft.resources.Identifier.fromNamespaceAndPath("morph", "hud"), HudOverlay::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MorphClient.onDisconnect());
    }

    private static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type)
    {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, ctx) -> Packets.clientHandler.handle(payload));
    }
}
