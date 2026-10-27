package com.iiampanda.morph.fabric;

import com.iiampanda.morph.platform.Platform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.file.Path;

public class FabricPlatform implements Platform.Impl
{
    @Override
    public Path configDir()
    {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload)
    {
        ServerPlayNetworking.send(player, payload);
    }

    @Override
    public void sendToTracking(Entity entity, CustomPacketPayload payload)
    {
        for(ServerPlayer p : PlayerLookup.tracking(entity))
        {
            ServerPlayNetworking.send(p, payload);
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload)
    {
        ClientPlayNetworking.send(payload);
    }
}
