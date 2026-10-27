package com.iiampanda.morph.neoforge;

import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.common.packet.Packets;
import com.iiampanda.morph.common.packet.ServerPackets;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Morph.MOD_ID)
public class MorphNeoForge
{
    public MorphNeoForge(IEventBus modBus)
    {
        Platform.set(new NeoForgePlatform());
        Morph.init("NeoForge 1.21.1");

        modBus.addListener(RegisterEvent.class, event -> event.register(Registries.SOUND_EVENT, helper -> helper.register(Morph.SOUND_ID, Morph.MORPH_SOUND)));
        modBus.addListener(RegisterPayloadHandlersEvent.class, event ->
        {
            PayloadRegistrar reg = event.registrar("1");
            Packets.registerAll(new Packets.Registrar()
            {
                @Override
                public <T extends CustomPacketPayload> void s2c(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec)
                {
                    reg.playToClient(type, codec, (payload, ctx) -> Packets.clientHandler.handle(payload));
                }

                @Override
                public <T extends CustomPacketPayload> void c2s(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec)
                {
                    reg.playToServer(type, codec, (payload, ctx) ->
                    {
                        if(ctx.player() instanceof ServerPlayer sp)
                        {
                            ServerPackets.handle(payload, sp);
                        }
                    });
                }
            });
        });

        IEventBus bus = NeoForge.EVENT_BUS;
        bus.addListener(ServerStartedEvent.class, event -> Morph.onServerStarted(event.getServer()));
        bus.addListener(ServerStoppedEvent.class, event -> Morph.onServerStopped());
        bus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event ->
        {
            if(event.getEntity() instanceof ServerPlayer sp)
            {
                Morph.onJoin(sp);
            }
        });
        bus.addListener(PlayerEvent.PlayerRespawnEvent.class, event ->
        {
            if(event.getEntity() instanceof ServerPlayer sp)
            {
                Morph.onRespawn(null, sp, event.isEndConquered());
            }
        });
        bus.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event ->
        {
            if(event.getEntity() instanceof ServerPlayer sp)
            {
                Morph.onChangeWorld(sp);
            }
        });
        bus.addListener(PlayerEvent.StartTracking.class, event ->
        {
            if(event.getEntity() instanceof ServerPlayer viewer)
            {
                Morph.onStartTracking(event.getTarget(), viewer);
            }
        });
        bus.addListener(EventPriority.LOWEST, false, LivingDeathEvent.class, event ->
        {
            Morph.onLivingDeath(event.getEntity(), event.getSource());
            Morph.onDeath(event.getEntity());
        });
        bus.addListener(PlayerInteractEvent.EntityInteract.class, event ->
        {
            InteractionResult r = Morph.onUseEntity(event.getEntity(), event.getTarget(), event.getHand());
            if(r != InteractionResult.PASS)
            {
                event.setCancellationResult(r);
                event.setCanceled(true);
            }
        });
        bus.addListener(ServerTickEvent.Post.class, event -> Morph.onServerTick(event.getServer()));
        bus.addListener(RegisterCommandsEvent.class, event -> Morph.registerCommands(event.getDispatcher()));
    }
}
