package com.iiampanda.morph.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.file.Path;

public final class Platform
{
    public interface Impl
    {
        Path configDir();

        void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

        void sendToTracking(Entity entity, CustomPacketPayload payload);

        void sendToServer(CustomPacketPayload payload);
    }

    private static Impl impl;

    private Platform() {}

    public static void set(Impl i)
    {
        impl = i;
    }

    public static Path configDir()
    {
        return impl.configDir();
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload)
    {
        impl.sendToPlayer(player, payload);
    }

    public static void sendToTracking(Entity entity, CustomPacketPayload payload)
    {
        impl.sendToTracking(entity, payload);
    }

    public static void sendToServer(CustomPacketPayload payload)
    {
        impl.sendToServer(payload);
    }
}
