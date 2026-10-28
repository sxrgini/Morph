package com.iiampanda.morph.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Sitting for morphs. Cats, wolves and parrots sit through TamableAnimal, foxes and pandas and camels each have their own call,
 * and modded mobs are asked through whichever "sit" setter they have. Everything but TamableAnimal is found by name, so no
 * mod (and no particular Minecraft version) is needed to compile it.
 */
public final class SitSupport
{
    private static final String[] SETTERS = { "setInSittingPose", "setOrderedToSit", "setSitting", "setIsSitting", "setSit", "setSat", "sit", "setSittingPose", "setSitDown", "setIsSit" };

    private static final Map<LivingEntity, Boolean> LAST = new WeakHashMap<>();

    private SitSupport() {}

    private static Method method(Class<?> c, String name, Class<?>... params)
    {
        for(; c != null && c != Object.class; c = c.getSuperclass())
        {
            try
            {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            }
            catch(Throwable ignored) {}
        }
        return null;
    }

    /** True if the stand-in has some way to sit down. */
    public static boolean canSit(LivingEntity e)
    {
        if(e instanceof TamableAnimal)
        {
            return true;
        }
        if(method(e.getClass(), "sitDown") != null && method(e.getClass(), "standUp") != null)
        {
            return true;
        }
        for(String s : SETTERS)
        {
            if(method(e.getClass(), s, boolean.class) != null)
            {
                return true;
            }
        }
        return false;
    }

    /** Camels sit by changing pose, which the player's own pose must not overwrite. */
    public static boolean ownsPose(LivingEntity e)
    {
        return LAST.getOrDefault(e, Boolean.FALSE) && method(e.getClass(), "sitDown") != null;
    }

    private static void call(LivingEntity e, String name)
    {
        try
        {
            Method m = method(e.getClass(), name);
            if(m != null)
            {
                m.invoke(e);
            }
        }
        catch(Throwable ignored) {}
    }

    /** Sits the stand-in down or stands it up (only does anything when the state actually changes). */
    public static void apply(LivingEntity e, boolean sit)
    {
        Boolean last = LAST.get(e);
        if(last == null ? !sit : last == sit)
        {
            return;
        }
        LAST.put(e, sit);
        if(e instanceof TamableAnimal t)
        {
            t.setInSittingPose(sit);
            t.setOrderedToSit(sit);
            return;
        }
        if(method(e.getClass(), "sitDown") != null && method(e.getClass(), "standUp") != null)
        {
            call(e, sit ? "sitDown" : "standUp");
            return;
        }
        for(String s : SETTERS)
        {
            Method m = method(e.getClass(), s, boolean.class);
            if(m != null)
            {
                try
                {
                    m.invoke(e, sit);
                }
                catch(Throwable ignored) {}
            }
        }
    }

    public static void forget(LivingEntity e)
    {
        LAST.remove(e);
    }
}
