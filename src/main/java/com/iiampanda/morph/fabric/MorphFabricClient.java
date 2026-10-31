package com.iiampanda.morph.fabric;

import com.iiampanda.morph.client.HudOverlay;
import com.iiampanda.morph.client.MorphClient;
import com.iiampanda.morph.common.packet.Packets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
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
            KeyBindingHelper.registerKeyBinding(key);
        }
        register(Packets.SyncMorph.TYPE);
        register(Packets.SyncData.TYPE);
        register(Packets.SyncBiomass.TYPE);
        register(Packets.Acquire.TYPE);
        register(Packets.SyncSit.TYPE);
        register(Packets.SyncBucketed.TYPE);
        register(Packets.SyncSheared.TYPE);
        register(Packets.SyncRules.TYPE);

        ClientTickEvents.END_CLIENT_TICK.register(MorphClient::tick);
        HudRenderCallback.EVENT.register(HudOverlay::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MorphClient.onDisconnect());
    }

    private static <T extends CustomPacketPayload> void register(CustomPacketPayload.Type<T> type)
    {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, ctx) -> Packets.clientHandler.handle(payload));
    }
}
