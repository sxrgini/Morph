package com.iiampanda.morph.common.command;

import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.common.biomass.Upgrades;
import com.iiampanda.morph.common.config.ConfigServer;
import com.iiampanda.morph.common.mob.MobProfileManager;
import com.iiampanda.morph.common.mob.VariantFactory;
import com.iiampanda.morph.common.morph.MorphManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;

public final class CommandMorph
{
    private CommandMorph() {}

    private static SimpleCommandExceptionType error(String key)
    {
        return new SimpleCommandExceptionType(Component.translatable(key));
    }

    public static void register(CommandDispatcher<CommandSourceStack> d)
    {
        d.register(Commands.literal("morph").requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("morph").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("mob", IdentifierArgument.id()).suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
                                .executes(c -> run(c, Action.MORPH, false))
                                .then(Commands.literal("baby").executes(c -> run(c, Action.MORPH, true))))))
                .then(Commands.literal("acquire").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("mob", IdentifierArgument.id()).suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
                                .executes(c -> run(c, Action.ACQUIRE, false))
                                .then(Commands.literal("baby").executes(c -> run(c, Action.ACQUIRE, true))))))
                .then(Commands.literal("forget").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("mob", IdentifierArgument.id()).suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
                                .executes(c -> run(c, Action.FORGET, false))
                                .then(Commands.literal("baby").executes(c -> run(c, Action.FORGET, true))))))
                .then(Commands.literal("demorph").then(Commands.argument("targets", EntityArgument.players()).executes(c ->
                {
                    int n = 0;
                    for(ServerPlayer p : EntityArgument.getPlayers(c, "targets"))
                    {
                        if(MorphManager.morph(p, null, true))
                        {
                            n++;
                        }
                    }
                    int count = n;
                    c.getSource().sendSuccess(() -> Component.translatable("command.morph.demorph", count), true);
                    return n;
                })))
                .then(Commands.literal("biomass").then(Commands.literal("add").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg()).executes(c ->
                        {
                            double amount = DoubleArgumentType.getDouble(c, "amount");
                            for(ServerPlayer p : EntityArgument.getPlayers(c, "targets"))
                            {
                                MorphManager.addBiomass(p, amount);
                                MorphManager.sendData(p);
                            }
                            return 1;
                        })))))
                .then(Commands.literal("reload").executes(c ->
                {
                    Morph.configServer = ConfigServer.load();
                    Morph.setMode(Morph.configServer.morphMode);
                    MobProfileManager.reload();
                    Upgrades.load();
                    for(ServerPlayer p : c.getSource().getServer().getPlayerList().getPlayers())
                    {
                        MorphManager.sendData(p);
                    }
                    c.getSource().sendSuccess(() -> Component.translatable("command.morph.reloaded"), true);
                    return 1;
                }))
                .then(Commands.literal("scan").executes(c ->
                {
                    int n = MobProfileManager.scanAll(c.getSource().getLevel());
                    c.getSource().sendSuccess(() -> Component.translatable("command.morph.scanned", n), true);
                    return n;
                })));
    }

    private enum Action { MORPH, ACQUIRE, FORGET }

    private static int run(CommandContext<CommandSourceStack> c, Action action, boolean baby) throws CommandSyntaxException
    {
        Identifier id = IdentifierArgument.getId(c, "mob");
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow(() -> error("command.morph.error.unknownMob").create());
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        int done = 0;
        for(ServerPlayer p : players)
        {
            Entity e = type.create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if(!(e instanceof LivingEntity living))
            {
                throw error("command.morph.error.notLiving").create();
            }
            MorphVariant v = VariantFactory.create(living);
            if(v == null)
            {
                throw error("command.morph.error.cannotMorph").create();
            }
            if(baby)
            {
                v = v.asBaby(true);
            }
            switch(action)
            {
                case MORPH ->
                {
                    MorphManager.acquire(p, v);
                    if(MorphManager.morph(p, v, true)) done++;
                }
                case ACQUIRE -> { if(MorphManager.acquire(p, v)) done++; }
                case FORGET -> { if(MorphManager.forget(p, v)) done++; }
            }
        }
        int result = done;
        c.getSource().sendSuccess(() -> Component.translatable("command.morph." + action.name().toLowerCase(), result), true);
        return done;
    }
}
