package com.iiampanda.morph.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.nio.file.Path;

/** Everything the shared code needs from the mod loader. Each loader module installs one implementation at start up. */
public final class Platform
{
    public interface Impl
    {
        Path configDir();

        /** Sends a payload to one player. */
        void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

        /** Sends a payload to every player currently tracking (seeing) the entity, not to the entity itself. */
        void sendToTracking(Entity entity, CustomPacketPayload payload);

        /** Client to server. */
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
