package com.iiampanda.morph.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Models built from boxes rather than vanilla ModelParts: Citadel's AdvancedModelBox (Alex's Mobs), the LionfishAPI copy of it
 * (L_Ender's Cataclysm) and anything else shaped like them. They are only read by reflection, so none of those mods is needed to
 * compile, and a box is recognised by what it has (a rotation point, rotation angles and a render method), not by its class name.
 * The hand infos name the model's field, exactly as they do for ModelParts.
 */
public final class MorphBoxes
{
    private MorphBoxes() {}

    private static Field field(Class<?> c, String name)
    {
        for(; c != null && c != Object.class; c = c.getSuperclass())
        {
            try
            {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            }
            catch(Throwable ignored) {}
        }
        return null;
    }

    private static float num(Object o, String name)
    {
        try
        {
            Field f = field(o.getClass(), name);
            return f == null ? 0F : f.getFloat(o);
        }
        catch(Throwable t)
        {
            return 0F;
        }
    }

    /** A box: has a rotation point, rotation angles and a render method taking a pose stack and a vertex consumer. */
    public static boolean isBox(Object o)
    {
        if(o == null || field(o.getClass(), "rotationPointX") == null || field(o.getClass(), "rotateAngleX") == null)
        {
            return false;
        }
        return renderMethod(o) != null;
    }

    private static Method renderMethod(Object box)
    {
        Method best = null;
        for(Class<?> c = box.getClass(); c != null && c != Object.class; c = c.getSuperclass())
        {
            for(Method m : c.getDeclaredMethods())
            {
                Class<?>[] p = m.getParameterTypes();
                if(m.getName().equals("render") && p.length >= 4 && p[0] == PoseStack.class && p[1] == VertexConsumer.class && p[2] == int.class && p[3] == int.class)
                {
                    if(p.length == 5 && p[4] == int.class || p.length == 8 && p[4] == float.class)
                    {
                        if(best == null || p.length == 5)
                        {
                            best = m;
                        }
                    }
                }
            }
        }
        if(best != null)
        {
            best.setAccessible(true);
        }
        return best;
    }

    /** The box in a model's field of that name, or null when the field is not a box. */
    public static Object boxField(Object model, String name)
    {
        if(name == null)
        {
            return null;
        }
        try
        {
            Field f = field(model.getClass(), name);
            if(f != null)
            {
                Object v = f.get(model);
                return isBox(v) ? v : null;
            }
        }
        catch(Throwable ignored) {}
        return null;
    }

    /** True if the model keeps its parts in boxes. */
    public static boolean hasBoxes(Object model)
    {
        return !allBoxes(model).isEmpty();
    }

    /**
     * The model that actually holds the boxes: the model itself, or one it wraps (Alex's Mobs on newer versions puts its Citadel model
     * inside a vanilla EntityModel bridge). Null if there are none.
     */
    public static Object unwrap(Object model)
    {
        if(model == null)
        {
            return null;
        }
        if(hasBoxes(model))
        {
            return model;
        }
        for(Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass())
        {
            for(Field f : c.getDeclaredFields())
            {
                if(java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive() || f.getType() == String.class)
                {
                    continue;
                }
                try
                {
                    f.setAccessible(true);
                    Object v = f.get(model);
                    if(v != null && v != model && !(v instanceof java.util.Collection) && !(v instanceof Map) && hasBoxes(v))
                    {
                        return v;
                    }
                }
                catch(Throwable ignored) {}
            }
        }
        return null;
    }

    private static Map<Object, String> allBoxes(Object model)
    {
        Map<Object, String> out = new IdentityHashMap<>();
        for(Class<?> c = model.getClass(); c != null && c != Object.class; c = c.getSuperclass())
        {
            for(Field f : c.getDeclaredFields())
            {
                if(java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                {
                    continue;
                }
                try
                {
                    f.setAccessible(true);
                    Object v = f.get(model);
                    if(v != null && !out.containsKey(v) && isBox(v))
                    {
                        out.put(v, f.getName());
                    }
                }
                catch(Throwable ignored) {}
            }
        }
        return out;
    }

    private static Object parent(Object box)
    {
        try
        {
            Field f = field(box.getClass(), "parent");
            return f == null ? null : f.get(box);
        }
        catch(Throwable t)
        {
            return null;
        }
    }

    private static void apply(Object box, PoseStack ps)
    {
        try
        {
            for(Method m : box.getClass().getMethods())
            {
                if(m.getName().equals("translateAndRotate") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == PoseStack.class)
                {
                    m.invoke(box, ps);
                    return;
                }
            }
        }
        catch(Throwable ignored) {}
    }

    /** Puts the model back to its rest pose (the one its constructor saved) so the hand is not caught mid stride. Skipped when the model never saved one. */
    public static void rest(Object model)
    {
        for(Object box : allBoxes(model).keySet())
        {
            boolean live = num(box, "rotationPointX") != 0F || num(box, "rotationPointY") != 0F || num(box, "rotationPointZ") != 0F;
            boolean saved = num(box, "defaultPositionX") != 0F || num(box, "defaultPositionY") != 0F || num(box, "defaultPositionZ") != 0F;
            if(live && !saved)
            {
                return;
            }
        }
        try
        {
            model.getClass().getMethod("resetToDefaultPose").invoke(model);
        }
        catch(Throwable ignored) {}
    }

    /**
     * Draws a box (and what hangs off it) into quads, with its parents' pose applied, turned the way a renderer would turn the mob
     * (y up) so the quads can be treated like any other captured limb.
     * @param type the render type every quad is drawn with
     */
    public static MorphQuads capture(Object box, Object type)
    {
        List<Object> chain = new ArrayList<>();
        for(Object p = parent(box); p != null && chain.size() < 32; p = parent(p))
        {
            chain.add(0, p);
        }
        PoseStack ps = new PoseStack();
        for(Object p : chain)
        {
            apply(p, ps);
        }
        MorphQuads mq = new MorphQuads();
        VertexConsumer rec = mq.new Rec(type);
        try
        {
            Method m = renderMethod(box);
            if(m == null)
            {
                return null;
            }
            if(m.getParameterCount() == 5)
            {
                m.invoke(box, ps, rec, 15728880, 0, -1);
            }
            else
            {
                m.invoke(box, ps, rec, 15728880, 0, 1F, 1F, 1F, 1F);
            }
        }
        catch(Throwable t)
        {
            return null;
        }
        if(mq.quads.isEmpty())
        {
            return null;
        }
        // model space is y down and x flipped compared with the world the renderer puts it in
        for(MorphQuads.Quad q : mq.quads)
        {
            for(int i = 0; i < 4; i++)
            {
                q.p[i * 3] = -q.p[i * 3];
                q.p[i * 3 + 1] = -q.p[i * 3 + 1];
            }
        }
        return mq;
    }

    /** A box for the player's hand when the mob has no hand info: the best arm-like thing on that side. */
    public static Object pick(Object model, boolean right)
    {
        Map<Object, String> all = allBoxes(model);
        Object best = null;
        float bs = Float.MAX_VALUE;
        for(boolean strict : new boolean[]{ true, false })
        {
            for(Map.Entry<Object, String> e : all.entrySet())
            {
                float s = score(e.getKey(), e.getValue(), right, strict);
                if(s < bs)
                {
                    bs = s;
                    best = e.getKey();
                }
            }
            if(best != null)
            {
                return best;
            }
        }
        return null;
    }

    private static boolean hasCubes(Object box)
    {
        try
        {
            Field f = field(box.getClass(), "cubeList");
            if(f != null && f.get(box) instanceof java.util.Collection<?> c && !c.isEmpty())
            {
                return true;
            }
            Field k = field(box.getClass(), "childModels");
            if(k != null && k.get(box) instanceof java.util.Collection<?> kids)
            {
                for(Object kid : kids)
                {
                    if(kid != null && hasCubes(kid))
                    {
                        return true;
                    }
                }
            }
        }
        catch(Throwable ignored) {}
        return false;
    }

    private static float score(Object box, String rawName, boolean right, boolean strict)
    {
        if(!hasCubes(box))
        {
            return Float.MAX_VALUE;
        }
        String n = rawName.toLowerCase(Locale.ROOT).replace("_", "");
        float x = 0F, y = 0F, z = 0F;
        for(Object b = box; b != null; b = parent(b))
        {
            x += num(b, "rotationPointX");
            y += num(b, "rotationPointY");
            z += num(b, "rotationPointZ");
        }
        boolean nameLeft = n.contains("left") || n.matches("^l(arm|leg|wing|fin|claw|hand|paw).*") || n.matches(".*(arm|leg|wing|fin|claw|hand|paw)l$");
        boolean nameRight = n.contains("right") || n.matches("^r(arm|leg|wing|fin|claw|hand|paw).*") || n.matches(".*(arm|leg|wing|fin|claw|hand|paw)r$");
        boolean onSide = nameLeft ^ nameRight ? (right ? nameRight : nameLeft) : (right ? x < -0.5F : x > 0.5F);
        if(strict && !onSide)
        {
            return Float.MAX_VALUE;
        }
        float sc = z * 4F - y * 0.01F + (onSide ? 0F : Math.abs(x) * 0.5F);
        if(n.matches(".*(forearm|upperarm|arm|hand|claw|paw|frontleg|foreleg|fore|flipper|front).*"))
        {
            sc -= 60F;
        }
        else if(n.matches(".*(leg|foot|wing|fin|tentacle|pincer|pedipalp|limb).*"))
        {
            sc -= 30F;
        }
        if(n.matches(".*(backleg|hindleg|rear|back).*") && !n.matches(".*(front|fore).*"))
        {
            sc += 70F;
        }
        if(n.matches(".*(bottom|lower|tip|toe|end)$"))
        {
            sc += 25F;
        }
        if(n.matches(".*(eye|ear|horn|spike|nose|beak|jaw|antenna|tail|gill|crest|whisker|tooth|teeth|mouth|tongue|hair|mane|hat|stick|snout|tusk|brow|lash|fang|armor|plate|shield|sword|blade|head).*"))
        {
            sc += 80F;
        }
        if(n.matches(".*(baby|saddle|rein|chest|bag).*"))
        {
            sc += 120F;
        }
        return sc;
    }
}
