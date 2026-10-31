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

public class MorphFabric implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        Platform.set(new FabricPlatform());
        com.iiampanda.morph.common.MorphRules.INVENTORY = net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry.register("morphCanUseInventory", net.minecraft.world.level.GameRules.Category.PLAYER, net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory.createBooleanRule(true));
        com.iiampanda.morph.common.MorphRules.INTERACT = net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry.register("morphCanInteract", net.minecraft.world.level.GameRules.Category.PLAYER, net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory.createBooleanRule(true));
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
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, level, hand) ->
        {
            return Morph.onUseItem(player, hand) ? net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand)) : net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        });
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> com.iiampanda.morph.common.MorphRules.interactAllowed(player) ? net.minecraft.world.InteractionResult.PASS : net.minecraft.world.InteractionResult.FAIL);
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> com.iiampanda.morph.common.MorphRules.interactAllowed(player) ? net.minecraft.world.InteractionResult.PASS : net.minecraft.world.InteractionResult.FAIL);
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> com.iiampanda.morph.common.MorphRules.interactAllowed(player));
        ServerTickEvents.END_SERVER_TICK.register(Morph::onServerTick);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> Morph.registerCommands(dispatcher));
    }
}
