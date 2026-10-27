package com.iiampanda.morph.client.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Finds a modded mob's real arm by reading the mods' own resources: the Bedrock style "geo" model files used by GeckoLib
 * and similar libraries, and the entity textures. Everything loaded (mod jars, resource packs) is searched through the
 * game's resource manager, so no per-mod support is needed.
 */
public final class MorphGeo
{
    public record GeoArm(ModelPart part, ResourceLocation texture) {}

    private static final Map<String, Optional<GeoArm>> CACHE = new HashMap<>();
    private static final Map<String, List<ResourceLocation>> GEO_INDEX = new HashMap<>();
    private static final Map<String, List<ResourceLocation>> TEX_INDEX = new HashMap<>();

    private MorphGeo() {}

    /** Called when resources reload. */
    public static void clear()
    {
        CACHE.clear();
        GEO_INDEX.clear();
        TEX_INDEX.clear();
    }

    /** @return null when the mob has no geo model with an arm in any loaded mod */
    public static GeoArm find(Entity entity, Object renderer, boolean right)
    {
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + (right ? "#R" : "#L");
        Optional<GeoArm> cached = CACHE.get(key);
        if(cached == null)
        {
            GeoArm built = null;
            try
            {
                built = build(entity, renderer, right);
            }
            catch(Throwable t)
            {
                com.iiampanda.morph.common.Morph.LOGGER.warn("Could not read the model of {}", key, t);
            }
            cached = Optional.ofNullable(built);
            CACHE.put(key, cached);
            if(built != null)
            {
                com.iiampanda.morph.common.Morph.LOGGER.info("Found the {} arm of {} in the mod's own model files", right ? "right" : "left", key);
            }
        }
        return cached.orElse(null);
    }

    private static String norm(String s)
    {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String stem(ResourceLocation rl)
    {
        String p = rl.getPath();
        p = p.substring(p.lastIndexOf('/') + 1);
        int dot = p.indexOf('.');
        return norm(dot < 0 ? p : p.substring(0, dot));
    }

    /** 0 = same name, larger = looser; -1 = unrelated. */
    private static int score(String want, String have)
    {
        if(have.equals(want))
        {
            return 0;
        }
        if(have.startsWith(want) || want.startsWith(have))
        {
            return 1 + Math.abs(have.length() - want.length());
        }
        if(have.contains(want) || want.contains(have))
        {
            return 20 + Math.abs(have.length() - want.length());
        }
        return -1;
    }

    private static List<ResourceLocation> index(Map<String, List<ResourceLocation>> cache, String ns, String[] dirs, String suffixes)
    {
        List<ResourceLocation> list = cache.get(ns);
        if(list == null)
        {
            list = new ArrayList<>();
            ResourceManager mgr = Minecraft.getInstance().getResourceManager();
            for(String dir : dirs)
            {
                for(ResourceLocation rl : mgr.listResources(dir, r -> r.getNamespace().equals(ns) && (suffixes.contains("*") ? true : r.getPath().endsWith(suffixes))).keySet())
                {
                    if(!list.contains(rl))
                    {
                        list.add(rl);
                    }
                }
            }
            cache.put(ns, list);
        }
        return list;
    }

    private static List<ResourceLocation> best(List<ResourceLocation> all, String want, boolean geo)
    {
        List<ResourceLocation> out = new ArrayList<>(all);
        Map<ResourceLocation, Integer> sc = new HashMap<>();
        out.removeIf(rl ->
        {
            if(geo && !(rl.getPath().endsWith(".geo.json") || rl.getPath().startsWith("geo/") || rl.getPath().startsWith("geckolib/models")))
            {
                return true;
            }
            int s = score(want, stem(rl));
            if(s < 0)
            {
                return true;
            }
            sc.put(rl, s + (rl.getPath().contains("entity") ? 0 : 3));
            return false;
        });
        out.sort(java.util.Comparator.comparingInt(sc::get));
        return out;
    }

    private static GeoArm build(Entity entity, Object renderer, boolean right) throws Exception
    {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String ns = id.getNamespace();
        String want = norm(id.getPath());
        ResourceManager mgr = Minecraft.getInstance().getResourceManager();
        List<ResourceLocation> geos = best(index(GEO_INDEX, ns, new String[]{ "geo", "geckolib", "models", "bedrock" }, ".json"), want, true);
        int tried = 0;
        for(ResourceLocation g : geos)
        {
            if(tried++ >= 4)
            {
                break;
            }
            Optional<Resource> res = mgr.getResource(g);
            if(res.isEmpty())
            {
                continue;
            }
            JsonObject geometry;
            try(java.io.Reader r = res.get().openAsReader())
            {
                geometry = pickGeometry(JsonParser.parseReader(r), want);
            }
            catch(Exception e)
            {
                continue;
            }
            if(geometry == null)
            {
                continue;
            }
            ModelPart arm = buildArm(geometry, right);
            if(arm == null)
            {
                continue;
            }
            ResourceLocation tex = textureFor(entity, renderer, ns, want);
            if(tex != null)
            {
                return new GeoArm(arm, tex);
            }
        }
        return null;
    }

    private static ResourceLocation textureFor(Entity entity, Object renderer, String ns, String want)
    {
        if(renderer != null)
        {
            for(java.lang.reflect.Method m : renderer.getClass().getMethods())
            {
                if(m.getName().equals("getTextureLocation") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInstance(entity) && ResourceLocation.class.isAssignableFrom(m.getReturnType()))
                {
                    try
                    {
                        Object o = m.invoke(renderer, entity);
                        if(o != null)
                        {
                            return (ResourceLocation)o;
                        }
                    }
                    catch(Throwable ignored) {}
                }
            }
        }
        List<ResourceLocation> tex = best(index(TEX_INDEX, ns, new String[]{ "textures/entity", "textures" }, ".png"), want, false);
        return tex.isEmpty() ? null : tex.get(0);
    }

    private static JsonObject pickGeometry(JsonElement root, String want)
    {
        if(!root.isJsonObject())
        {
            return null;
        }
        JsonObject o = root.getAsJsonObject();
        List<JsonObject> found = new ArrayList<>();
        if(o.has("minecraft:geometry") && o.get("minecraft:geometry").isJsonArray())
        {
            for(JsonElement e : o.getAsJsonArray("minecraft:geometry"))
            {
                if(e.isJsonObject())
                {
                    found.add(e.getAsJsonObject());
                }
            }
        }
        else
        {
            for(Map.Entry<String, JsonElement> e : o.entrySet())
            {
                if(e.getKey().startsWith("geometry.") && e.getValue().isJsonObject())
                {
                    found.add(e.getValue().getAsJsonObject());
                }
            }
        }
        for(JsonObject g : found)
        {
            if(g.has("description") && g.getAsJsonObject("description").has("identifier") && norm(g.getAsJsonObject("description").get("identifier").getAsString()).contains(want))
            {
                return g;
            }
        }
        return found.isEmpty() ? null : found.get(0);
    }

    private static float f(JsonArray a, int i, float def)
    {
        return a != null && a.size() > i ? a.get(i).getAsFloat() : def;
    }

    private static JsonArray arr(JsonObject o, String k)
    {
        return o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : null;
    }

    private static boolean isArmName(String n, boolean right)
    {
        String s = norm(n);
        if(s.contains("forearm") || s.contains("lower") || s.contains("hand") || s.contains("finger") || s.contains("elbow") || s.contains("sleeve"))
        {
            return false;
        }
        String side = right ? "right" : "left";
        String c = right ? "r" : "l";
        return (s.contains(side) && s.contains("arm")) || s.equals("arm" + c) || s.equals(c + "arm");
    }

    private static ModelPart buildArm(JsonObject geometry, boolean right)
    {
        JsonArray bones = arr(geometry, "bones");
        if(bones == null)
        {
            return null;
        }
        float texW = 64F, texH = 64F;
        JsonObject desc = geometry.has("description") && geometry.get("description").isJsonObject() ? geometry.getAsJsonObject("description") : geometry;
        for(String k : new String[]{ "texture_width", "texturewidth" })
        {
            if(desc.has(k))
            {
                texW = desc.get(k).getAsFloat();
            }
        }
        for(String k : new String[]{ "texture_height", "textureheight" })
        {
            if(desc.has(k))
            {
                texH = desc.get(k).getAsFloat();
            }
        }
        Map<String, JsonObject> byName = new HashMap<>();
        for(JsonElement e : bones)
        {
            JsonObject b = e.getAsJsonObject();
            byName.put(b.get("name").getAsString(), b);
        }
        // the top most bone that looks like this arm
        JsonObject root = null;
        for(JsonObject b : byName.values())
        {
            if(isArmName(b.get("name").getAsString(), right))
            {
                String parent = b.has("parent") ? b.get("parent").getAsString() : null;
                JsonObject p = parent == null ? null : byName.get(parent);
                if(p == null || !isArmName(parent, right))
                {
                    root = b;
                    break;
                }
            }
        }
        if(root == null)
        {
            return null;
        }
        JsonArray pivot = arr(root, "pivot");
        float px = f(pivot, 0, 0F), py = f(pivot, 1, 0F), pz = f(pivot, 2, 0F);
        CubeListBuilder cubes = CubeListBuilder.create();
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        int count = 0;
        for(JsonObject b : byName.values())
        {
            // the arm bone itself and everything parented under it (forearm, hand...)
            boolean inside = false;
            for(JsonObject c = b; c != null; )
            {
                if(c == root)
                {
                    inside = true;
                    break;
                }
                c = c.has("parent") ? byName.get(c.get("parent").getAsString()) : null;
            }
            JsonArray cl = arr(b, "cubes");
            if(!inside || cl == null)
            {
                continue;
            }
            boolean boneMirror = b.has("mirror") && b.get("mirror").getAsBoolean();
            for(JsonElement ce : cl)
            {
                JsonObject c = ce.getAsJsonObject();
                JsonArray o = arr(c, "origin"), s = arr(c, "size");
                if(o == null || s == null)
                {
                    continue;
                }
                float ox = f(o, 0, 0F), oy = f(o, 1, 0F), oz = f(o, 2, 0F), sx = f(s, 0, 0F), sy = f(s, 1, 0F), sz = f(s, 2, 0F);
                float u = 0F, v = 0F;
                if(c.has("uv") && c.get("uv").isJsonArray())
                {
                    u = f(c.getAsJsonArray("uv"), 0, 0F);
                    v = f(c.getAsJsonArray("uv"), 1, 0F);
                }
                else if(c.has("uv") && c.get("uv").isJsonObject() && c.getAsJsonObject("uv").has("north"))
                {
                    JsonObject n = c.getAsJsonObject("uv").getAsJsonObject("north");
                    u = f(arr(n, "uv"), 0, 0F) - sz;
                    v = f(arr(n, "uv"), 1, 0F) - sz;
                }
                float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : (b.has("inflate") ? b.get("inflate").getAsFloat() : 0F);
                boolean mirror = c.has("mirror") ? c.get("mirror").getAsBoolean() : boneMirror;
                float rx = ox - px, ry = py - oy - sy, rz = oz - pz;
                cubes.texOffs((int)u, (int)v).mirror(mirror).addBox(rx, ry, rz, sx, sy, sz, new CubeDeformation(inflate));
                minX = Math.min(minX, rx);
                maxX = Math.max(maxX, rx + sx);
                minY = Math.min(minY, ry);
                maxY = Math.max(maxY, ry + sy);
                minZ = Math.min(minZ, rz);
                maxZ = Math.max(maxZ, rz + sz);
                count++;
            }
        }
        if(count == 0)
        {
            return null;
        }
        // an arm sized to fit in front of the camera, top edge at the shoulder
        float h = Math.max(0.01F, maxY - minY);
        float w = Math.max(0.01F, Math.max(maxX - minX, maxZ - minZ));
        float scale = Math.min(1F, Math.min(14F / h, 6F / w));
        float cx = (minX + maxX) / 2F, cz = (minZ + maxZ) / 2F;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition arm = mesh.getRoot().addOrReplaceChild("arm", cubes, PartPose.offset((right ? -6F : 6F) - cx * scale, -minY * scale, -cz * scale));
        ModelPart part = LayerDefinition.create(mesh, (int)texW, (int)texH).bakeRoot().getChild("arm");
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
        return part;
    }
}
