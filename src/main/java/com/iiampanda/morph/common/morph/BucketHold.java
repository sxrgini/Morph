package com.iiampanda.morph.common.morph;

import com.iiampanda.morph.common.packet.Packets;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BucketHold
{
    public static final Set<UUID> VIEW = ConcurrentHashMap.newKeySet();

    private record Hold(UUID picker, long since) {}

    private static final Map<UUID, Hold> HELD = new HashMap<>();

    private static final Map<UUID, java.util.List<Integer>> PASSENGERS = new HashMap<>();

    private BucketHold() {}

    private static String key(UUID bucketed)
    {
        return "morph_bucketed_" + bucketed;
    }

    public static void mark(ItemStack stack, UUID bucketed)
    {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(key(bucketed), true));
    }

    private static boolean marked(ItemStack stack, UUID bucketed)
    {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(key(bucketed));
    }

    private static void broadcast(ServerPlayer player, boolean held)
    {
        Packets.SyncBucketed payload = new Packets.SyncBucketed(player.getUUID(), held);
        Platform.sendToPlayer(player, payload);
        Platform.sendToTracking(player, payload);
    }

    public static void start(ServerPlayer bucketed, ServerPlayer picker)
    {
        if(bucketed == picker || HELD.containsKey(picker.getUUID()) || HELD.containsKey(bucketed.getUUID()))
        {
            return;
        }
        com.iiampanda.morph.common.Dbg.log("{} bucketed by {}", bucketed.getName().getString(), picker.getName().getString());
        HELD.put(bucketed.getUUID(), new Hold(picker.getUUID(), bucketed.level().getGameTime()));
        VIEW.add(bucketed.getUUID());
        broadcast(bucketed, true);
        bucketed.setCamera(picker);
        RideBypass.ACTIVE.set(true);
        try
        {
            bucketed.startRiding(picker, true, true);
        }
        finally
        {
            RideBypass.ACTIVE.set(false);
        }
        hide(bucketed);
        bucketed.sendSystemMessage(net.minecraft.network.chat.Component.translatable("morph.message.bucketed"), true);
    }

    private static void hide(ServerPlayer bucketed)
    {
        bucketed.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY, 40, 0, false, false, false));
    }

    private static void resend(ServerPlayer vehicle)
    {
        ((net.minecraft.server.level.ServerLevel)vehicle.level()).getChunkSource().sendToTrackingPlayersAndSelf(vehicle, new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(vehicle));
    }

    private static void end(MinecraftServer server, UUID bucketedId)
    {
        com.iiampanda.morph.common.Dbg.log("bucket hold of {} ends", bucketedId);
        HELD.remove(bucketedId);
        VIEW.remove(bucketedId);
        ServerPlayer bucketed = server.getPlayerList().getPlayer(bucketedId);
        if(bucketed != null)
        {
            bucketed.removeEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
            if(bucketed.getVehicle() instanceof net.minecraft.world.entity.player.Player)
            {
                bucketed.stopRiding();
            }
            if(bucketed.getCamera() != bucketed)
            {
                bucketed.setCamera(bucketed);
            }
            broadcast(bucketed, false);
        }
    }

    private static boolean carries(ServerPlayer picker, UUID bucketed)
    {
        if(marked(picker.containerMenu.getCarried(), bucketed))
        {
            return true;
        }
        for(int i = 0; i < picker.getInventory().getContainerSize(); i++)
        {
            if(marked(picker.getInventory().getItem(i), bucketed))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean useBucket(ServerPlayer picker, InteractionHand hand)
    {
        MinecraftServer server = picker.level().getServer();
        ItemStack stack = picker.getItemInHand(hand);
        com.iiampanda.morph.common.Dbg.log("{} uses item {} (holds: {})", picker.getName().getString(), stack, HELD.size());
        if(server == null || stack.isEmpty())
        {
            return false;
        }
        for(Map.Entry<UUID, Hold> entry : new HashMap<>(HELD).entrySet())
        {
            if(entry.getValue().picker().equals(picker.getUUID()) && marked(stack, entry.getKey()))
            {
                end(server, entry.getKey());
                picker.setItemInHand(hand, picker.getAbilities().instabuild ? ItemStack.EMPTY : new ItemStack(Items.BUCKET));
                return true;
            }
        }
        return false;
    }

    public static void tick(MinecraftServer server)
    {
        Iterator<Map.Entry<UUID, Hold>> it = new HashMap<>(HELD).entrySet().iterator();
        while(it.hasNext())
        {
            Map.Entry<UUID, Hold> entry = it.next();
            ServerPlayer bucketed = server.getPlayerList().getPlayer(entry.getKey());
            ServerPlayer picker = server.getPlayerList().getPlayer(entry.getValue().picker());
            if(bucketed == null)
            {
                HELD.remove(entry.getKey());
                VIEW.remove(entry.getKey());
                continue;
            }
            if(picker == null || !picker.isAlive() || picker.isRemoved() || bucketed.getCamera() != picker || bucketed.getVehicle() != picker || bucketed.isShiftKeyDown() || !carries(picker, entry.getKey()) || bucketed.level().getGameTime() - entry.getValue().since() > 6000L)
            {
                end(server, entry.getKey());
            }
            else
            {
                hide(bucketed);
            }
        }
        for(ServerPlayer v : server.getPlayerList().getPlayers())
        {
            java.util.List<Integer> ids = new java.util.ArrayList<>();
            for(net.minecraft.world.entity.Entity passenger : v.getPassengers())
            {
                ids.add(passenger.getId());
            }
            java.util.List<Integer> before = PASSENGERS.getOrDefault(v.getUUID(), java.util.List.of());
            if(!ids.equals(before))
            {
                resend(v);
                if(ids.isEmpty())
                {
                    PASSENGERS.remove(v.getUUID());
                }
                else
                {
                    PASSENGERS.put(v.getUUID(), ids);
                }
            }
        }
        for(ServerPlayer p : server.getPlayerList().getPlayers())
        {
            if(p.getVehicle() instanceof ServerPlayer vehicle)
            {
                if(p.isShiftKeyDown() && !HELD.containsKey(p.getUUID()))
                {
                    com.iiampanda.morph.common.Dbg.log("{} dismounts {}", p.getName().getString(), vehicle.getName().getString());
                    p.stopRiding();
                    p.connection.teleport(vehicle.getX(), vehicle.getY(), vehicle.getZ(), p.getYRot(), p.getXRot());
                    resend(vehicle);
                }
                else
                {
                    vehicle.positionRider(p);
                    if(server.getTickCount() % 10 == 0)
                    {
                        resend(vehicle);
                    }
                }
            }
        }
    }

    public static void clear()
    {
        HELD.clear();
        VIEW.clear();
        PASSENGERS.clear();
    }
}
