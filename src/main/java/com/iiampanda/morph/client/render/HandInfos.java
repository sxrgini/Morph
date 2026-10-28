package com.iiampanda.morph.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iiampanda.morph.common.Morph;
import com.iiampanda.morph.platform.Platform;
import net.minecraft.client.Minecraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand infos: which part of a mob's model is held out as the player's arm in first person, and how to nudge it into place.
 * Same idea as the original mod's morph/hand folder. Files are plain JSON and are read, later ones overriding earlier ones, from:
 *  1. every loaded mod / resource pack:  assets/&lt;namespace&gt;/morph/hand/&lt;anything&gt;.json   (this is how a mob mod ships its own)
 *  2. the player's config folder:        config/morph/hand/&lt;anything&gt;.json  (sub folders are fine)
 *
 * <pre>
 * {
 *   "author": "you",
 *   "forEntity": "examplemod:crab",                          // an entity type id, or...
 *   "forClass": "com.example.client.model.CrabModel",       // ...a model class (its subclasses are covered too)
 *   "leftHandParts":  [ { "partName": "left_claw",  "placementCorrectors": [ { "type": "translate", "axis": "y", "amount": -0.25 } ] } ],
 *   "rightHandParts": [ { "partName": "right_claw" } ]
 * }
 * </pre>
 * fieldName is the java field of the part in the model class (like the original mod), partName is the part's name in the model tree (a/b/c walks down it); give either or both. Several parts can be listed
 * (an arm and its sleeve, say). placementCorrectors are optional: "translate" moves in blocks, "rotate" turns in degrees, on axis x, y or z.
 * The part is first set where a player's arm sits, with its own rotation cleared, and then the correctors are applied.
 */
public final class HandInfos
{
    public record Corrector(boolean rotate, char axis, float amount) {}

    public record Entry(String partName, String fieldName, boolean autoFit, List<Corrector> correctors) {}

    public record Info(String author, List<Entry> left, List<Entry> right)
    {
        public List<Entry> side(boolean rightHand)
        {
            return rightHand ? right : left;
        }
    }

    private static final Map<String, Info> BY_ENTITY = new HashMap<>();
    private static final Map<String, Info> BY_CLASS = new HashMap<>();
    private static boolean loaded;

    private HandInfos() {}

    public static synchronized void reset()
    {
        loaded = false;
        BY_ENTITY.clear();
        BY_CLASS.clear();
    }

    public static synchronized void ensureLoaded()
    {
        if(loaded)
        {
            return;
        }
        loaded = true;
        try
        {
            var found = Minecraft.getInstance().getResourceManager().listResources("morph/hand", id -> id.getPath().endsWith(".json"));
            for(var e : found.entrySet())
            {
                try(BufferedReader r = e.getValue().openAsReader())
                {
                    parse(e.getKey().toString(), JsonParser.parseReader(r));
                }
                catch(Exception ex)
                {
                    Morph.LOGGER.warn("Could not read hand info {}", e.getKey(), ex);
                }
            }
        }
        catch(Throwable t)
        {
            Morph.LOGGER.warn("Could not look for hand infos in the loaded resources", t);
        }
        Path dir = Platform.configDir().resolve("morph").resolve("hand");
        copyDefaults(dir);
        try
        {
            if(!Files.isDirectory(dir))
            {
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("README.txt"), "Put hand info .json files in here (sub folders are fine) to change which part of a mob's model is shown as your arm in first person.\n"
                    + "Mod authors can ship the same files inside their mod as assets/<modid>/morph/hand/<name>.json. Files in this folder win over those.\n\n"
                    + "{\n  \"forEntity\": \"examplemod:crab\",\n  \"leftHandParts\": [ { \"partName\": \"left_claw\", \"placementCorrectors\": [ { \"type\": \"translate\", \"axis\": \"y\", \"amount\": -0.25 } ] } ],\n"
                    + "  \"rightHandParts\": [ { \"partName\": \"right_claw\" } ]\n}\n\n"
                    + "Use \"forEntity\" (an entity id) or \"forClass\" (a model class name). translate is in blocks, rotate is in degrees, axis is x, y or z.\n"
                    + "The part is placed where a player's arm sits, with its own rotation cleared, then the correctors are applied.\n", StandardCharsets.UTF_8);
            }
            try(var walk = Files.walk(dir))
            {
                for(Path p : (Iterable<Path>)walk.filter(x -> x.getFileName().toString().endsWith(".json"))::iterator)
                {
                    try(BufferedReader r = Files.newBufferedReader(p))
                    {
                        parse(p.toString(), JsonParser.parseReader(r));
                    }
                    catch(Exception ex)
                    {
                        Morph.LOGGER.warn("Could not read hand info {}", p, ex);
                    }
                }
            }
        }
        catch(IOException e)
        {
            Morph.LOGGER.warn("Could not read the hand info folder", e);
        }
        Morph.LOGGER.info("Loaded {} hand info(s)", BY_ENTITY.size() + BY_CLASS.size());
    }

    /** Copies the hand infos bundled in the jar (morph_defaults/hand, listed in its index.txt) into the config folder, never overwriting a file. */
    private static void copyDefaults(Path dir)
    {
        try(java.io.InputStream in = HandInfos.class.getResourceAsStream("/morph_defaults/hand/index.txt"))
        {
            if(in == null)
            {
                return;
            }
            for(String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\\R"))
            {
                line = line.trim();
                if(line.isEmpty())
                {
                    continue;
                }
                Path target = dir.resolve(line);
                if(Files.exists(target))
                {
                    continue;
                }
                try(java.io.InputStream file = HandInfos.class.getResourceAsStream("/morph_defaults/hand/" + line))
                {
                    if(file != null)
                    {
                        Files.createDirectories(target.getParent());
                        Files.write(target, file.readAllBytes());
                    }
                }
            }
        }
        catch(IOException e)
        {
            Morph.LOGGER.warn("Could not copy the default hand infos", e);
        }
    }

    private static void parse(String source, JsonElement root)
    {
        if(!root.isJsonObject())
        {
            return;
        }
        JsonObject o = root.getAsJsonObject();
        Info info = new Info(o.has("author") ? o.get("author").getAsString() : "", entries(o.get("leftHandParts")), entries(o.get("rightHandParts")));
        if(info.left.isEmpty() && info.right.isEmpty())
        {
            Morph.LOGGER.warn("Hand info {} lists no parts", source);
            return;
        }
        boolean any = false;
        if(o.has("forEntity"))
        {
            BY_ENTITY.put(o.get("forEntity").getAsString(), info);
            any = true;
        }
        if(o.has("forClass"))
        {
            BY_CLASS.put(o.get("forClass").getAsString(), info);
            any = true;
        }
        if(!any)
        {
            Morph.LOGGER.warn("Hand info {} needs a forEntity or a forClass", source);
        }
    }

    private static List<Entry> entries(JsonElement el)
    {
        List<Entry> list = new ArrayList<>();
        if(el == null || !el.isJsonArray())
        {
            return list;
        }
        for(JsonElement e : (JsonArray)el)
        {
            if(!e.isJsonObject())
            {
                continue;
            }
            JsonObject o = e.getAsJsonObject();
            String partName = o.has("partName") ? o.get("partName").getAsString() : null;
            String fieldName = o.has("fieldName") ? o.get("fieldName").getAsString() : null;
            if(partName == null && fieldName == null)
            {
                continue;
            }
            List<Corrector> fix = new ArrayList<>();
            if(o.has("placementCorrectors") && o.get("placementCorrectors").isJsonArray())
            {
                for(JsonElement c : o.getAsJsonArray("placementCorrectors"))
                {
                    JsonObject co = c.getAsJsonObject();
                    String axis = co.get("axis").getAsString();
                    fix.add(new Corrector("rotate".equals(co.get("type").getAsString()), axis.isEmpty() ? 'x' : Character.toLowerCase(axis.charAt(0)), co.get("amount").getAsFloat()));
                }
            }
            list.add(new Entry(partName, fieldName, !o.has("autoFit") || o.get("autoFit").getAsBoolean(), fix));
        }
        return list;
    }

    /** The info for a mob: its entity id first, then its model's class and superclasses. */
    public static synchronized Info lookup(String entityId, Class<?> modelClass)
    {
        ensureLoaded();
        Info i = BY_ENTITY.get(entityId);
        for(Class<?> c = modelClass; i == null && c != null && c != Object.class; c = c.getSuperclass())
        {
            i = BY_CLASS.get(c.getName());
        }
        return i;
    }
}
