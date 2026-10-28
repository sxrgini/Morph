package com.iiampanda.morph.client;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * Plays a mob's own attack animation when the player who is morphed into it swings. Vanilla style attack animations are started
 * by the swing and entity event already; this is for the mods that start theirs from their AI instead:
 * <ul>
 * <li>Citadel / LLibrary / LionfishAPI style <code>IAnimatedEntity</code> (Alex's Mobs, Mowzie's Mobs, Cataclysm and others): an animation is
 * chosen from the entity's static Animation fields by name and set on the stand-in,</li>
 * <li>GeckoLib entities that register triggerable animations: the attack-like one is triggered.</li>
 * </ul>
 * Everything is found by reflection so none of those mods is needed to compile.
 */
public final class ModAnimations
{
    private static final Pattern TIER1 = Pattern.compile("(^|_)(ATTACK|MELEE|STRIKE)(_|$)");
    private static final Pattern TIER2 = Pattern.compile("BITE|SLASH|CLAW|SCRATCH|PUNCH|SWING|SMASH|SLAM|STOMP|PECK|STING|SNAP|KICK|HEADBUTT|BUTT|GORE|LUNGE|POUNCE|SWIPE|CHOMP|LASH|WHIP|STAB|THRUST|HIT|ATTACK");
    private static final Pattern BAD = Pattern.compile("DIE|DEATH|HURT|DAMAGE|SLEEP|SIT|EAT|IDLE|BREED|SPAWN|ACTIVATE|BLINK|TALK|CHARGE|SHOOT|FIRE|SUMMON|SPELL|RAGE|ROAR|FLAP|HOVER|FLY|SWIM|WALK|DANCE|EMOTE|YAWN|STRETCH|TAKE_?OFF|LAND|BURROW|DIG|DEFEND|BLOCK|SHIELD|PROJECTILE|BREATH|BEAM|LASER|HEAL|PHASE|TRANSFORM|TELEPORT|THROW|SPIT|ROLL|PLAY|HOWL|SCREAM|SPRINT|LEAP|JUMP|CHARGING|PREPARE|WINDUP|WIND_UP|READY|START|END_|_END|STUN|SHAKE|SNIFF|DRINK|FEED|LAY|NOD|WAVE|SQUEAK|CROAK|CALL|PET|TAME|FLEE|STAGGER|KNOCKBACK|RECOIL");

    private static final Map<LivingEntity, Integer> NEXT = new WeakHashMap<>();
    private static final Map<Class<?>, List<Object[]>> ANIMS = new WeakHashMap<>();

    private ModAnimations() {}

    private static Method find(Class<?> c, String name, Class<?>... params)
    {
        try
        {
            return c.getMethod(name, params);
        }
        catch(Throwable ignored) {}
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

    /** Names that look like an attack, best first (tier 1 names, then tier 2), with the nasty ones left out. */
    private static List<String> attackish(List<String> names)
    {
        List<String> one = new ArrayList<>();
        List<String> two = new ArrayList<>();
        for(String n : names)
        {
            String u = n.toUpperCase(Locale.ROOT).replace("ANIMATION_", "").replace("_ANIMATION", "");
            if(BAD.matcher(u).find() && !TIER1.matcher(u).find())
            {
                continue;
            }
            if(TIER1.matcher(u).find())
            {
                one.add(n);
            }
            else if(TIER2.matcher(u).find())
            {
                two.add(n);
            }
        }
        one.addAll(two);
        return one;
    }

    /** Call when the player begins a swing (or a Better Combat attack). */
    public static void attack(LivingEntity e)
    {
        try
        {
            if(citadel(e))
            {
                return;
            }
        }
        catch(Throwable ignored) {}
        try
        {
            gecko(e);
        }
        catch(Throwable ignored) {}
    }

    private static boolean citadel(LivingEntity e) throws Exception
    {
        Class<?> c = e.getClass();
        Method get = find(c, "getAnimation");
        if(get == null || get.getParameterCount() != 0 || get.getReturnType().isPrimitive())
        {
            return false;
        }
        Method set = find(c, "setAnimation", get.getReturnType());
        Method setTick = find(c, "setAnimationTick", int.class);
        if(set == null || setTick == null)
        {
            return false;
        }
        List<Object[]> anims = ANIMS.get(c);
        if(anims == null)
        {
            anims = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for(Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass())
            {
                for(Field f : k.getDeclaredFields())
                {
                    if(Modifier.isStatic(f.getModifiers()) && get.getReturnType().isAssignableFrom(f.getType()))
                    {
                        f.setAccessible(true);
                        Object v = f.get(null);
                        if(v != null && !values.contains(v))
                        {
                            names.add(f.getName());
                            values.add(v);
                        }
                    }
                }
            }
            for(String n : attackish(names))
            {
                anims.add(new Object[]{ n, values.get(names.indexOf(n)) });
            }
            ANIMS.put(c, anims);
        }
        if(anims.isEmpty())
        {
            return true; // an animated entity with nothing that looks like an attack: leave it be
        }
        Object cur = get.invoke(e);
        if(cur != null)
        {
            Method dur = find(cur.getClass(), "getDuration");
            Method tick = find(c, "getAnimationTick");
            if(dur != null && tick != null && ((Number)dur.invoke(cur)).intValue() > 0 && ((Number)tick.invoke(e)).intValue() < ((Number)dur.invoke(cur)).intValue())
            {
                return true; // already busy with one
            }
        }
        // the first tier is the plain attack, so cycle only through animations of the same kind as the best one
        int best = tier(anims.get(0)[0].toString());
        List<Object[]> pool = new ArrayList<>();
        for(Object[] a : anims)
        {
            if(tier(a[0].toString()) == best)
            {
                pool.add(a);
            }
        }
        int i = NEXT.getOrDefault(e, 0);
        NEXT.put(e, i + 1);
        set.invoke(e, pool.get(i % pool.size())[1]);
        setTick.invoke(e, 0);
        return true;
    }

    private static int tier(String n)
    {
        return TIER1.matcher(n.toUpperCase(Locale.ROOT).replace("ANIMATION_", "").replace("_ANIMATION", "")).find() ? 1 : 2;
    }

    private static void gecko(LivingEntity e) throws Exception
    {
        Class<?> c = e.getClass();
        Method cacheM = find(c, "getAnimatableInstanceCache");
        Method trigger = find(c, "triggerAnim", String.class, String.class);
        if(cacheM == null || trigger == null)
        {
            return;
        }
        Object cache = cacheM.invoke(e);
        Method mgrM = find(cache.getClass(), "getManagerForId", long.class);
        if(mgrM == null)
        {
            return;
        }
        Object mgr = mgrM.invoke(cache, (long)e.getId());
        Method ctrlM = find(mgr.getClass(), "getAnimationControllers");
        if(ctrlM == null)
        {
            return;
        }
        Object ctrls = ctrlM.invoke(mgr);
        if(!(ctrls instanceof Map<?, ?> map))
        {
            return;
        }
        for(Map.Entry<?, ?> entry : map.entrySet())
        {
            Field f = field(entry.getValue().getClass(), "triggerableAnimations");
            if(f == null || !(f.get(entry.getValue()) instanceof Map<?, ?> trig) || trig.isEmpty())
            {
                continue;
            }
            List<String> names = new ArrayList<>();
            for(Object k : trig.keySet())
            {
                names.add(k.toString());
            }
            List<String> good = attackish(names);
            if(!good.isEmpty())
            {
                int i = NEXT.getOrDefault(e, 0);
                NEXT.put(e, i + 1);
                trigger.invoke(e, entry.getKey().toString(), good.get(i % good.size()));
                return;
            }
        }
    }
}
