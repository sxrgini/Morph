package com.iiampanda.morph.neoforge;

import com.iiampanda.morph.client.HudOverlay;
import com.iiampanda.morph.client.MorphClient;
import com.iiampanda.morph.common.Morph;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Morph.MOD_ID, dist = Dist.CLIENT)
public class MorphNeoForgeClient
{
    public MorphNeoForgeClient(IEventBus modBus)
    {
        MorphClient.init();
        modBus.addListener(RegisterKeyMappingsEvent.class, event ->
        {
            for(KeyMapping key : MorphClient.createKeys())
            {
                event.register(key);
            }
        });
        modBus.addListener(RegisterGuiLayersEvent.class, event ->
                event.registerAboveAll(Identifier.fromNamespaceAndPath(Morph.MOD_ID, "hud"), HudOverlay::render));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> MorphClient.tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> MorphClient.onDisconnect());
    }
}
