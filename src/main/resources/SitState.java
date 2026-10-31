package com.iiampanda.morph.common.morph;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.common.packet.Packets;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class SitState
{
    private static final Set<UUID> SITTING = new HashSet<>();

    private SitState() {}

    public static boolean isSitting(ServerPlayer player)
    {
        return SITTING.contains(player.getUUID());
    }

    public static void set(ServerPlayer player, boolean sit)
    {
        sit = sit && MorphApi.getMorphInfo(player).isMorphed();
        if(sit)
        {
            SITTING.add(player.getUUID());
        }
        else
        {
            SITTING.remove(player.getUUID());
        }

        Packets.SyncSit payload = new Packets.SyncSit(player.getUUID(), sit);
        Platform.sendToPlayer(player, payload);
        Platform.sendToTracking(player, payload);
    }

    public static void resend(ServerPlayer player, ServerPlayer to)
    {
        if(SITTING.contains(player.getUUID()))
        {
            Platform.sendToPlayer(to, new Packets.SyncSit(player.getUUID(), true));
        }
    }

    public static void forget(ServerPlayer player)
    {
        SITTING.remove(player.getUUID());
    }
}
