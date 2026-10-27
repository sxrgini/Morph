package com.iiampanda.morph.fabric;

import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.common.packet.Packets;
import com.iiampanda.morph.common.packet.ServerPackets;
import com.iiampanda.morph.platform.Platform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Fabric entry point. All of the behaviour lives in the shared {@link Morph} class. */
public class MorphFabric implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        Platform.set(new FabricPlatform());
        Morph.init("Fabric 1.21.1");
        Registry.register(BuiltInRegistries.SOUND_EVENT, Morph.SOUND_ID, Morph.MORPH_SOUND);

        Packets.registerAll(new Packets.Registrar()
        {
            @Override
            public <T extends CustomPacketPayload> void s2c(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec)
            {
                PayloadTypeRegistry.playS2C().register(type, codec);
            }

            @Override
            public <T extends CustomPacketPayload> void c2s(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec)
            {
                PayloadTypeRegistry.playC2S().register(type, codec);
                ServerPlayNetworking.registerGlobalReceiver(type, (payload, ctx) -> ServerPackets.handle(payload, ctx.player()));
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(Morph::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> Morph.onServerStopped());
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Morph.onJoin(handler.getPlayer()));
        ServerPlayerEvents.AFTER_RESPAWN.register(Morph::onRespawn);
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> Morph.onChangeWorld(player));
        EntityTrackingEvents.START_TRACKING.register(Morph::onStartTracking);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { Morph.onLivingDeath(entity, source); Morph.onDeath(entity); });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> Morph.onUseEntity(player, entity, hand));
        ServerTickEvents.END_SERVER_TICK.register(Morph::onServerTick);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Morph.registerCommands(dispatcher));
    }
}
