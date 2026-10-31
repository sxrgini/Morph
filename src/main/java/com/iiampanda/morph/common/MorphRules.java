package com.iiampanda.morph.common;

import net.minecraft.world.level.GameRules;

public final class MorphRules
{
    public static GameRules.Key<GameRules.BooleanValue> INVENTORY;
    public static GameRules.Key<GameRules.BooleanValue> INTERACT;

    public static volatile boolean clientInventory = true;
    public static volatile boolean clientInteract = true;
    private static final java.util.Map<java.util.UUID, Integer> LAST = new java.util.HashMap<>();

    private MorphRules() {}

    private static boolean morphed(net.minecraft.world.entity.player.Player p)
    {
        return com.iiampanda.morph.api.MorphApi.getMorphInfo(p).isMorphed();
    }

    public static boolean inventoryAllowed(net.minecraft.world.entity.player.Player p)
    {
        if(!morphed(p))
        {
            return true;
        }
        return p.level().isClientSide() ? clientInventory : rule(p, true);
    }

    public static boolean interactAllowed(net.minecraft.world.entity.player.Player p)
    {
        if(!morphed(p))
        {
            return true;
        }
        return p.level().isClientSide() ? clientInteract : rule(p, false);
    }

    public static void forget(net.minecraft.server.level.ServerPlayer p)
    {
        LAST.remove(p.getUUID());
    }

    public static void tick(net.minecraft.server.MinecraftServer server)
    {
        if(server.getTickCount() % 20 != 0)
        {
            return;
        }
        for(net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers())
        {
            boolean inv = rule(p, true);
            boolean interact = rule(p, false);
            int v = (inv ? 1 : 0) | (interact ? 2 : 0);
            Integer old = LAST.get(p.getUUID());
            if(old == null || old != v)
            {
                LAST.put(p.getUUID(), v);
                com.iiampanda.morph.platform.Platform.sendToPlayer(p, new com.iiampanda.morph.common.packet.Packets.SyncRules(inv, interact));
            }
        }
    }

    private static boolean rule(net.minecraft.world.entity.player.Player p, boolean inventory)
    {
        if(!(p.level() instanceof net.minecraft.server.level.ServerLevel level) || INVENTORY == null)
        {
            return true;
        }
        return level.getGameRules().getBoolean(inventory ? INVENTORY : INTERACT);
    }
}
