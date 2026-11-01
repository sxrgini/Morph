package com.iiampanda.morph.common;

import net.minecraft.world.level.GameRules;

public final class MorphRules
{
    public static GameRules.Key<GameRules.BooleanValue> INVENTORY;
    public static GameRules.Key<GameRules.BooleanValue> INTERACT;
    public static GameRules.Key<GameRules.BooleanValue> CRAFTING;
    public static GameRules.Key<GameRules.BooleanValue> HOTBAR;

    public static volatile boolean clientInventory = true;
    public static volatile boolean clientInteract = true;
    public static volatile boolean clientCrafting = true;
    public static volatile boolean clientHotbarOnly = false;
    public static volatile int clientSlots = 9;
    public static volatile int clientNormalTicks = 0;
    public static volatile int clientCooldownTicks = 0;

    public static void clientTimers()
    {
        if(clientNormalTicks > 0)
        {
            clientNormalTicks--;
        }
        if(clientCooldownTicks > 0)
        {
            clientCooldownTicks--;
        }
    }
    private static final java.util.Map<java.util.UUID, Long> LAST = new java.util.HashMap<>();

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
        return p.level().isClientSide() ? clientInventory : rule(p, 0);
    }

    public static boolean interactAllowed(net.minecraft.world.entity.player.Player p)
    {
        if(!morphed(p))
        {
            return true;
        }
        return p.level().isClientSide() ? clientInteract : rule(p, 1);
    }

    public static boolean craftingAllowed(net.minecraft.world.entity.player.Player p)
    {
        if(!morphed(p))
        {
            return true;
        }
        return p.level().isClientSide() ? clientCrafting : rule(p, 2);
    }

    public static boolean hotbarOnly(net.minecraft.world.entity.player.Player p)
    {
        if(!morphed(p))
        {
            return false;
        }
        return p.level().isClientSide() ? clientHotbarOnly : rule(p, 3);
    }

    public static int hotbarSlots(net.minecraft.world.entity.player.Player p)
    {
        if(!hotbarOnly(p))
        {
            return 9;
        }
        int n = p.level().isClientSide() ? clientSlots : serverSlots(p);
        return Math.max(1, Math.min(9, n));
    }

    private static int serverSlots(net.minecraft.world.entity.player.Player p)
    {
        net.minecraft.world.entity.LivingEntity e = com.iiampanda.morph.api.MorphApi.getMorphInfo(p).getEntity();
        if(e == null)
        {
            return 9;
        }
        return com.iiampanda.morph.common.mob.MobProfileManager.hotbarSlots(e);
    }

    public static boolean slotAllowed(net.minecraft.world.entity.player.Player p, int menuIndex)
    {
        if(menuIndex < 0)
        {
            return true;
        }
        if(menuIndex <= 4)
        {
            return craftingAllowed(p);
        }
        if(menuIndex >= 36 && menuIndex <= 44)
        {
            return menuIndex - 36 < hotbarSlots(p);
        }
        if(menuIndex <= 45)
        {
            return !hotbarOnly(p);
        }
        return true;
    }

    public static boolean blockClick(net.minecraft.server.level.ServerPlayer p, int containerId, int slotNum, String type, int button)
    {
        if(containerId != 0)
        {
            return false;
        }
        if(!inventoryAllowed(p))
        {
            return true;
        }
        boolean hotbar = hotbarOnly(p);
        boolean craft = craftingAllowed(p);
        if(!hotbar && craft)
        {
            return false;
        }
        switch(type)
        {
            case "QUICK_MOVE":
                return hotbar || !slotAllowed(p, slotNum);
            case "PICKUP_ALL":
                return true;
            case "SWAP":
                if(button == 40)
                {
                    return !slotAllowed(p, 45) || !slotAllowed(p, slotNum);
                }
                return !slotAllowed(p, slotNum) || !slotAllowed(p, 36 + button);
            case "CLONE":
                return false;
            default:
                return slotNum >= 0 && !slotAllowed(p, slotNum);
        }
    }

    public static boolean recipeAllowed(net.minecraft.server.level.ServerPlayer p)
    {
        return craftingAllowed(p);
    }

    public static void forget(net.minecraft.server.level.ServerPlayer p)
    {
        LAST.remove(p.getUUID());
    }

    public static void tick(net.minecraft.server.MinecraftServer server)
    {
        if(server.getTickCount() % 5 != 0)
        {
            return;
        }
        for(net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers())
        {
            enforce(p);
            sync(p);
        }
    }

    private static void enforce(net.minecraft.server.level.ServerPlayer p)
    {
        if(!morphed(p))
        {
            return;
        }
        if(!recipeAllowed(p))
        {
            boolean any = false;
            for(int i = 1; i <= 4; i++)
            {
                any |= p.inventoryMenu.getSlot(i).hasItem();
            }
            if(any)
            {
                p.inventoryMenu.removed(p);
                p.inventoryMenu.broadcastChanges();
            }
        }
        int n = hotbarSlots(p);
        if(n < 9 && p.getInventory().selected >= n)
        {
            p.getInventory().selected = 0;
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        }
    }

    public static void sync(net.minecraft.server.level.ServerPlayer p)
    {
        boolean inv = rule(p, 0);
        boolean interact = rule(p, 1);
        boolean craft = rule(p, 2);
        boolean hotbar = rule(p, 3);
        int slots = morphed(p) ? serverSlots(p) : 9;
        int normalTicks = 0;
        int cooldownTicks = 0;
        if(com.iiampanda.morph.common.morph.MorphManager.mode() instanceof com.iiampanda.morph.common.mode.OneMorphMode)
        {
            com.iiampanda.morph.common.save.PlayerData pd = com.iiampanda.morph.common.morph.MorphManager.data(p);
            long now = com.iiampanda.morph.common.morph.OneMorph.now(p);
            normalTicks = pd.oneNormalUntil > now ? (int)Math.min(Integer.MAX_VALUE, pd.oneNormalUntil - now) : 0;
            cooldownTicks = pd.oneCooldownUntil > now ? (int)Math.min(Integer.MAX_VALUE, pd.oneCooldownUntil - now) : 0;
        }
        long v = (inv ? 1 : 0) | (interact ? 2 : 0) | (craft ? 4 : 0) | (hotbar ? 8 : 0) | ((long)slots << 8);
        Long old = LAST.get(p.getUUID());
        if(old == null || old != v)
        {
            LAST.put(p.getUUID(), v);
            com.iiampanda.morph.platform.Platform.sendToPlayer(p, new com.iiampanda.morph.common.packet.Packets.SyncRules(inv, interact, craft, hotbar, slots, normalTicks, cooldownTicks));
        }
    }

    public static void clampClient(net.minecraft.world.entity.player.Player p)
    {
        int n = hotbarSlots(p);
        int sel = p.getInventory().selected;
        if(n >= 9 || sel < n)
        {
            clientLastSel = sel;
            return;
        }
        int target;
        if(sel == n && clientLastSel == n - 1)
        {
            target = 0;
        }
        else if(sel == 8 && clientLastSel == 0)
        {
            target = n - 1;
        }
        else
        {
            target = clientLastSel >= 0 && clientLastSel < n ? clientLastSel : 0;
        }
        p.getInventory().selected = target;
        clientLastSel = target;
    }

    private static int clientLastSel = 0;

    private static boolean rule(net.minecraft.world.entity.player.Player p, int which)
    {
        if(!(p.level() instanceof net.minecraft.server.level.ServerLevel level) || INVENTORY == null)
        {
            return which != 3;
        }
        GameRules.Key<GameRules.BooleanValue> r = which == 0 ? INVENTORY : which == 1 ? INTERACT : which == 2 ? CRAFTING : HOTBAR;
        return level.getGameRules().getBoolean(r);
    }

}
