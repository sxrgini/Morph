package com.iiampanda.morph.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iiampanda.morph.api.morph.AttributeConfig;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/** config/morph/server.json */
public class ConfigServer
{
    public enum FilterType { ALLOW, DENY }

    public enum ModeType { CLASSIC, DEFAULT, COMMAND_ONLY, COMMAND_ALLOW_SELECTION, DISGUISE }

    /** Used to migrate old config files; don't edit. */
    public Integer configVersion = 2;

    public ModeType morphMode = ModeType.DEFAULT;
    public int morphTime = 100;
    public boolean silentMorphs = false;
    public boolean classicUpgradeTraits = false;
    public List<String> disabledTraits = new ArrayList<>();
    public List<String> disabledMobs = new ArrayList<>(List.of("minecraft:armor_stand"));

    /** Variants and baby forms */
    public boolean allowVariants = true;
    public boolean allowBabyForms = true;

    /** Modded mob support */
    public boolean autoDetectMobAbilities = true; // inspect mobs without a config file and write one for them
    public boolean generateMobConfigFiles = true;
    /** On server start, inspect every registered mob (including modded ones) and write config files for those that have none. */
    public boolean scanAllMobsOnStartup = true;

    /** Largest hitbox a morph can give you (blocks). Keeps huge mobs like the Ender Dragon from wedging you into the terrain. The model still renders full size. */
    public float maxHitboxWidth = 3F;
    public float maxHitboxHeight = 4F;

    /** Boss / mini boss special moves, used by right clicking while morphed. */
    public boolean specialAbilities = true;
    public boolean specialRequiresEmptyHand = true;

    /** Biomass (Default mode) */
    public double biomassValue = 0.3D;
    /** Biomass gained per kill, as a fraction of the killed mob's max health (0.5 = half its health). */
    public double biomassHealthFraction = 0.5D;
    public boolean biomassBypassAdvancement = true;

    /** modifier;more|less[;cap] */
    public List<String> supportedAttributes = new ArrayList<>(List.of(
            "minecraft:generic.max_health;more;20",
            "minecraft:generic.knockback_resistance;more",
            "minecraft:generic.movement_speed;more;0.1",
            "minecraft:generic.attack_damage;more",
            "minecraft:generic.attack_knockback;more",
            "minecraft:generic.attack_speed;more",
            "minecraft:generic.armor;more",
            "minecraft:generic.luck;more"
    ));

    public FilterType biomassFilterType = FilterType.DENY;
    public List<String> biomassFilterNames = new ArrayList<>();
    public FilterType morphFilterType = FilterType.DENY;
    public List<String> morphFilterNames = new ArrayList<>();
    public FilterType selectorFilterType = FilterType.DENY;
    public List<String> selectorFilterNames = new ArrayList<>();

    public transient ArrayList<Pattern> disabledMobsID = new ArrayList<>();
    public transient HashMap<Identifier, AttributeConfig> supportedAttributesMap = new HashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Path file()
    {
        return Platform.configDir().resolve("morph").resolve("server.json");
    }

    public static ConfigServer load()
    {
        ConfigServer cfg = new ConfigServer();
        Path file = file();
        try
        {
            Files.createDirectories(file.getParent());
            if(Files.exists(file))
            {
                try(Reader r = Files.newBufferedReader(file))
                {
                    ConfigServer read = GSON.fromJson(r, ConfigServer.class);
                    if(read != null)
                    {
                        cfg = read;
                        if(read.configVersion == null)
                        {
                            // files from before 1.0.1 defaulted to Classic; biomass (Default mode) is the intended default now
                            cfg.configVersion = 2;
                            if(cfg.morphMode == ModeType.CLASSIC)
                            {
                                cfg.morphMode = ModeType.DEFAULT;
                            }
                        }
                    }
                }
            }
            try(Writer w = Files.newBufferedWriter(file)) //re-save so newly added options show up
            {
                GSON.toJson(cfg, w);
            }
        }
        catch(IOException | RuntimeException e)
        {
            Morph.LOGGER.error("Could not read/write {}", file, e);
        }
        cfg.parse();
        return cfg;
    }

    public void parse()
    {
        disabledMobsID.clear();
        for(String s : disabledMobs)
        {
            try
            {
                disabledMobsID.add(Pattern.compile(s));
            }
            catch(RuntimeException e)
            {
                Morph.LOGGER.error("Invalid disabledMobs regex {}", s);
            }
        }
        supportedAttributesMap.clear();
        for(String s : supportedAttributes)
        {
            String[] split = s.split(";");
            if(split.length < 2)
            {
                Morph.LOGGER.error("Error parsing supported attribute config: {}", s);
                continue;
            }
            Double cap = null;
            if(split.length >= 3)
            {
                try { cap = Double.parseDouble(split[2]); } catch(NumberFormatException e) { Morph.LOGGER.error("Invalid cap in supported attribute: {}", s); }
            }
            Identifier id = Identifier.tryParse(split[0]);
            if(id != null)
            {
                supportedAttributesMap.put(id, new AttributeConfig(split[1].equalsIgnoreCase("more"), cap));
            }
        }
        if(morphMode == null)
        {
            morphMode = ModeType.DEFAULT;
        }
    }
}
