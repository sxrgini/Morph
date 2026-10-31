package com.iiampanda.morph.common.biomass;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.common.save.PlayerData;
import com.iiampanda.morph.platform.Platform;

import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class Upgrades
{
    public static final String ID_BIOMASS_CAPACITY = "biomass_capacity";
    public static final String ID_MORPH_ABILITY = "morph_ability";
    public static final String ID_MORPH_VARIANTS = "morph_variants";
    public static final String ID_BIOMASS_ABILITIES = "biomass_abilities";
    public static final String ID_ABILITY_EFFICIENCY = "biomass_ability_efficiency";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Map<String, BiomassUpgradeInfo> TREE = new LinkedHashMap<>();

    private Upgrades() {}

    public static void load()
    {
        TREE.clear();
        Path dir = Platform.configDir().resolve("morph").resolve("biomass");
        if(!Files.isDirectory(dir))
        {
            return;
        }
        try(Stream<Path> s = Files.list(dir))
        {
            s.filter(p -> p.toString().endsWith(".json")).sorted().forEach(p ->
            {
                String fn = p.getFileName().toString();
                if(fn.equals("biomass_efficiency.json") || fn.equals("biomass_max_mass_absorbable.json"))
                {
                    try { Files.deleteIfExists(p); } catch(IOException ignored) {}
                    return;
                }
                try(Reader r = Files.newBufferedReader(p))
                {
                    BiomassUpgradeInfo info = GSON.fromJson(r, BiomassUpgradeInfo.class);
                    if(info != null && info.id != null)
                    {
                        TREE.put(info.id, info);
                    }
                }
                catch(Exception e)
                {
                    Morph.LOGGER.error("Could not read biomass upgrade {}", p, e);
                }
            });
        }
        catch(IOException e)
        {
            Morph.LOGGER.error("Could not list biomass upgrades", e);
        }
    }

    public static String toJson()
    {
        return GSON.toJson(new ArrayList<>(TREE.values()));
    }

    public static void fromJson(String json)
    {
        TREE.clear();
        try
        {
            BiomassUpgradeInfo[] arr = GSON.fromJson(json, BiomassUpgradeInfo[].class);
            if(arr != null)
            {
                for(BiomassUpgradeInfo i : arr)
                {
                    TREE.put(i.id, i);
                }
            }
        }
        catch(RuntimeException e)
        {
            Morph.LOGGER.error("Bad upgrade list from server", e);
        }
    }

    @Nullable
    public static BiomassUpgradeInfo get(String id)
    {
        return TREE.get(id);
    }

    public static int level(PlayerData data, String id)
    {
        BiomassUpgradeInfo info = TREE.get(id);
        if(info == null)
        {
            return 0;
        }
        return info.free() + data.purchased.getOrDefault(id, 0);
    }

    public static double value(PlayerData data, String id)
    {
        BiomassUpgradeInfo info = TREE.get(id);
        if(info == null || info.value == null)
        {
            return 0D;
        }
        return info.value.get(level(data, id));
    }

    public static boolean has(PlayerData data, String id)
    {
        return level(data, id) > 0;
    }

    public static boolean isMaxed(PlayerData data, String id)
    {
        BiomassUpgradeInfo info = TREE.get(id);
        return info == null || (info.maxLevel != null && level(data, id) >= info.maxLevel);
    }

    public static double nextCost(PlayerData data, String id)
    {
        BiomassUpgradeInfo info = TREE.get(id);
        if(info == null || info.cost == null || isMaxed(data, id))
        {
            return -1D;
        }

        return Math.min(Math.round(info.cost.get(level(data, id) + 1)), Math.floor(maxBiomass(data)));
    }

    public static boolean parentUnlocked(PlayerData data, String id)
    {
        BiomassUpgradeInfo info = TREE.get(id);
        return info != null && (info.parentId == null || level(data, info.parentId) > 0);
    }

    public static boolean canPurchase(PlayerData data, String id)
    {
        double cost = nextCost(data, id);
        return cost >= 0D && parentUnlocked(data, id) && data.biomass >= cost;
    }

    public static boolean purchase(PlayerData data, String id)
    {
        if(!canPurchase(data, id))
        {
            return false;
        }
        data.biomass -= nextCost(data, id);
        data.purchased.merge(id, 1, Integer::sum);
        data.biomass = Math.min(data.biomass, maxBiomass(data));
        return true;
    }

    public static double maxBiomass(PlayerData data)
    {
        return Math.max(1D, value(data, ID_BIOMASS_CAPACITY));
    }

    public static List<BiomassUpgradeInfo> children(String parentId)
    {
        List<BiomassUpgradeInfo> list = new ArrayList<>();
        for(BiomassUpgradeInfo i : TREE.values())
        {
            if((parentId == null && i.parentId == null) || (parentId != null && parentId.equals(i.parentId)))
            {
                list.add(i);
            }
        }
        return list;
    }
}
