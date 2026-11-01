package com.iiampanda.morph.common.morph;

import com.iiampanda.morph.api.morph.MorphVariant;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.common.mob.MobProfile;
import com.iiampanda.morph.common.mob.MobProfileManager;
import com.iiampanda.morph.common.mob.VariantFactory;
import com.iiampanda.morph.common.save.PlayerData;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import com.iiampanda.morph.api.mob.trait.SunburnTrait;
import com.iiampanda.morph.api.mob.trait.Trait;
import com.iiampanda.morph.api.mob.trait.WaterBreatherTrait;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class OneMorph
{
    private static List<EntityType<?>> pool;

    private static final Map<String, String[]> STRUCTURES = Map.ofEntries(
            Map.entry("minecraft:villager", new String[]{"minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna", "minecraft:village_snowy", "minecraft:village_taiga"}),
            Map.entry("minecraft:iron_golem", new String[]{"minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna", "minecraft:village_snowy", "minecraft:village_taiga"}),
            Map.entry("minecraft:wandering_trader", new String[]{"minecraft:village_plains", "minecraft:village_desert", "minecraft:village_savanna", "minecraft:village_snowy", "minecraft:village_taiga"}),
            Map.entry("minecraft:piglin_brute", new String[]{"minecraft:bastion_remnant"}),
            Map.entry("minecraft:blaze", new String[]{"minecraft:fortress"}),
            Map.entry("minecraft:wither_skeleton", new String[]{"minecraft:fortress"}),
            Map.entry("minecraft:guardian", new String[]{"minecraft:ocean_monument"}),
            Map.entry("minecraft:elder_guardian", new String[]{"minecraft:ocean_monument"}),
            Map.entry("minecraft:shulker", new String[]{"minecraft:end_city"}),
            Map.entry("minecraft:pillager", new String[]{"minecraft:pillager_outpost"}),
            Map.entry("minecraft:ravager", new String[]{"minecraft:pillager_outpost"}),
            Map.entry("minecraft:vindicator", new String[]{"minecraft:mansion"}),
            Map.entry("minecraft:evoker", new String[]{"minecraft:mansion"}),
            Map.entry("minecraft:witch", new String[]{"minecraft:swamp_hut"}),
            Map.entry("minecraft:breeze", new String[]{"minecraft:trial_chambers"}),
            Map.entry("minecraft:warden", new String[]{"minecraft:ancient_city"}),
            Map.entry("minecraft:cave_spider", new String[]{"minecraft:mineshaft", "minecraft:mineshaft_mesa"}),
            Map.entry("minecraft:silverfish", new String[]{"minecraft:stronghold"})
    );

    private static final Set<String> CAVE_MOBS = Set.of("minecraft:cave_spider", "minecraft:bat", "minecraft:silverfish");

    private static final Set<String> NETHER_MOBS = Set.of("minecraft:strider", "minecraft:ghast", "minecraft:blaze", "minecraft:magma_cube", "minecraft:piglin", "minecraft:piglin_brute", "minecraft:hoglin", "minecraft:zoglin", "minecraft:zombified_piglin", "minecraft:wither_skeleton");
    private static final Set<String> END_MOBS = Set.of("minecraft:shulker", "minecraft:ender_dragon");

    private OneMorph() {}

    public static synchronized void clear()
    {
        pool = null;
    }

    private static boolean excluded(ResourceLocation id)
    {
        String s = id.toString();
        if(Morph.configServer.oneMorphExcludedMobs.contains(s))
        {
            return true;
        }
        for(Pattern p : Morph.configServer.disabledMobsID)
        {
            if(p.matcher(s).matches())
            {
                return true;
            }
        }
        return false;
    }

    private static synchronized List<EntityType<?>> pool(ServerLevel level)
    {
        if(pool != null)
        {
            return pool;
        }
        List<EntityType<?>> list = new ArrayList<>();
        for(EntityType<?> type : BuiltInRegistries.ENTITY_TYPE)
        {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if(excluded(id) || type.getCategory() == MobCategory.MISC || !type.canSummon())
            {
                continue;
            }
            try
            {
                Entity e = type.create(level);
                if(e instanceof Mob mob && !(e instanceof Player))
                {
                    MobProfile profile = MobProfileManager.getOrCreate(mob);
                    if(profile.canMorph())
                    {
                        list.add(type);
                    }
                }
                if(e != null)
                {
                    e.discard();
                }
            }
            catch(Throwable ignored) {}
        }
        pool = list;
        return list;
    }

    public static void assign(ServerPlayer player)
    {
        ServerLevel level = (ServerLevel)player.level();
        List<EntityType<?>> types = pool(level);
        if(types.isEmpty())
        {
            Morph.LOGGER.warn("One Morph mode found no mobs to pick from");
            return;
        }
        PlayerData data = MorphManager.data(player);
        for(int attempt = 0; attempt < 10; attempt++)
        {
            EntityType<?> type = types.get(player.getRandom().nextInt(types.size()));
            Entity e = type.create(level);
            if(!(e instanceof LivingEntity living))
            {
                continue;
            }
            MorphVariant variant = VariantFactory.create(living);
            living.discard();
            if(variant == null)
            {
                continue;
            }
            variant = MorphManager.restrictVariant(player, variant);
            data.oneMorph = variant;
            data.last = variant;
            data.current = variant;
            Morph.LOGGER.info("One Morph: {} was assigned {}", player.getGameProfile().getName(), BuiltInRegistries.ENTITY_TYPE.getKey(type));
            if(Morph.configServer.oneMorphMoveToHabitat)
            {
                later(player, () -> moveToHabitat(player, type, data));
            }
            player.sendSystemMessage(Component.translatable("morph.message.oneMorphAssigned", variant.displayName()));
            return;
        }
    }

    private static final List<Object[]> PENDING = new ArrayList<>();

    private static void later(ServerPlayer player, Runnable task)
    {
        PENDING.add(new Object[] {player, new int[] {40}, task});
    }

    public static final int NORMAL_TICKS = 1200;
    public static final int COOLDOWN_TICKS = 12000;

    public static long now(ServerPlayer player)
    {
        return player.level().getServer().overworld().getGameTime();
    }

    public static String format(long ticks)
    {
        long s = Math.max(0L, (ticks + 19L) / 20L);
        return String.format(java.util.Locale.ROOT, "%d:%02d", s / 60L, s % 60L);
    }

    public static void timers(MinecraftServer server)
    {
        if(!(MorphManager.mode() instanceof com.iiampanda.morph.common.mode.OneMorphMode))
        {
            return;
        }
        for(ServerPlayer p : server.getPlayerList().getPlayers())
        {
            PlayerData d = MorphManager.data(p);
            if(d.oneMorph == null || !p.isAlive())
            {
                continue;
            }
            com.iiampanda.morph.api.morph.MorphInfo info = com.iiampanda.morph.api.MorphApi.getMorphInfo(p);
            if(info.isMorphed() || info.isTransitioning())
            {
                continue;
            }
            long now = now(p);
            if(d.oneNormalUntil == 0L)
            {
                d.oneNormalUntil = now + NORMAL_TICKS;
                com.iiampanda.morph.common.MorphRules.forget(p);
                com.iiampanda.morph.common.MorphRules.sync(p);
            }
            else if(now >= d.oneNormalUntil)
            {
                MorphManager.morph(p, d.oneMorph, true);
                MorphManager.sendData(p);
            }
        }
    }

    public static void tick()
    {
        if(PENDING.isEmpty())
        {
            return;
        }
        List<Object[]> due = new ArrayList<>();
        for(Object[] o : PENDING)
        {
            if(--((int[])o[1])[0] <= 0)
            {
                due.add(o);
            }
        }
        PENDING.removeAll(due);
        for(Object[] o : due)
        {
            ServerPlayer p = (ServerPlayer)o[0];
            if(p.connection != null && !p.hasDisconnected() && !p.isRemoved())
            {
                ((Runnable)o[2]).run();
            }
        }
    }

    public static void returnToHabitat(ServerPlayer player)
    {
        later(player, () -> doReturn(player));
    }

    private static void doReturn(ServerPlayer player)
    {
        PlayerData data = MorphManager.data(player);
        if(!data.hasSpawn || player.getRespawnPosition() != null)
        {
            return;
        }
        MinecraftServer server = player.level().getServer();
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(data.spawnDim)));
        if(level == null)
        {
            return;
        }
        BlockPos pos = BlockPos.of(data.spawnPos);
        player.teleportTo(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYRot(), player.getXRot());
    }

    private static void moveToHabitat(ServerPlayer player, EntityType<?> type, PlayerData data)
    {
        MinecraftServer server = player.level().getServer();
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
        ResourceKey<Level> homeKey = NETHER_MOBS.contains(id) ? Level.NETHER : END_MOBS.contains(id) ? Level.END : null;
        List<ServerLevel> levels = new ArrayList<>();
        ServerLevel current = (ServerLevel)player.level();
        ServerLevel home = homeKey == null ? null : server.getLevel(homeKey);
        if(home != null)
        {
            levels.add(home);
        }
        if(current != home)
        {
            levels.add(current);
        }
        for(ResourceKey<Level> key : List.of(Level.OVERWORLD, Level.NETHER, Level.END))
        {
            ServerLevel l = server.getLevel(key);
            if(l != null && !levels.contains(l))
            {
                levels.add(l);
            }
        }
        String[] structures = STRUCTURES.get(id);
        boolean cave = CAVE_MOBS.contains(id);
        boolean water = false;
        MobProfile profile = MobProfileManager.get(type);
        if(profile != null)
        {
            for(Trait<?> trait : profile.traits)
            {
                cave |= trait instanceof SunburnTrait;
                water |= trait instanceof WaterBreatherTrait wb && Boolean.TRUE.equals(wb.suffocatesOnLand);
            }
        }
        for(ServerLevel level : levels)
        {
            try
            {
                BlockPos origin = player.blockPosition();
                if(level.dimension() == Level.NETHER && current.dimension() != Level.NETHER)
                {
                    origin = new BlockPos(origin.getX() / 8, 64, origin.getZ() / 8);
                }
                else if(level.dimension() == Level.END && current.dimension() != Level.END)
                {
                    origin = new BlockPos(0, 64, 0);
                }
                BlockPos target = null;
                boolean undergroundHere = cave && !level.dimensionType().hasCeiling();
                if(structures != null)
                {
                    target = findStructure(level, structures, origin);
                }
                if(target == null && undergroundHere)
                {
                    target = origin;
                }
                if(target == null)
                {
                    final boolean wet = water;
                    Predicate<Holder<Biome>> test = h -> spawnsIn(h.value(), type) || (wet && (h.is(BiomeTags.IS_OCEAN) || h.is(BiomeTags.IS_RIVER)));
                    if(level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().stream().anyMatch(test))
                    {
                        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(test, origin, 6400, 32, 64);
                        if(found != null)
                        {
                            target = found.getFirst();
                        }
                    }
                }
                if(target == null && level == home)
                {
                    target = origin;
                }
                if(target == null)
                {
                    continue;
                }
                BlockPos spot = findSpot(level, type, target, undergroundHere, water);
                if(spot == null)
                {
                    continue;
                }
                data.spawnDim = level.dimension().location().toString();
                data.spawnPos = spot.asLong();
                data.hasSpawn = true;
                Morph.LOGGER.info("One Morph: moving {} from {} to {} at {}", player.getName().getString(), player.level().dimension(), level.dimension(), spot);
                player.teleportTo(level, spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, player.getYRot(), player.getXRot());
                return;
            }
            catch(RuntimeException e)
            {
                Morph.LOGGER.warn("One Morph could not search {} for a home for {}", level.dimension(), id, e);
            }
        }
        Morph.LOGGER.info("One Morph found no natural habitat for {}, leaving {} where they are", id, player.getGameProfile().getName());
    }

    private static boolean spawnsIn(Biome biome, EntityType<?> type)
    {
        MobSpawnSettings settings = biome.getMobSettings();
        for(MobCategory category : MobCategory.values())
        {
            for(MobSpawnSettings.SpawnerData w : settings.getMobs(category).unwrap())
            {
                if(w.type == type)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockPos findStructure(ServerLevel level, String[] ids, BlockPos origin)
    {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        List<Holder<Structure>> holders = new ArrayList<>();
        for(String s : ids)
        {
            Optional<Holder.Reference<Structure>> h = registry.getHolder(ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(s)));
            h.ifPresent(holders::add);
        }
        if(holders.isEmpty())
        {
            return null;
        }
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holders), origin, 100, false);
        return found == null ? null : found.getFirst();
    }

    private static BlockPos findSpot(ServerLevel level, EntityType<?> type, BlockPos center, boolean cave, boolean water)
    {
        boolean ceiling = level.dimensionType().hasCeiling();
        int max = cave ? 96 : 40;
        int step = cave ? 4 : 2;
        for(int r = 0; r <= max; r += step)
        {
            for(int dx = -r; dx <= r; dx += step)
            {
                for(int dz = -r; dz <= r; dz += step)
                {
                    if(Math.max(Math.abs(dx), Math.abs(dz)) != r)
                    {
                        continue;
                    }
                    int x = center.getX() + dx;
                    int z = center.getZ() + dz;
                    level.getChunk(x >> 4, z >> 4);
                    if(ceiling)
                    {
                        for(int y = 20; y < 110; y++)
                        {
                            BlockPos pos = new BlockPos(x, y, z);
                            if(fits(level, type, pos, water))
                            {
                                return pos;
                            }
                        }
                    }
                    else if(cave)
                    {
                        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                        for(int y = Math.min(top - 6, 62); y > level.getMinBuildHeight() + 5; y--)
                        {
                            BlockPos pos = new BlockPos(x, y, z);
                            if(fits(level, type, pos, water) && level.getBrightness(LightLayer.SKY, pos) == 0)
                            {
                                return pos;
                            }
                        }
                    }
                    else
                    {
                        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                        if(top <= level.getMinBuildHeight())
                        {
                            continue;
                        }
                        for(int y = top + 1; y >= Math.max(level.getMinBuildHeight() + 1, top - 12); y--)
                        {
                            BlockPos pos = new BlockPos(x, y, z);
                            if(fits(level, type, pos, water))
                            {
                                return pos;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean wet(ServerLevel level, BlockPos pos)
    {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static boolean fits(ServerLevel level, EntityType<?> type, BlockPos pos, boolean water)
    {
        if(water && !(wet(level, pos) && wet(level, pos.below()) && wet(level, pos.north()) && wet(level, pos.south()) && wet(level, pos.east()) && wet(level, pos.west())))
        {
            return false;
        }
        return SpawnPlacements.isSpawnPositionOk(type, level, pos) && level.noCollision(type.getSpawnAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
    }
}
