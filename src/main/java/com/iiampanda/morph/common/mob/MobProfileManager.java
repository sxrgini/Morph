package com.iiampanda.morph.common.mob;

import com.iiampanda.morph.api.mob.trait.TraitHandler;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class MobProfileManager
{
    private static final Map<ResourceLocation, MobProfile> PROFILES = new HashMap<>();
    private static final Map<ResourceLocation, Path> FILES = new HashMap<>();
    private static Path dir;

    private MobProfileManager() {}

    public static Path dir()
    {
        if(dir == null)
        {
            dir = Platform.configDir().resolve("morph").resolve("mobs");
        }
        return dir;
    }

    private static final java.util.Set<String> EMISSIVE = new java.util.TreeSet<>();

    private static Path emissiveFile()
    {
        return Platform.configDir().resolve("morph").resolve("emissive_mobs.txt");
    }

    public static synchronized boolean hasEmissiveTextures(ResourceLocation id)
    {
        return EMISSIVE.contains(id.toString());
    }

    public static synchronized void markEmissive(java.util.List<String> ids)
    {
        boolean changed = false;
        for(String s : ids)
        {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if(id == null || id.getNamespace().equals("minecraft") || !BuiltInRegistries.ENTITY_TYPE.containsKey(id))
            {
                continue;
            }
            if(EMISSIVE.add(id.toString()))
            {
                changed = true;
            }
            MobProfile p = PROFILES.get(id);
            if(p != null && p.autoDetected != null && p.autoDetected && p.traits.stream().noneMatch(t -> t instanceof com.iiampanda.morph.api.mob.trait.EmissiveTrait))
            {
                com.iiampanda.morph.api.mob.trait.EmissiveTrait glow = new com.iiampanda.morph.api.mob.trait.EmissiveTrait();
                glow.lightLevel = 7;
                p.traits.add(glow);
                save(id, p);
            }
        }
        if(changed)
        {
            try
            {
                Files.createDirectories(emissiveFile().getParent());
                Files.write(emissiveFile(), EMISSIVE, StandardCharsets.UTF_8);
            }
            catch(IOException e)
            {
                Morph.LOGGER.warn("Could not save the emissive mob list", e);
            }
        }
    }

    public static synchronized void init()
    {
        try
        {
            if(Files.exists(emissiveFile()))
            {
                EMISSIVE.addAll(Files.readAllLines(emissiveFile(), StandardCharsets.UTF_8));
            }
        }
        catch(IOException ignored) {}
        try
        {
            Files.createDirectories(dir());
            copyDefaults();
        }
        catch(IOException e)
        {
            Morph.LOGGER.error("Could not set up the mob config folder", e);
        }
        reload();
    }

    private static void copyDefaults() throws IOException
    {
        try(InputStream in = MobProfileManager.class.getResourceAsStream("/morph_defaults/index.txt"))
        {
            if(in == null)
            {
                return;
            }
            try(BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
                String line;
                while((line = br.readLine()) != null)
                {
                    line = line.trim();
                    if(line.isEmpty())
                    {
                        continue;
                    }
                    Path target = Platform.configDir().resolve("morph").resolve(line);
                    if(Files.exists(target))
                    {
                        if(line.startsWith("biomass/") && biomassSchemaOutdated(target, line))
                        {
                            Files.move(target, target.resolveSibling(target.getFileName() + ".old"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            Morph.LOGGER.info("Replaced outdated biomass upgrade file {} (old copy kept as .old)", line);
                        }
                        else
                        {
                            continue;
                        }
                    }
                    Files.createDirectories(target.getParent());
                    try(InputStream res = MobProfileManager.class.getResourceAsStream("/morph_defaults/" + line))
                    {
                        if(res != null)
                        {
                            Files.copy(res, target);
                        }
                    }
                }
            }
        }
    }

    private static int schemaOf(Reader r)
    {
        try
        {
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseReader(r).getAsJsonObject();
            return o.has("schema") ? o.get("schema").getAsInt() : 0;
        }
        catch(Exception e)
        {
            return 0;
        }
    }

    private static boolean biomassSchemaOutdated(Path existing, String resource)
    {
        try(Reader a = Files.newBufferedReader(existing); InputStream in = MobProfileManager.class.getResourceAsStream("/morph_defaults/" + resource))
        {
            return in != null && schemaOf(a) < schemaOf(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        catch(IOException e)
        {
            return false;
        }
    }

    public static synchronized void reload()
    {
        PROFILES.clear();
        FILES.clear();
        try(Stream<Path> stream = Files.walk(dir()))
        {
            stream.filter(p -> p.toString().endsWith(".json")).forEach(MobProfileManager::loadFile);
        }
        catch(IOException e)
        {
            Morph.LOGGER.error("Could not read mob configs", e);
        }
        Morph.LOGGER.info("Loaded {} mob profiles", PROFILES.size());
    }

    private static void loadFile(Path p)
    {
        try(Reader r = Files.newBufferedReader(p))
        {
            MobProfile profile = TraitHandler.GSON.fromJson(r, MobProfile.class);
            if(profile != null && profile.forEntity != null)
            {
                ResourceLocation id = ResourceLocation.tryParse(profile.forEntity);
                if(id != null)
                {
                    PROFILES.put(id, profile);
                    FILES.put(id, p);
                    boolean dirty = SpecialAbilities.upgrade(profile, SpecialAbilities.defaultForVanilla(id));
                    for(com.iiampanda.morph.api.mob.trait.Trait<?> t : profile.traits)
                    {

                        if(t instanceof com.iiampanda.morph.api.mob.trait.WaterBreatherTrait wb && wb.suffocatesOnLand != null && wb.suffocatesOnLand && wb.nightVisionInWater == null)
                        {
                            wb.nightVisionInWater = true;
                            dirty = true;
                        }
                    }
                    if(dirty)
                    {
                        save(id, profile);
                    }
                }
            }
        }
        catch(Exception e)
        {
            Morph.LOGGER.error("Could not parse mob config {}", p, e);
        }
    }

    @Nullable
    public static synchronized MobProfile get(EntityType<?> type)
    {
        return PROFILES.get(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static synchronized MobProfile getOrCreate(LivingEntity sample)
    {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(sample.getType());
        MobProfile existing = PROFILES.get(id);
        if(existing != null)
        {
            if(existing.specialsHandled == null && SpecialAbilities.upgrade(existing, SpecialAbilities.defaultFor(sample)))
            {
                save(id, existing);
            }
            if((existing.defaultsVersion == null || existing.defaultsVersion < 15))
            {
                MobProfiler.migrate(existing, sample);
                existing.defaultsVersion = 15;
                save(id, existing);
            }
            return existing;
        }
        MobProfile profile;
        if(Morph.configServer == null || Morph.configServer.autoDetectMobAbilities)
        {
            profile = MobProfiler.detect(sample);
            Morph.LOGGER.info("Detected abilities for {}: {}", id, profile.traits.stream().map(t -> t.type).toList());
        }
        else
        {
            profile = new MobProfile();
            profile.forEntity = id.toString();
        }
        PROFILES.put(id, profile);
        if(Morph.configServer == null || Morph.configServer.generateMobConfigFiles)
        {
            Path file = dir().resolve(id.getNamespace()).resolve(id.getPath().replace('/', '_') + ".json");
            try
            {
                Files.createDirectories(file.getParent());
                if(!Files.exists(file))
                {
                    try(Writer w = Files.newBufferedWriter(file))
                    {
                        TraitHandler.GSON.toJson(profile, w);
                    }
                }
            }
            catch(IOException e)
            {
                Morph.LOGGER.error("Could not write mob config {}", file, e);
            }
        }
        return profile;
    }

    private static void save(ResourceLocation id, MobProfile profile)
    {
        if(Morph.configServer != null && !Morph.configServer.generateMobConfigFiles)
        {
            return;
        }
        Path file = FILES.getOrDefault(id, dir().resolve(id.getNamespace()).resolve(id.getPath().replace('/', '_') + ".json"));
        try(Writer w = Files.newBufferedWriter(file))
        {
            TraitHandler.GSON.toJson(profile, w);
        }
        catch(IOException e)
        {
            Morph.LOGGER.error("Could not update mob config {}", file, e);
        }
    }

    public static int scanAll(net.minecraft.world.level.Level level)
    {
        int count = 0;
        for(EntityType<?> type : BuiltInRegistries.ENTITY_TYPE)
        {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if(PROFILES.containsKey(id))
            {
                continue;
            }
            try
            {
                net.minecraft.world.entity.Entity e = type.create(level);
                if(e instanceof LivingEntity living && !(e instanceof net.minecraft.world.entity.player.Player))
                {
                    getOrCreate(living);
                    count++;
                }
            }
            catch(Throwable ignored) {}
        }
        return count;
    }
}
