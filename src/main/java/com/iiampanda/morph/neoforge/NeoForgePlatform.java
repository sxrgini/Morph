package com.iiampanda.morph.neoforge;

import com.iiampanda.morph.platform.Platform;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;

public class NeoForgePlatform implements Platform.Impl
{
    @Override
    public Path configDir()
    {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload)
    {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToTracking(Entity entity, CustomPacketPayload payload)
    {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload)
    {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
    }
}
