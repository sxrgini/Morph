package com.iiampanda.morph.common;

import com.iiampanda.morph.api.MorphApi;
import com.iiampanda.morph.api.mob.trait.TraitHooks;
import com.iiampanda.morph.common.biomass.Upgrades;
import com.iiampanda.morph.common.command.CommandMorph;
import com.iiampanda.morph.common.config.ConfigServer;
import com.iiampanda.morph.common.mob.MobProfileManager;
import com.iiampanda.morph.common.mode.*;
import com.iiampanda.morph.common.morph.MorphManager;
import com.iiampanda.morph.common.packet.Packets;
import com.iiampanda.morph.common.packet.ServerPackets;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

public class Morph
{
    public static final String MOD_ID = "morph";
    public static final String MOD_NAME = "Morph";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static ConfigServer configServer;
    public static MorphMode mode;
    public static final Identifier SOUND_ID = Identifier.fromNamespaceAndPath(MOD_ID, "morph");

    public static final SoundEvent MORPH_SOUND = SoundEvent.createVariableRangeEvent(SOUND_ID);

    public static void init(String loaderName)
    {
        MorphRules.register();
        configServer = ConfigServer.load();
        MobProfileManager.init();
        Upgrades.load();
        setMode(configServer.morphMode);

        MorphApi.setBridge(new MorphApi.Bridge()
        {
            @Override
            public boolean consumeBiomass(Player player, double amount)
            {
                return !(player instanceof ServerPlayer sp) || MorphManager.consumeBiomass(sp, amount);
            }

            @Override
            public boolean hasBiomass(Player player, double amount)
            {
                return !(player instanceof ServerPlayer sp) || !mode.usesBiomass() || MorphManager.data(sp).biomass >= amount;
            }
        });
        LOGGER.info("Morph ({}) by iiAmPanda (original mod by iChun) loaded in {} mode", loaderName, mode.name());
    }

    public static void onServerStarted(MinecraftServer server)
    {
        if(configServer.scanAllMobsOnStartup && configServer.generateMobConfigFiles)
        {
            int n = MobProfileManager.scanAll(server.overworld());
            if(n > 0)
            {
                LOGGER.info("Detected abilities and wrote config files for {} mob types without one", n);
            }
        }
    }

    public static void onServerStopped()
    {
        com.iiampanda.morph.common.morph.BucketHold.clear();
        com.iiampanda.morph.common.morph.OneMorph.clear();
        TraitHooks.active().clear();
    }

    public static void onJoin(ServerPlayer player)
    {
        MorphRules.forget(player);
        MorphManager.onJoin(player);
    }

    public static void onRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive)
    {
        MorphManager.onRespawn(oldPlayer, newPlayer, alive);
    }

    public static void onChangeWorld(ServerPlayer player)
    {
        MorphManager.onChangeWorld(player);
    }

    public static void onStartTracking(Entity tracked, ServerPlayer viewer)
    {
        if(tracked instanceof ServerPlayer morphed)
        {
            MorphManager.syncMorph(morphed, viewer, 1, false);
        }
    }

    public static void onLivingDeath(LivingEntity killed, net.minecraft.world.damagesource.DamageSource source)
    {
        if(killed.level().isClientSide() || killed instanceof net.minecraft.server.level.ServerPlayer)
        {
            return;
        }
        Entity killer = source == null ? null : source.getEntity();
        if(!(killer instanceof net.minecraft.server.level.ServerPlayer))
        {
            killer = killed.getKillCredit();
        }
        if(killer instanceof net.minecraft.server.level.ServerPlayer)
        {
            onKilledOther(killer, killed);
        }
    }

    public static void onKilledOther(Entity killer, LivingEntity killed)
    {
        if(killer instanceof ServerPlayer player)
        {
            MorphManager.onKill(player, killed);
        }
    }

    public static void onDeath(LivingEntity entity)
    {
        if(entity instanceof ServerPlayer player)
        {
            MorphManager.data(player).current = null;
        }
    }

    public static boolean onUseItem(Player player, InteractionHand hand)
    {
        return player instanceof ServerPlayer sp && com.iiampanda.morph.common.morph.BucketHold.useBucket(sp, hand);
    }

    public static InteractionResult onUseEntity(Player player, Entity entity, InteractionHand hand)
    {
        return TraitHooks.onInteractEntity(player, entity, hand);
    }

    public static void onServerTick(MinecraftServer server)
    {
        com.iiampanda.morph.common.morph.BucketHold.tick(server);
        MorphRules.tick(server);
        if(server.getTickCount() % 5 == 0)
        {
            com.iiampanda.morph.common.morph.OneMorph.timers(server);
        }
        com.iiampanda.morph.common.morph.OneMorph.tick();
        if(server.getTickCount() % 10 == 0)
        {
            MorphManager.tickDirtyBiomass(server);
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        CommandMorph.register(dispatcher);
    }

    public static void setMode(ConfigServer.ModeType type)
    {
        switch(type)
        {
            case DEFAULT -> mode = new DefaultMode();
            case COMMAND_ONLY -> mode = new CommandMode(false);
            case COMMAND_ALLOW_SELECTION -> mode = new CommandMode(true);
            case DISGUISE -> mode = new DisguiseMode();
            case ONE_MORPH -> mode = new OneMorphMode();
            default -> mode = new ClassicMode();
        }
    }
}
