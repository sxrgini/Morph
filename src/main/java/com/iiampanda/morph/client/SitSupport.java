package com.iiampanda.morph.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.Panda;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

public final class SitSupport
{
    private static final String[] SETTERS = { "setInSittingPose", "setOrderedToSit", "setSitting", "setIsSitting", "setSit", "setSat", "sit", "setSittingPose", "setSitDown", "setIsSit",
        "setLying", "setLaying", "setLayingDown", "setLyingDown", "setIsLying", "setResting", "setIsResting", "setRelaxed", "setLieDown", "setSleeping" };

    private static final Map<LivingEntity, Boolean> LAST = new WeakHashMap<>();

    private static final Map<LivingEntity, Integer> RISING = new WeakHashMap<>();

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

    private static boolean direct(LivingEntity e)
    {
        return e instanceof TamableAnimal || e instanceof Camel || e instanceof Panda || e instanceof Sniffer;
    }

    public static boolean canSit(LivingEntity e)
    {
        if(direct(e))
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

    public static boolean ownsPose(LivingEntity e)
    {
        if(e instanceof Camel c)
        {
            return c.isCamelSitting() || c.isInPoseTransition();
        }
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

    public static void apply(LivingEntity e, boolean sit)
    {
        if(e instanceof TamableAnimal t)
        {
            if(t.isInSittingPose() != sit)
            {
                t.setInSittingPose(sit);
                t.setOrderedToSit(sit);
            }
            return;
        }
        if(e instanceof Camel c)
        {
            if(sit && !c.isCamelSitting() && !c.isInPoseTransition())
            {
                c.sitDown();
            }
            else if(!sit && c.isCamelSitting() && !c.isInPoseTransition())
            {
                c.standUp();
            }
            return;
        }
        if(e instanceof Panda p)
        {
            if(p.isSitting() != sit)
            {
                p.sit(sit);
            }
            return;
        }
        if(e instanceof Sniffer s)
        {

            Integer rising = RISING.get(s);
            if(sit)
            {
                RISING.remove(s);
                if(!Boolean.TRUE.equals(LAST.get(s)))
                {
                    s.transitionTo(Sniffer.State.DIGGING);
                }
            }
            else if(Boolean.TRUE.equals(LAST.get(s)))
            {
                s.transitionTo(Sniffer.State.RISING);
                RISING.put(s, s.tickCount);
            }
            else if(rising != null && s.tickCount - rising > 70)
            {
                RISING.remove(s);
                s.transitionTo(Sniffer.State.IDLING);
            }
            LAST.put(s, sit);
            return;
        }
        Boolean last = LAST.get(e);
        if(last == null ? !sit : last == sit)
        {
            return;
        }
        LAST.put(e, sit);
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
        RISING.remove(e);
    }
}
